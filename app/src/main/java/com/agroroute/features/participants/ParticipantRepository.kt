@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package com.agroroute.features.participants

import com.agroroute.core.model.UserRole
import com.google.firebase.firestore.*
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.tasks.await

interface ParticipantRepository {
    fun profile(id: String): Flow<ParticipantProfile>
    suspend fun identity(id: String): ParticipantIdentity
}

@Singleton
class ParticipantRepositoryImpl @Inject constructor(private val db: FirebaseFirestore) : ParticipantRepository {
    override suspend fun identity(id: String): ParticipantIdentity = identityFrom(db.collection("users").document(id).get(Source.SERVER).await())

    override fun profile(id: String): Flow<ParticipantProfile> = documentUpdates(db.collection("users").document(id)).flatMapLatest { doc ->
        val person = identityFrom(doc)
        val reviews = queryUpdates(db.collection("reviews").whereEqualTo("revieweeId", id)).map { snapshot ->
            snapshot.documents.mapNotNull { r ->
                val rating = r.getLong("rating")?.toInt()?.takeIf { it in 1..5 } ?: return@mapNotNull null
                val reviewer = try { r.getString("reviewerId")?.let { identity(it).name } ?: "Participant" }
                    catch (e: CancellationException) { throw e }
                    catch (_: Exception) { "Participant" }
                ParticipantReview(r.id, rating, r.getString("comment").orEmpty(), r.getLong("createdAt"), reviewer)
            }.sortedWith(compareByDescending<ParticipantReview> { it.date ?: 0 }.thenBy { it.id })
        }
        // Existing listing rules require both status and isDraft constraints for supplier queries.
        val harvests: Flow<List<String>?> = if (person.role == UserRole.FARMER) queryUpdates(
            db.collection("listings").whereEqualTo("farmerId", id).whereEqualTo("status", "OPEN").whereEqualTo("isDraft", false)
        ).map<QuerySnapshot, List<String>?> { snapshot -> snapshot.documents.filter { d ->
            val expiry = d.getLong("expiresAt")
            val status = d.getString("harvestStatus") ?: "READY"
            val stock = d.getDouble("availableQuantity") ?: ((d.getDouble("quantity") ?: 0.0) - (d.getDouble("soldQuantity") ?: 0.0))
            (expiry == null || expiry > System.currentTimeMillis()) && (status == "EXPECTED" || status == "READY" && stock > 0)
        }.map { it.getString("cropName").orEmpty() } }.catch { emit(null) } else flowOf(null)
        combine(reviews, harvests) { r, h -> ParticipantProfile(person, r, h, doc.getString("phone")?.trim()?.takeIf { it.isNotBlank() }) }
    }
}

internal fun identityFrom(d: DocumentSnapshot): ParticipantIdentity {
    check(d.exists())
    val role = when (d.getString("role")) { "FARMER" -> UserRole.FARMER; "SUPPLIER" -> UserRole.SUPPLIER; else -> error("Unavailable profile") }
    return ParticipantIdentity(d.id, d.getString("displayName")?.takeIf { it.isNotBlank() } ?: "Name unavailable", role,
        d.getString("profileImageUrl"), d.getLong("createdAt")?.takeIf { it > 0 }, d.getLong("points"))
}
internal fun queryUpdates(query: Query): Flow<QuerySnapshot> = callbackFlow {
    val listener = query.addSnapshotListener(MetadataChanges.INCLUDE) { value, error ->
        if (error != null) close(error) else if (value != null) trySend(value)
    }
    awaitClose { listener.remove() }
}
internal fun documentUpdates(ref: DocumentReference): Flow<DocumentSnapshot> = callbackFlow {
    val listener = ref.addSnapshotListener(MetadataChanges.INCLUDE) { value, error ->
        if (error != null) close(error) else if (value != null) trySend(value)
    }
    awaitClose { listener.remove() }
}
