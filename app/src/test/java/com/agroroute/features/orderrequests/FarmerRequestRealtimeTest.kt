package com.agroroute.features.orderrequests

import com.agroroute.core.database.OrderRequestDao
import com.agroroute.core.model.OrderRequest
import com.agroroute.core.model.OrderRequestStatus
import com.agroroute.core.notification.NotificationService
import com.agroroute.core.remote.FirestoreService
import com.agroroute.core.repository.*
import com.agroroute.core.util.Result
import com.google.firebase.firestore.*
import io.mockk.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Test

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class FarmerRequestRealtimeTest {
    @Test fun committedRemoteRequestUpdatesRoomAndListenerIsRemovedOnStop() = runTest {
        val db = mockk<FirebaseFirestore>()
        val collection = mockk<CollectionReference>()
        val query = mockk<Query>()
        val registration = mockk<ListenerRegistration>(relaxed = true)
        val snapshot = mockk<QuerySnapshot>()
        val document = mockk<DocumentSnapshot>()
        lateinit var listener: EventListener<QuerySnapshot>
        every { db.collection("order_requests") } returns collection
        every { collection.whereEqualTo("farmerId", "f") } returns query
        every { query.addSnapshotListener(any<EventListener<QuerySnapshot>>()) } answers { listener = firstArg(); registration }
        every { snapshot.documents } returns listOf(document)
        val dao = mockk<OrderRequestDao>()
        val local = MutableStateFlow(emptyList<OrderRequest>())
        every { dao.getRequestsForFarmer("f") } returns local
        coEvery { dao.upsertRequests(any()) } coAnswers { local.value = firstArg(); Unit }
        val auth = mockk<AuthRepository>()
        every { auth.currentUserId } returns "f"
        val repo = OrderRequestRepositoryImpl(dao, FirestoreService(db), auth, mockk<NotificationService>())
        val seen = mutableListOf<Result<List<OrderRequest>>>()
        val job = launch { repo.observeRequestsForFarmer("f").collect { seen += it } }; runCurrent()
        val request = OrderRequest(requestId = "new", farmerId = "f")
        every { document.toObject(OrderRequest::class.java) } returns request
        listener.onEvent(snapshot, null); runCurrent()
        assertEquals("new", seen.last().getOrNull()!!.single().requestId)
        every { document.toObject(OrderRequest::class.java) } returns request.copy(status = OrderRequestStatus.ACCEPTED)
        listener.onEvent(snapshot, null); runCurrent()
        assertEquals(OrderRequestStatus.ACCEPTED, seen.last().getOrNull()!!.single().status)
        every { auth.currentUserId } returns "another-account"
        listener.onEvent(snapshot, null); runCurrent()
        coVerify(exactly = 2) { dao.upsertRequests(any()) }
        job.cancel(); runCurrent()
        verify(exactly = 1) { registration.remove() }
    }
}
