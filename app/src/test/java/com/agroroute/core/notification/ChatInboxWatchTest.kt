package com.agroroute.core.notification

import android.content.Context
import android.content.SharedPreferences
import com.agroroute.core.model.UserProfile
import com.agroroute.core.model.UserRole
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.*
import io.mockk.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.*
import org.junit.*

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class ChatInboxWatchTest {
    @After fun cleanup() { ChatNotificationVisibility.visible.value = null; unmockkAll() }
    private class Fixture {
        val auth = mockk<FirebaseAuth>(relaxed = true)
        val db = mockk<FirebaseFirestore>(relaxed = true)
        val context = mockk<Context>(relaxed = true)
        val prefs = mockk<SharedPreferences>()
        val editor = mockk<SharedPreferences.Editor>(relaxed = true)
        val query = mockk<Query>()
        val registration = mockk<ListenerRegistration>(relaxed = true)
        val row = mockk<DocumentSnapshot>()
        val snapshot = mockk<QuerySnapshot>(relaxed = true)
        val delivered = mutableSetOf<String>()
        lateinit var callback: EventListener<QuerySnapshot>
        val service: NotificationService
        val profile = UserProfile(userId = "receiver", role = UserRole.SUPPLIER)
        init {
            mockkStatic(android.util.Log::class)
            every { android.util.Log.d(any(), any()) } returns 0
            mockkStatic(FirebaseAuth::class, FirebaseFirestore::class)
            every { FirebaseAuth.getInstance() } returns auth
            every { FirebaseFirestore.getInstance() } returns db
            every { auth.currentUser?.uid } returns "receiver"
            every { context.getSharedPreferences(any(), any()) } returns prefs
            every { prefs.getBoolean(any(), false) } answers { firstArg<String>() in delivered }
            every { prefs.edit() } returns editor
            every { editor.putBoolean(any(), true) } answers { delivered.add(firstArg()); editor }
            every { db.collection("user_notifications").document("receiver").collection("items")
                .orderBy("createdAt", Query.Direction.DESCENDING).limit(100) } returns query
            every { query.addSnapshotListener(MetadataChanges.INCLUDE, any<EventListener<QuerySnapshot>>()) } answers {
                callback = secondArg(); registration
            }
            every { snapshot.metadata.isFromCache } returns false
            every { snapshot.documents } returns listOf(row)
            every { row.id } returns "CHAT_MESSAGE_pair_message"
            every { row.getString("eventType") } returns "CHAT_MESSAGE"
            every { row.getString("actorId") } returns "sender"
            every { row.getString("recipientId") } returns "receiver"
            every { row.get("readAt") } returns null
            service = spyk(NotificationService(context))
            every { service.post(any(), any(), any(), any(), any(), any()) } returns true
            coEvery { service.markInboxItemRead(any()) } just Runs
            mockkObject(NotificationHelper, SparkNotificationInbox)
            every { NotificationHelper.areNotificationsEnabled(context, any()) } returns true
            coEvery { SparkNotificationInbox.render(context, db, row, UserRole.SUPPLIER) } returns
                NotificationInboxItem("CHAT_MESSAGE_pair_message", "CHAT_MESSAGE", "New message from Nimal",
                    "Hello", "chat/sender", 1, null)
        }
    }
    @Test fun repeatedSnapshotsAndListenerRestartPostOneLocalNotification() = runTest {
        val f = Fixture()
        var job = launch { f.service.watchInbox(f.profile) }; runCurrent()
        f.callback.onEvent(f.snapshot, null); runCurrent()
        f.callback.onEvent(f.snapshot, null); runCurrent()
        job.cancel(); runCurrent()
        job = launch { f.service.watchInbox(f.profile) }; runCurrent()
        f.callback.onEvent(f.snapshot, null); runCurrent()
        verify(exactly = 1) { f.service.post(NotificationEvent.CHAT_MESSAGE, any(), "chat/sender", "New message from Nimal", "Hello", "CHAT_MESSAGE_pair_message") }
        job.cancel(); runCurrent()
        verify(exactly = 2) { f.registration.remove() }
    }
    @Test fun openingExactChatMarksInboxReadAndSuppressesLocalAlert() = runTest {
        val f = Fixture()
        ChatNotificationVisibility.visible.value = "receiver" to "sender"
        val job = launch { f.service.watchInbox(f.profile) }; runCurrent()
        f.callback.onEvent(f.snapshot, null); runCurrent()
        coVerify(exactly = 1) { f.service.markInboxItemRead("CHAT_MESSAGE_pair_message") }
        verify(exactly = 0) { f.service.post(any(), any(), any(), any(), any(), any()) }
        job.cancel(); runCurrent()
    }
    @Test fun ownMessageAndAnotherAccountNeverPost() = runTest {
        val f = Fixture()
        every { f.row.getString("actorId") } returns "receiver"
        val job = launch { f.service.watchInbox(f.profile) }; runCurrent()
        f.callback.onEvent(f.snapshot, null); runCurrent()
        every { f.auth.currentUser?.uid } returns "another-account"
        f.callback.onEvent(f.snapshot, null); runCurrent()
        verify(exactly = 0) { f.service.post(any(), any(), any(), any(), any(), any()) }
        coVerify(exactly = 0) { f.service.markInboxItemRead(any()) }
        job.cancel(); runCurrent()
    }
    @Test fun permissionSettingsRefreshRetriesAnUnreadUnpostedEvent() = runTest {
        val f = Fixture()
        every { NotificationHelper.areNotificationsEnabled(f.context, any()) } returns false
        val job = launch { f.service.watchInbox(f.profile) }; runCurrent()
        f.callback.onEvent(f.snapshot, null); runCurrent()
        verify(exactly = 0) { f.service.post(any(), any(), any(), any(), any(), any()) }
        every { NotificationHelper.areNotificationsEnabled(f.context, any()) } returns true
        NotificationDeliveryRefresh.refresh(); runCurrent()
        verify(exactly = 1) { f.service.post(any(), any(), any(), any(), any(), any()) }
        job.cancel(); runCurrent()
    }
    @Test fun skippedPostDoesNotPermanentlyMarkAnEventDelivered() = runTest {
        val f = Fixture()
        every { f.service.post(any(), any(), any(), any(), any(), any()) } returnsMany listOf(false, true)
        val job = launch { f.service.watchInbox(f.profile) }; runCurrent()
        repeat(3) { f.callback.onEvent(f.snapshot, null); runCurrent() }
        verify(exactly = 2) { f.service.post(any(), any(), any(), any(), any(), any()) }
        job.cancel(); runCurrent()
    }
    @Test fun nonChatEventsRetryAfterPermissionGrantAndDeduplicateAcrossRestart() = runTest {
        val f = Fixture()
        every { f.row.getString("eventType") } returns "ORDER_COMPLETED"
        // An open chat must never consume an order notification.
        ChatNotificationVisibility.visible.value = "receiver" to "sender"
        every { NotificationHelper.areNotificationsEnabled(f.context, any()) } returns false
        var job = launch { f.service.watchInbox(f.profile) }; runCurrent()
        f.callback.onEvent(f.snapshot, null); runCurrent()
        verify(exactly = 0) { f.service.post(any(), any(), any(), any(), any(), any()) }
        every { NotificationHelper.areNotificationsEnabled(f.context, any()) } returns true
        NotificationDeliveryRefresh.refresh(); runCurrent()
        job.cancel(); runCurrent()
        job = launch { f.service.watchInbox(f.profile) }; runCurrent()
        f.callback.onEvent(f.snapshot, null); runCurrent()
        verify(exactly = 1) { f.service.post(NotificationEvent.ORDER_COMPLETED, any(), any(), any(), any(), any()) }
        coVerify(exactly = 0) { f.service.markInboxItemRead(any()) }
        job.cancel(); runCurrent()
    }

    @Test fun terminalListenerFailureRetriesWithoutDuplicatingAnAlreadyPostedMessage() = runTest {
        val f = Fixture()
        every { android.util.Log.w(any<String>(), any<String>(), any<Throwable>()) } returns 0
        val job = launch { f.service.watchInbox(f.profile) }; runCurrent()
        f.callback.onEvent(f.snapshot, null); runCurrent()
        f.callback.onEvent(null, mockk<FirebaseFirestoreException>(relaxed = true)); runCurrent()
        advanceTimeBy(5_000); runCurrent()
        f.callback.onEvent(f.snapshot, null); runCurrent()
        verify(exactly = 2) { f.query.addSnapshotListener(MetadataChanges.INCLUDE, any<EventListener<QuerySnapshot>>()) }
        verify(exactly = 1) { f.service.post(any(), any(), any(), any(), any(), any()) }
        job.cancel(); runCurrent()
    }

}
