package com.agroroute.core.repository

import com.agroroute.core.database.BidDao
import com.agroroute.core.database.HarvestListingDao
import com.agroroute.core.database.OrderCompletionRecordDao
import com.agroroute.core.database.OrderDao
import com.agroroute.core.database.OrderRequestDao
import com.agroroute.core.database.ReviewDao
import com.agroroute.core.database.UserProfileDao
import com.agroroute.core.model.Order
import com.agroroute.core.model.OrderRequest
import com.agroroute.core.model.OrderStatus
import com.agroroute.core.model.UserProfile
import com.agroroute.core.model.UserRole
import com.agroroute.core.notification.NotificationService
import com.agroroute.core.remote.FirestoreService
import com.agroroute.core.util.Constants
import com.agroroute.core.util.Result
import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.EventListener
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.Source
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Test

class OrderSyncTest {

    private val farmerId = "farmer-1"

    @Test
    fun farmerOrderSyncUsesServerSnapshotAfterEmptyCacheSnapshot() = runTest {
        val order = Order(
            orderId = "order-1",
            farmerId = farmerId,
            supplierId = "supplier-1",
            status = OrderStatus.AWAITING_PICKUP
        )
        val dao = mockk<OrderDao>(relaxed = true)
        val query = mockOrderQuery(order)
        val firestore = mockk<FirebaseFirestore>()
        val orders = mockk<CollectionReference>()
        every { firestore.collection(Constants.COLLECTION_ORDERS) } returns orders
        every { orders.whereEqualTo("farmerId", farmerId) } returns query

        val repository = orderRepository(dao, firestore, farmerAuth())
        val result = repository.syncOrders(farmerId)

        assertTrue(result is Result.Success)
        verify(exactly = 1) { query.get(Source.SERVER) }
        coVerify(exactly = 1) { dao.upsertOrders(listOf(order)) }
    }

    @Test
    fun farmerRequestSyncUsesServerSnapshotAfterEmptyCacheSnapshot() = runTest {
        val request = OrderRequest(
            requestId = "request-1",
            farmerId = farmerId,
            supplierId = "supplier-1"
        )
        val dao = mockk<OrderRequestDao>(relaxed = true)
        val query = mockRequestQuery(request)
        val firestore = mockk<FirebaseFirestore>()
        val requests = mockk<CollectionReference>()
        every { firestore.collection(Constants.COLLECTION_ORDER_REQUESTS) } returns requests
        every { requests.whereEqualTo("farmerId", farmerId) } returns query

        val repository = OrderRequestRepositoryImpl(
            dao,
            FirestoreService(firestore),
            farmerAuth(),
            mockk<NotificationService>(relaxed = true)
        )
        val result = repository.syncRequestsForUser(farmerId)

        assertTrue(result is Result.Success)
        verify(exactly = 1) { query.get(Source.SERVER) }
        coVerify(exactly = 1) { dao.upsertRequests(listOf(request)) }
    }

    private fun farmerAuth(): AuthRepository = mockk {
        every { currentUserId } returns farmerId
        every { getCurrentUserProfile() } returns flowOf(
            Result.Success(UserProfile(userId = farmerId, role = UserRole.FARMER))
        )
    }

    private fun orderRepository(dao: OrderDao, firestore: FirebaseFirestore, auth: AuthRepository) =
        OrderRepositoryImpl(
            dao = dao,
            firestoreService = FirestoreService(firestore),
            bidDao = mockk<BidDao>(relaxed = true),
            listingDao = mockk<HarvestListingDao>(relaxed = true),
            orderRequestDao = mockk<OrderRequestDao>(relaxed = true),
            authRepository = auth,
            orderCompletionRecordDao = mockk<OrderCompletionRecordDao>(relaxed = true),
            userProfileDao = mockk<UserProfileDao>(relaxed = true),
            notificationService = mockk<NotificationService>(relaxed = true),
            completionService = mockk(relaxed = true)
        )

    private fun mockOrderQuery(order: Order): Query {
        val query = mockk<Query>()
        val cachedSnapshot = mockk<QuerySnapshot>()
        val serverSnapshot = mockk<QuerySnapshot>()
        val serverDocument = mockk<DocumentSnapshot>()
        every { cachedSnapshot.documents } returns emptyList()
        every { serverSnapshot.documents } returns listOf(serverDocument)
        every { serverDocument.toObject(Order::class.java) } returns order
        every { query.addSnapshotListener(any<EventListener<QuerySnapshot>>()) } answers {
            firstArg<EventListener<QuerySnapshot>>().onEvent(cachedSnapshot, null)
            mockk<ListenerRegistration>(relaxed = true)
        }
        every { query.get(Source.SERVER) } returns Tasks.forResult(serverSnapshot)
        return query
    }

    private fun mockRequestQuery(request: OrderRequest): Query {
        val query = mockk<Query>()
        val cachedSnapshot = mockk<QuerySnapshot>()
        val serverSnapshot = mockk<QuerySnapshot>()
        val serverDocument = mockk<DocumentSnapshot>()
        every { cachedSnapshot.documents } returns emptyList()
        every { serverSnapshot.documents } returns listOf(serverDocument)
        every { serverDocument.toObject(OrderRequest::class.java) } returns request
        every { query.addSnapshotListener(any<EventListener<QuerySnapshot>>()) } answers {
            firstArg<EventListener<QuerySnapshot>>().onEvent(cachedSnapshot, null)
            mockk<ListenerRegistration>(relaxed = true)
        }
        every { query.get(Source.SERVER) } returns Tasks.forResult(serverSnapshot)
        return query
    }
}