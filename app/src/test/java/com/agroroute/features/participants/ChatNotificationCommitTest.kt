package com.agroroute.features.participants

import com.agroroute.core.model.UserRole
import com.agroroute.core.notification.NotificationService
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.*
import io.mockk.*
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.launch
import org.junit.Assert.*
import org.junit.Test

class ChatNotificationCommitTest {
    @Test fun notificationsFollowSuccessfulCommitForBothRolesOnly() = runTest {
        for (role in UserRole.entries) {
            val me = if (role == UserRole.FARMER) "farmer" else "supplier"
            val other = if (role == UserRole.FARMER) "supplier" else "farmer"
            val db = mockk<FirebaseFirestore>(relaxed = true)
            val auth = mockk<FirebaseAuth>()
            val user = mockk<FirebaseUser>()
            every { auth.currentUser } returns user
            every { user.uid } returns me
            val people = mockk<ParticipantRepository>()
            coEvery { people.identity(me) } returns ParticipantIdentity(me, "Me", role)
            coEvery { people.identity(other) } returns ParticipantIdentity(other, "Other", if (role == UserRole.FARMER) UserRole.SUPPLIER else UserRole.FARMER)
            val notices = mockk<NotificationService>(relaxed = true)
            val conversationId = conversationIdentity("farmer", "supplier")
            val conversation = mockk<DocumentReference>(relaxed = true)
            val messages = mockk<CollectionReference>()
            val message = mockk<DocumentReference>()
            every { db.collection("conversations").document(conversationId) } returns conversation
            every { conversation.collection("messages") } returns messages
            every { messages.document() } returns message
            every { message.id } returns "committed-message"
            val completion = com.google.android.gms.tasks.TaskCompletionSource<Transaction>()
            every { db.runTransaction(any<Transaction.Function<Transaction>>()) } returns completion.task
            val repo = ChatRepositoryImpl(db, auth, people, notices)
            val job = launch { repo.send(other, "Hello") }
            testScheduler.runCurrent()
            coVerify(exactly = 0) { notices.recordChatMessage(any(), any(), any(), any()) }
            completion.setResult(mockk())
            testScheduler.runCurrent(); job.join()
            coVerify(exactly = 1) { notices.recordChatMessage(me, other, conversationId, "committed-message") }
        }
    }
    @Test fun failedCommitCannotCreateReceiverNotification() = runTest {
        val db = mockk<FirebaseFirestore>(relaxed = true)
        val auth = mockk<FirebaseAuth>(relaxed = true)
        every { auth.currentUser?.uid } returns "farmer"
        val people = mockk<ParticipantRepository>()
        coEvery { people.identity("farmer") } returns ParticipantIdentity("farmer", "Farmer", UserRole.FARMER)
        coEvery { people.identity("supplier") } returns ParticipantIdentity("supplier", "Supplier", UserRole.SUPPLIER)
        every { db.runTransaction(any<Transaction.Function<Transaction>>()) } returns Tasks.forException(IllegalStateException("commit failed"))
        val notices = mockk<NotificationService>(relaxed = true)
        try { ChatRepositoryImpl(db, auth, people, notices).send("supplier", "Hi"); fail("Expected commit failure") }
        catch (e: IllegalStateException) { assertEquals("commit failed", e.message) }
        coVerify(exactly = 0) { notices.recordChatMessage(any(), any(), any(), any()) }
    }
}
