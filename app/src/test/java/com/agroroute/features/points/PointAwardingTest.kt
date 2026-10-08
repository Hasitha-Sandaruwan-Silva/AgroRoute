package com.agroroute.features.points

import com.agroroute.core.model.HarvestListing
import com.agroroute.core.model.Order
import com.agroroute.core.model.OrderCompletionRecord
import com.agroroute.core.model.OrderStatus
import com.agroroute.core.model.QuantityUnit
import com.agroroute.core.model.UserProfile
import com.agroroute.core.model.UserRole
import com.agroroute.core.notification.NotificationService
import com.agroroute.core.remote.FirestoreService
import com.agroroute.core.repository.AuthRepository
import com.agroroute.core.repository.OrderRepositoryImpl
import com.agroroute.core.util.Result
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Transaction
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.UUID

@OptIn(ExperimentalCoroutinesApi::class)
class PointAwardingTest {

    private lateinit var orderRepository: OrderRepositoryImpl
    private val firestoreService = mockk<FirestoreService>(relaxed = true)
    private val firestore = mockk<FirebaseFirestore>(relaxed = true)

    private val testOrderId = UUID.randomUUID().toString()
    private val testFarmerId = "farmer123"
    private val testSupplierId = "supplier123"
    private val testListingId = "listing123"

    @Before
    fun setup() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        every { firestoreService.firestore } returns firestore

