@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package com.agroroute.features.participants

import com.agroroute.core.model.UserRole
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.*
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.tasks.await

interface ChatRepository {
    val currentUserId: String?
    val authenticatedUserIds: Flow<String?> get() = flowOf(currentUserId)
    fun conversations(): Flow<List<Conversation>>
    suspend fun pair(otherId: String): Pair<String, ParticipantIdentity>
    fun messages(conversationId: String): Flow<List<ChatMessage>>
    suspend fun send(otherId: String, input: String)
}

@Singleton
class ChatRepositoryImpl @Inject constructor(
    private val db: FirebaseFirestore, private val auth: FirebaseAuth, private val people: ParticipantRepository
) : ChatRepository {
    override val currentUserId get() = auth.currentUser?.uid
    override val authenticatedUserIds: Flow<String?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { trySend(it.currentUser?.uid) }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }.distinctUntilChanged()
    private fun uid() = checkNotNull(currentUserId)
    override suspend fun pair(otherId: String): Pair<String, ParticipantIdentity> {
        val me = people.identity(uid())
        val other = people.identity(otherId)
        require(me.id != other.id && me.role != other.role)
        return (if (me.role == UserRole.FARMER) conversationIdentity(me.id, other.id)
            else conversationIdentity(other.id, me.id)) to other
    }
    override fun conversations(): Flow<List<Conversation>> {
        val userId = uid()
        return queryUpdates(db.collection("conversations").whereArrayContains("participantIds", userId)).filter { !it.metadata.isFromCache }.map { snapshot ->
            check(currentUserId == userId)
            snapshot.documents.map { d ->
                val otherId = (d.get("participantIds") as List<*>).filterIsInstance<String>().single { it != userId }
                Conversation(d.id, people.identity(otherId), d.getString("lastMessage").orEmpty(), d.getTimestamp("updatedAt")?.toDate()?.time)
            }.sortedWith(compareByDescending<Conversation> { it.updatedAt ?: 0 }.thenBy { it.id })
        }
    }
    override fun messages(conversationId: String): Flow<List<ChatMessage>> {
        val userId = uid()
        // Only committed server timestamps are displayed; local pending writes never masquerade as sent.
        return documentUpdates(db.collection("conversations").document(conversationId)).filter { !it.metadata.isFromCache }
            .map { it.exists() }.distinctUntilChanged().flatMapLatest { exists ->
            if (!exists) flowOf(emptyList()) else queryUpdates(db.collection("conversations").document(conversationId).collection("messages").orderBy("sentAt")).filter { !it.metadata.isFromCache }.map { s ->
            check(currentUserId == userId)
            s.documents.filterNot { it.metadata.hasPendingWrites() }.map { d ->
                ChatMessage(d.id, d.getString("senderId").orEmpty(), d.getString("text").orEmpty(), d.getTimestamp("sentAt")?.toDate()?.time)
            }.sortedWith(compareBy<ChatMessage> { it.sentAt ?: Long.MAX_VALUE }.thenBy { it.id })
            }
        }
    }
    override suspend fun send(otherId: String, input: String) {
        val text = messageText(input)
        val userId = uid()
        val (id, other) = pair(otherId)
        check(currentUserId == userId)
        val farmerId = if (other.role == UserRole.FARMER) other.id else userId
        val supplierId = if (other.role == UserRole.SUPPLIER) other.id else userId
        val conversation = db.collection("conversations").document(id)
        val message = conversation.collection("messages").document()
        // Transactions fail while offline and atomically commit the message and its preview.
        db.runTransaction { tx ->
            check(currentUserId == userId)
            val existing = tx.get(conversation)
            val summary = mutableMapOf<String, Any>("participantIds" to listOf(farmerId, supplierId),
                "farmerId" to farmerId, "supplierId" to supplierId,
                "updatedAt" to FieldValue.serverTimestamp(), "lastMessage" to text,
                "lastSenderId" to userId, "lastMessageId" to message.id)
            if (!existing.exists()) summary["createdAt"] = FieldValue.serverTimestamp()
            tx.set(message, mapOf("senderId" to userId, "text" to text, "sentAt" to FieldValue.serverTimestamp()))
            tx.set(conversation, summary, SetOptions.merge())
        }.await()
    }
}
