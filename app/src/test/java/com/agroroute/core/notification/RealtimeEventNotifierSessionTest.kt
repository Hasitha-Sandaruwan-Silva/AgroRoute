package com.agroroute.core.notification

import com.agroroute.core.model.UserProfile
import com.agroroute.core.model.UserRole
import com.agroroute.core.remote.FirestoreService
import com.google.firebase.firestore.*
import io.mockk.*
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.runCurrent
import org.junit.Assert.*
import org.junit.Test

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class RealtimeEventNotifierSessionTest {
    private suspend fun kotlinx.coroutines.test.TestScope.verifyStop(role: UserRole) {
        val firestore=mockk<FirebaseFirestore>()
        val collection=mockk<CollectionReference>()
        val query=mockk<Query>()
        val registrations=mutableListOf<ListenerRegistration>()
        every { firestore.collection(any()) } returns collection
        every { collection.whereEqualTo(any<String>(),any()) } returns query
        every { query.addSnapshotListener(any<EventListener<QuerySnapshot>>()) } answers {
            mockk<ListenerRegistration>(relaxed=true).also { registrations += it }
        }
        val notifier=RealtimeEventNotifier(FirestoreService(firestore),mockk(relaxed=true))
        notifier.start(this,UserProfile(userId="real-session-test",role=role))
        runCurrent()
        assertTrue("Expected actual callback-flow listeners",registrations.isNotEmpty())
        notifier.stop()
        runCurrent()
        registrations.forEach { verify(exactly=1) { it.remove() } }
        assertTrue(coroutineContext[kotlinx.coroutines.Job]!!.isActive)
    }
    @Test fun farmerStopCancelsEverySessionChildListener() = runTest { verifyStop(UserRole.FARMER) }
    @Test fun supplierStopCancelsEverySessionChildListener() = runTest { verifyStop(UserRole.SUPPLIER) }
}