        orderRepository = OrderRepositoryImpl(
            dao = mockk(relaxed = true),
            firestoreService = firestoreService,
            bidDao = mockk(relaxed = true),
            listingDao = mockk(relaxed = true),
            orderRequestDao = mockk(relaxed = true),
            authRepository = supplierAuthRepository(),
            orderCompletionRecordDao = mockk(relaxed = true),
            userProfileDao = mockk(relaxed = true),
            notificationService = mockk<NotificationService>(relaxed = true)
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun supplierAuthRepository(): AuthRepository {
        val mock = mockk<AuthRepository>(relaxed = true)
        every { mock.currentUserId } returns testSupplierId
        every { mock.getCurrentUserProfile() } returns flowOf(Result.Success(UserProfile(userId = testSupplierId, role = UserRole.SUPPLIER)))
        return mock
    }

    private fun farmerAuthRepository(): AuthRepository {
        val mock = mockk<AuthRepository>(relaxed = true)
        every { mock.currentUserId } returns testFarmerId
        every { mock.getCurrentUserProfile() } returns flowOf(Result.Success(UserProfile(userId = testFarmerId, role = UserRole.FARMER)))
        return mock
    }

    @Test
    fun `completed order awards exactly one point to farmer and supplier`() = runTest {
        val existingOrder = Order(
            orderId = testOrderId,
            listingId = testListingId,
            bidId = "",
            farmerId = testFarmerId,
            supplierId = testSupplierId,
            cropName = "Rice",
            quantity = 5.0,
            quantityUnit = QuantityUnit.KG,
            agreedPrice = 8.0,
            status = OrderStatus.IN_TRANSIT
        )

        val farmerProfile = UserProfile(
            userId = testFarmerId,
            displayName = "Test Farmer",
            role = UserRole.FARMER,
            points = 5
        )
        val supplierProfile = UserProfile(
            userId = testSupplierId,
            displayName = "Test Supplier",
            role = UserRole.SUPPLIER,
            points = 3
        )

        val orderRef = mockk<DocumentReference>(relaxed = true)
        val recordRef = mockk<DocumentReference>(relaxed = true)
        val farmerRef = mockk<DocumentReference>(relaxed = true)
        val supplierRef = mockk<DocumentReference>(relaxed = true)

        every { orderRef.path } returns "orders/$testOrderId"
        every { recordRef.path } returns "order_completion_records/$testOrderId"
        every { farmerRef.path } returns "users/$testFarmerId"
        every { supplierRef.path } returns "users/$testSupplierId"

        val ordersColl = mockk<CollectionReference>(relaxed = true)
        val recordsColl = mockk<CollectionReference>(relaxed = true)
        val usersColl = mockk<CollectionReference>(relaxed = true)

        every { firestore.collection("orders") } returns ordersColl
        every { ordersColl.document(testOrderId) } returns orderRef

        every { firestore.collection("order_completion_records") } returns recordsColl
        every { recordsColl.document(testOrderId) } returns recordRef

        every { firestore.collection("users") } returns usersColl
        every { usersColl.document(testFarmerId) } returns farmerRef
        every { usersColl.document(testSupplierId) } returns supplierRef

        val mockTransaction = mockk<Transaction>(relaxed = true)
        val mockOrderDoc = mockk<DocumentSnapshot>()
        val mockRecordDoc = mockk<DocumentSnapshot>()
        val mockFarmerDoc = mockk<DocumentSnapshot>()
        val mockSupplierDoc = mockk<DocumentSnapshot>()

        every { mockOrderDoc.toObject(Order::class.java) } returns existingOrder
        every { mockRecordDoc.exists() } returns false
        every { mockFarmerDoc.toObject(UserProfile::class.java) } returns farmerProfile
        every { mockSupplierDoc.toObject(UserProfile::class.java) } returns supplierProfile

        every { mockTransaction.get(match<DocumentReference> { it == orderRef || it.path == "orders/$testOrderId" }) } returns mockOrderDoc
        every { mockTransaction.get(match<DocumentReference> { it == recordRef || it.path == "order_completion_records/$testOrderId" }) } returns mockRecordDoc
        every { mockTransaction.get(match<DocumentReference> { it == farmerRef || it.path == "users/$testFarmerId" }) } returns mockFarmerDoc
        every { mockTransaction.get(match<DocumentReference> { it == supplierRef || it.path == "users/$testSupplierId" }) } returns mockSupplierDoc

        coEvery { firestoreService.runTransaction<Order>(any()) } answers {
            val block = firstArg<(Transaction) -> Order>()
            block(mockTransaction)
        }

        val farmerSlot = slot<UserProfile>()
        val supplierSlot = slot<UserProfile>()
        val recordSlot = slot<OrderCompletionRecord>()

        every { mockTransaction.set(match<DocumentReference> { it == farmerRef || it.path == "users/$testFarmerId" }, capture(farmerSlot)) } returns mockTransaction
        every { mockTransaction.set(match<DocumentReference> { it == supplierRef || it.path == "users/$testSupplierId" }, capture(supplierSlot)) } returns mockTransaction
        every { mockTransaction.set(match<DocumentReference> { it == recordRef || it.path == "order_completion_records/$testOrderId" }, capture(recordSlot)) } returns mockTransaction

        val result = orderRepository.updateOrderStatus(testOrderId, OrderStatus.DELIVERED)

        assertTrue(result is Result.Success)
        assertEquals(6, farmerSlot.captured.points)
        assertEquals(4, supplierSlot.captured.points)
        assertEquals(testOrderId, recordSlot.captured.orderId)
    }

    @Test
    fun `delivery is rejected when a participant profile is missing`() = runTest {
        val existingOrder = Order(
            orderId = testOrderId,
            listingId = testListingId,
            farmerId = testFarmerId,
            supplierId = testSupplierId,
            status = OrderStatus.IN_TRANSIT
        )
        val orderRef = mockk<DocumentReference>(relaxed = true)
        val recordRef = mockk<DocumentReference>(relaxed = true)
        val farmerRef = mockk<DocumentReference>(relaxed = true)
        val supplierRef = mockk<DocumentReference>(relaxed = true)
        every { orderRef.path } returns "orders/$testOrderId"
        every { recordRef.path } returns "order_completion_records/$testOrderId"
        every { farmerRef.path } returns "users/$testFarmerId"
        every { supplierRef.path } returns "users/$testSupplierId"

        val ordersColl = mockk<CollectionReference>(relaxed = true)
        val recordsColl = mockk<CollectionReference>(relaxed = true)
        val usersColl = mockk<CollectionReference>(relaxed = true)
        every { firestore.collection("orders") } returns ordersColl
        every { ordersColl.document(testOrderId) } returns orderRef
        every { firestore.collection("order_completion_records") } returns recordsColl
        every { recordsColl.document(testOrderId) } returns recordRef
        every { firestore.collection("users") } returns usersColl
        every { usersColl.document(testFarmerId) } returns farmerRef
        every { usersColl.document(testSupplierId) } returns supplierRef

        val transaction = mockk<Transaction>(relaxed = true)
        val orderDoc = mockk<DocumentSnapshot>()
        val completionDoc = mockk<DocumentSnapshot>()
        val farmerDoc = mockk<DocumentSnapshot>()
        val supplierDoc = mockk<DocumentSnapshot>()
        every { orderDoc.toObject(Order::class.java) } returns existingOrder
        every { completionDoc.exists() } returns false
        every { farmerDoc.toObject(UserProfile::class.java) } returns UserProfile(
            userId = testFarmerId,
            role = UserRole.FARMER
        )
        every { supplierDoc.toObject(UserProfile::class.java) } returns null
        every { transaction.get(match { it.path == "orders/$testOrderId" }) } returns orderDoc
        every { transaction.get(match { it.path == "order_completion_records/$testOrderId" }) } returns completionDoc
        every { transaction.get(match { it.path == "users/$testFarmerId" }) } returns farmerDoc
        every { transaction.get(match { it.path == "users/$testSupplierId" }) } returns supplierDoc
        coEvery { firestoreService.runTransaction<Order>(any()) } answers {
            firstArg<(Transaction) -> Order>()(transaction)
        }

        val result = orderRepository.updateOrderStatus(testOrderId, OrderStatus.DELIVERED)

        assertTrue(result is Result.Error)
        verify(exactly = 0) { transaction.set(orderRef, any<Order>()) }
    }

    @Test
    fun `duplicate completion attempts do not award additional points`() = runTest {
        val existingOrder = Order(
            orderId = testOrderId,
            listingId = testListingId,
            bidId = "",
            farmerId = testFarmerId,
            supplierId = testSupplierId,
            cropName = "Rice",
            quantity = 5.0,
            quantityUnit = QuantityUnit.KG,
            agreedPrice = 8.0,
            status = OrderStatus.IN_TRANSIT
        )

        val orderRef = mockk<DocumentReference>(relaxed = true)
        val recordRef = mockk<DocumentReference>(relaxed = true)
        every { orderRef.path } returns "orders/$testOrderId"
        every { recordRef.path } returns "order_completion_records/$testOrderId"

        val ordersColl = mockk<CollectionReference>(relaxed = true)
        val recordsColl = mockk<CollectionReference>(relaxed = true)

        every { firestore.collection("orders") } returns ordersColl
        every { ordersColl.document(testOrderId) } returns orderRef

        every { firestore.collection("order_completion_records") } returns recordsColl
        every { recordsColl.document(testOrderId) } returns recordRef

        val mockTransaction = mockk<Transaction>(relaxed = true)
        val mockOrderDoc = mockk<DocumentSnapshot>()
        val mockRecordDoc = mockk<DocumentSnapshot>()

        every { mockOrderDoc.toObject(Order::class.java) } returns existingOrder
        every { mockRecordDoc.exists() } returns true // Completion record already exists!

        every { mockTransaction.get(match<DocumentReference> { it == orderRef || it.path == "orders/$testOrderId" }) } returns mockOrderDoc
        every { mockTransaction.get(match<DocumentReference> { it == recordRef || it.path == "order_completion_records/$testOrderId" }) } returns mockRecordDoc

        coEvery { firestoreService.runTransaction<Order>(any()) } answers {
            val block = firstArg<(Transaction) -> Order>()
            block(mockTransaction)
        }

        val result = orderRepository.updateOrderStatus(testOrderId, OrderStatus.DELIVERED)

        assertTrue(result is Result.Success)
        verify(exactly = 0) { mockTransaction.set(recordRef, any()) }
        verify(exactly = 0) { firestore.collection("users") }
    }

    @Test
    fun `cancelled orders award no points`() = runTest {
        val existingOrder = Order(
            orderId = testOrderId,
            listingId = testListingId,
            bidId = "",
            farmerId = testFarmerId,
            supplierId = testSupplierId,
            cropName = "Rice",
            quantity = 5.0,
            quantityUnit = QuantityUnit.KG,
            agreedPrice = 8.0,
            status = OrderStatus.AWAITING_PICKUP
        )

        val orderRef = mockk<DocumentReference>(relaxed = true)
        val listingRef = mockk<DocumentReference>(relaxed = true)
        every { orderRef.path } returns "orders/$testOrderId"
        every { listingRef.path } returns "listings/$testListingId"

        val ordersColl = mockk<CollectionReference>(relaxed = true)
        val listingsColl = mockk<CollectionReference>(relaxed = true)

        every { firestore.collection("orders") } returns ordersColl
        every { ordersColl.document(testOrderId) } returns orderRef

        every { firestore.collection("listings") } returns listingsColl
        every { listingsColl.document(testListingId) } returns listingRef

        val mockOrderDoc = mockk<DocumentSnapshot>()
        val mockListingDoc = mockk<DocumentSnapshot>()
        every { mockOrderDoc.toObject(Order::class.java) } returns existingOrder
        every { mockListingDoc.toObject(HarvestListing::class.java) } returns null

        val txMock = mockk<Transaction>(relaxed = true)
        every { txMock.get(match<DocumentReference> { it == orderRef || it.path == "orders/$testOrderId" }) } returns mockOrderDoc
        every { txMock.get(match<DocumentReference> { it == listingRef || it.path == "listings/$testListingId" }) } returns mockListingDoc

        coEvery { firestoreService.runTransaction<Pair<Order, HarvestListing?>>(any()) } answers {
            val block = firstArg<(Transaction) -> Pair<Order, HarvestListing?>>()
            block(txMock)
        }

        val result = orderRepository.updateOrderStatus(testOrderId, OrderStatus.CANCELLED)

        assertTrue(result is Result.Success)
        verify(exactly = 0) { firestore.collection("order_completion_records") }
        verify(exactly = 0) { firestore.collection("users") }
    }

    @Test
    fun `pending orders award no points`() = runTest {
        val existingOrder = Order(
            orderId = testOrderId,
            listingId = testListingId,
            bidId = "",
            farmerId = testFarmerId,
            supplierId = testSupplierId,
            cropName = "Rice",
            quantity = 5.0,
            quantityUnit = QuantityUnit.KG,
            agreedPrice = 8.0,
            status = OrderStatus.AWAITING_PICKUP
        )

        val orderRef = mockk<DocumentReference>(relaxed = true)
        every { orderRef.path } returns "orders/$testOrderId"

        val ordersColl = mockk<CollectionReference>(relaxed = true)
        every { firestore.collection("orders") } returns ordersColl
        every { ordersColl.document(testOrderId) } returns orderRef

        val mockOrderDoc = mockk<DocumentSnapshot>()
        every { mockOrderDoc.toObject(Order::class.java) } returns existingOrder

        val txMock = mockk<Transaction>(relaxed = true)
        every { txMock.get(match<DocumentReference> { it == orderRef || it.path == "orders/$testOrderId" }) } returns mockOrderDoc

        coEvery { firestoreService.runTransaction<Order>(any()) } answers {
            val block = firstArg<(Transaction) -> Order>()
            block(txMock)
        }

        val farmerRepo = OrderRepositoryImpl(
            dao = mockk(relaxed = true),
            firestoreService = firestoreService,
            bidDao = mockk(relaxed = true),
            listingDao = mockk(relaxed = true),
            orderRequestDao = mockk(relaxed = true),
            authRepository = farmerAuthRepository(),
            orderCompletionRecordDao = mockk(relaxed = true),
            userProfileDao = mockk(relaxed = true),
            notificationService = mockk<NotificationService>(relaxed = true)
        )

        val result = farmerRepo.updateOrderStatus(testOrderId, OrderStatus.IN_TRANSIT)

        assertTrue(result is Result.Success)
        verify(exactly = 0) { firestore.collection("order_completion_records") }
        verify(exactly = 0) { firestore.collection("users") }
    }
}
