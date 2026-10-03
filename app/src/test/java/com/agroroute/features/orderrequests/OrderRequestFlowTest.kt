package com.agroroute.features.orderrequests

import com.agroroute.core.model.HarvestListing
import com.agroroute.core.model.HarvestStatus
import com.agroroute.core.model.ListingStatus
import com.agroroute.core.model.Order
import com.agroroute.core.model.OrderRequest
import com.agroroute.core.model.OrderRequestStatus
import com.agroroute.core.model.OrderStatus
import com.agroroute.core.model.UserProfile
import com.agroroute.core.model.UserRole
import com.agroroute.core.model.acceptsDirectOrders
import com.agroroute.core.model.acceptsNewBids
import com.agroroute.core.model.availableStock
import com.agroroute.core.model.estimatedAvailable
import com.agroroute.core.model.isEligibleForMarketplace
import com.agroroute.core.model.markHarvestFailed
import com.agroroute.core.model.markHarvestReady
import com.agroroute.core.model.recordSale
import com.agroroute.core.model.updateHarvestEstimate
import com.agroroute.core.repository.AuthRepository
import com.agroroute.core.repository.GoogleSignInOutcome
import com.agroroute.core.repository.HarvestListingRepository
import com.agroroute.core.repository.OrderRepository
import com.agroroute.core.repository.OrderRequestRepository
import com.agroroute.core.util.Result
import com.agroroute.features.farmer.orderrequests.AcceptOrderRequestUseCase
import com.agroroute.features.farmer.orderrequests.RejectOrderRequestUseCase
import com.agroroute.features.supplier.orderrequest.PlaceOrderRequestUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OrderRequestFlowTest {

    // ── Supplier: placing requests ────────────────────────────────────

    @Test
    fun supplierCanRequestFullAvailableStockOnFixedPriceListing() = runBlocking {
        val listingRepository = FakeListingRepository(fixedPriceListing())
        val requests = FakeOrderRequestRepository()
        val useCase = PlaceOrderRequestUseCase(
            requests,
            listingRepository,
            FakeAuthRepository(UserRole.SUPPLIER, "supplier-1")
        )

        val result = useCase(validRequest().copy(requestedQuantity = 10.0))

        assertTrue(result is Result.Success)
        assertEquals(1, requests.placedCount)
        val stored = requests.storedRequests.single()
        assertEquals(OrderRequestStatus.PENDING, stored.status)
        assertEquals("farmer-1", stored.farmerId)
        assertEquals("Rice", stored.cropName)
        assertEquals(8.0, stored.listedPrice, 0.0)
    }

    @Test
    fun orderRequestRejectsZeroNegativeAndAboveStockQuantities() = runBlocking {
        val invalidQuantities = listOf(0.0, -1.0, 10.1, 11.0)
        for (quantity in invalidQuantities) {
            val requests = FakeOrderRequestRepository()
            val useCase = PlaceOrderRequestUseCase(
                requests,
                FakeListingRepository(fixedPriceListing()),
                FakeAuthRepository(UserRole.SUPPLIER, "supplier-1")
            )

            val result = useCase(validRequest().copy(requestedQuantity = quantity))

            assertTrue("quantity $quantity should be rejected", result is Result.Error)
            assertEquals(0, requests.placedCount)
        }
    }

    @Test
    fun biddingEnabledListingStillUsesBidFlowAndRejectsDirectRequest() = runBlocking {
        val biddingListing = fixedPriceListing().copy(biddingEnabled = true)
        assertTrue(biddingListing.acceptsNewBids())
        assertFalse(biddingListing.acceptsDirectOrders())
        val requests = FakeOrderRequestRepository()
        val useCase = PlaceOrderRequestUseCase(
            requests,
            FakeListingRepository(biddingListing),
            FakeAuthRepository(UserRole.SUPPLIER, "supplier-1")
        )

        val result = useCase(validRequest())

        assertTrue(result is Result.Error)
        assertEquals(0, requests.placedCount)
    }

    @Test
    fun farmerCannotPlaceOrderRequest() = runBlocking {
        val requests = FakeOrderRequestRepository()
        val useCase = PlaceOrderRequestUseCase(
            requests,
            FakeListingRepository(fixedPriceListing()),
            FakeAuthRepository(UserRole.FARMER, "farmer-1")
        )

        val result = useCase(validRequest())

        assertTrue(result is Result.Error)
        assertEquals(0, requests.placedCount)
    }

    @Test
    fun supplierCannotPlaceRequestUnderAnotherSuppliersUid() = runBlocking {
        val requests = FakeOrderRequestRepository()
        val useCase = PlaceOrderRequestUseCase(
            requests,
            FakeListingRepository(fixedPriceListing()),
            FakeAuthRepository(UserRole.SUPPLIER, "authenticated-supplier")
        )

        val result = useCase(validRequest().copy(supplierId = "different-supplier"))

        assertTrue(result is Result.Error)
        assertEquals(0, requests.placedCount)
    }

    // ── Farmer: accepting ─────────────────────────────────────────────

    @Test
    fun owningFarmerAcceptingPendingRequestCreatesOneOrderAndConsumesStock() = runBlocking {
        val listingRepository = FakeListingRepository(fixedPriceListing())
        val requests = FakeOrderRequestRepository(mutableListOf(validRequest()))
        val orders = FakeOrderRepository(listingRepository, requests)
        val useCase = AcceptOrderRequestUseCase(
            orders,
            requests,
            listingRepository,
            FakeAuthRepository(UserRole.FARMER, "farmer-1")
        )

        val result = useCase(validRequest())

        assertTrue(result is Result.Success)
        assertEquals(1, orders.acceptCount)
        val order = orders.acceptedOrders.single()
        assertEquals("request-1", order.orderId)
        assertEquals("", order.bidId)
        assertEquals("supplier-1", order.supplierId)
        assertEquals(5.0, order.quantity, 0.0)
        assertEquals(8.0, order.agreedPrice, 0.0)
        assertEquals(OrderStatus.AWAITING_PICKUP, order.status)
        assertEquals(5.0, orders.currentListing.availableStock(), 0.0)
        assertEquals(5.0, orders.currentListing.soldQuantity, 0.0)
        assertEquals(OrderRequestStatus.ACCEPTED, requests.storedRequests.single().status)
    }

    @Test
    fun differentFarmerCannotAcceptOrderRequest() = runBlocking {
        val listingRepository = FakeListingRepository(fixedPriceListing())
        val requests = FakeOrderRequestRepository(mutableListOf(validRequest()))
        val orders = FakeOrderRepository(listingRepository, requests)
        val useCase = AcceptOrderRequestUseCase(
            orders,
            requests,
            listingRepository,
            FakeAuthRepository(UserRole.FARMER, "different-farmer")
        )

        val result = useCase(validRequest())

        assertTrue(result is Result.Error)
        assertEquals(0, orders.acceptCount)
        assertTrue(orders.acceptedOrders.isEmpty())
    }

    @Test
    fun duplicateAcceptanceOfSettledRequestIsRejected() = runBlocking {
        val listingRepository = FakeListingRepository(fixedPriceListing())
        val requests = FakeOrderRequestRepository(
            mutableListOf(validRequest().copy(status = OrderRequestStatus.ACCEPTED))
        )
        val orders = FakeOrderRepository(listingRepository, requests)
        val useCase = AcceptOrderRequestUseCase(
            orders,
            requests,
            listingRepository,
            FakeAuthRepository(UserRole.FARMER, "farmer-1")
        )

        val result = useCase(validRequest().copy(status = OrderRequestStatus.ACCEPTED))

        assertTrue(result is Result.Error)
        assertEquals(0, orders.acceptCount)
        assertTrue(orders.acceptedOrders.isEmpty())
    }

    @Test
    fun concurrentRequestsCannotOversellAvailableStock() = runBlocking {
        val listingRepository = FakeListingRepository(fixedPriceListing())
        val requests = FakeOrderRequestRepository(
            mutableListOf(
                validRequest(),
                validRequest().copy(requestId = "request-2", requestedQuantity = 6.0)
            )
        )
        val orders = FakeOrderRepository(listingRepository, requests)
        val useCase = AcceptOrderRequestUseCase(
            orders,
            requests,
            listingRepository,
            FakeAuthRepository(UserRole.FARMER, "farmer-1")
        )

        val first = useCase(validRequest())
        val second = useCase(validRequest().copy(requestId = "request-2", requestedQuantity = 6.0))

        assertTrue(first is Result.Success)
        assertTrue(second is Result.Error)
        assertEquals(1, orders.acceptedOrders.size)
        assertEquals(5.0, orders.currentListing.availableStock(), 0.0)
    }

    // ── Farmer: rejecting ─────────────────────────────────────────────

    @Test
    fun owningFarmerRejectionMarksRequestRejectedWithoutTouchingStock() = runBlocking {
        val listing = fixedPriceListing()
        val listingRepository = FakeListingRepository(listing)
        val requests = FakeOrderRequestRepository(mutableListOf(validRequest()))
        val orders = FakeOrderRepository(listingRepository, requests)
        val useCase = RejectOrderRequestUseCase(
            requests,
            listingRepository,
            FakeAuthRepository(UserRole.FARMER, "farmer-1")
        )

        val result = useCase(validRequest())

        assertTrue(result is Result.Success)
        assertEquals(1, requests.statusUpdateCount)
        assertEquals(OrderRequestStatus.REJECTED, requests.storedRequests.single().status)
        assertEquals(0, orders.acceptCount)
        assertTrue(orders.acceptedOrders.isEmpty())
        assertEquals(10.0, listingRepository.listing.availableStock(), 0.0)
        assertEquals(0.0, listingRepository.listing.soldQuantity, 0.0)
    }

    @Test
    fun differentFarmerCannotRejectOrderRequest() = runBlocking {
        val listingRepository = FakeListingRepository(fixedPriceListing())
        val requests = FakeOrderRequestRepository(mutableListOf(validRequest()))
        val useCase = RejectOrderRequestUseCase(
            requests,
            listingRepository,
            FakeAuthRepository(UserRole.FARMER, "different-farmer")
        )

        val result = useCase(validRequest())

        assertTrue(result is Result.Error)
        assertEquals(0, requests.statusUpdateCount)
        assertEquals(OrderRequestStatus.PENDING, requests.storedRequests.single().status)
    }

    // ── Stock model guards (shared with bid acceptance) ───────────────

    @Test
    fun recordSaleRejectsSalesAboveAvailableStock() {
        val updated = fixedPriceListing().recordSale(5.0)

        assertEquals(5.0, updated.availableStock(), 0.0)
        assertEquals(5.0, updated.soldQuantity, 0.0)
        try {
            updated.recordSale(6.0)
            org.junit.Assert.fail("Selling more than available stock must be rejected")
        } catch (expected: IllegalArgumentException) {
        }
    }

    // ── Ownership filtering ───────────────────────────────────────────

    @Test
    fun supplierSeesOnlyOwnRequestsAndOrders() = runBlocking {
        val requests = FakeOrderRequestRepository(
            mutableListOf(
                validRequest(),
                validRequest().copy(requestId = "request-2", supplierId = "supplier-2")
            )
        )
        val orders = FakeOrderRepository(
            FakeListingRepository(fixedPriceListing()),
            FakeOrderRequestRepository()
        )
        orders.acceptedOrders += Order(orderId = "order-1", supplierId = "supplier-1")
        orders.acceptedOrders += Order(orderId = "order-2", supplierId = "supplier-2")

        val ownRequests = requests.getRequestsForSupplier("supplier-1").first().getOrNull().orEmpty()
        val ownOrders = orders.getOrdersForSupplier("supplier-1").first().getOrNull().orEmpty()

        assertEquals(listOf("request-1"), ownRequests.map { it.requestId })
        assertEquals(listOf("order-1"), ownOrders.map { it.orderId })
    }

    // ── Pre-harvest (future-harvest) flows ───────────────────────────

    @Test
    fun supplierCanPlacePreHarvestRequestWithinEstimatedQuantity() = runBlocking {
        val requests = FakeOrderRequestRepository()
        val useCase = PlaceOrderRequestUseCase(
            requests,
            FakeListingRepository(expectedHarvestListing()),
            FakeAuthRepository(UserRole.SUPPLIER, "supplier-1")
        )

        val result = useCase(validRequest().copy(requestedQuantity = 20.0))

        assertTrue(result is Result.Success)
        val stored = requests.storedRequests.single()
        assertTrue(stored.isPreHarvest)
        assertEquals(OrderRequestStatus.PENDING, stored.status)
        assertEquals("farmer-1", stored.farmerId)
        assertEquals("Rice", stored.cropName)
        assertEquals(8.0, stored.listedPrice, 0.0)
    }

    @Test
    fun preHarvestRequestAboveEstimatedQuantityIsRejected() = runBlocking {
        val requests = FakeOrderRequestRepository()
        val useCase = PlaceOrderRequestUseCase(
            requests,
            FakeListingRepository(expectedHarvestListing()),
            FakeAuthRepository(UserRole.SUPPLIER, "supplier-1")
        )

        val result = useCase(validRequest().copy(requestedQuantity = 20.5))

        assertTrue(result is Result.Error)
        assertEquals(0, requests.placedCount)
    }

    @Test
    fun preHarvestRequestDoesNotReserveStockOrCreateOrder() = runBlocking {
        val listingRepository = FakeListingRepository(expectedHarvestListing())
        val requests = FakeOrderRequestRepository()
        val orders = FakeOrderRepository(listingRepository, requests)

        val result = PlaceOrderRequestUseCase(
            requests,
            listingRepository,
            FakeAuthRepository(UserRole.SUPPLIER, "supplier-1")
        )(validRequest().copy(requestedQuantity = 5.0))

        assertTrue(result is Result.Success)
        assertTrue(requests.storedRequests.single().isPreHarvest)
        assertEquals(OrderRequestStatus.PENDING, requests.storedRequests.single().status)
        assertEquals(0.0, listingRepository.listing.availableStock(), 0.0)
        assertEquals(0.0, listingRepository.listing.soldQuantity, 0.0)
        assertEquals(0, orders.acceptCount)
        assertTrue(orders.acceptedOrders.isEmpty())
    }

    @Test
    fun farmerCannotAcceptPreHarvestRequestUntilHarvestIsMarkedReady() = runBlocking {
        val listingRepository = FakeListingRepository(expectedHarvestListing())
        val requests = FakeOrderRequestRepository(
            mutableListOf(validRequest().copy(isPreHarvest = true))
        )
        val orders = FakeOrderRepository(listingRepository, requests)
        val useCase = AcceptOrderRequestUseCase(
            orders,
            requests,
            listingRepository,
            FakeAuthRepository(UserRole.FARMER, "farmer-1")
        )

        val result = useCase(validRequest().copy(isPreHarvest = true))

        assertTrue(result is Result.Error)
        assertEquals(0, orders.acceptCount)
        assertTrue(orders.acceptedOrders.isEmpty())
        assertEquals(OrderRequestStatus.PENDING, requests.storedRequests.single().status)
    }

    @Test
    fun farmerCannotAcceptRequestAfterHarvestMarkedFailed() = runBlocking {
        val listingRepository = FakeListingRepository(
            expectedHarvestListing().markHarvestFailed("Flood damage")
        )
        val requests = FakeOrderRequestRepository(
            mutableListOf(validRequest().copy(isPreHarvest = true))
        )
        val orders = FakeOrderRepository(listingRepository, requests)
        val useCase = AcceptOrderRequestUseCase(
            orders,
            requests,
            listingRepository,
            FakeAuthRepository(UserRole.FARMER, "farmer-1")
        )

        val result = useCase(validRequest().copy(isPreHarvest = true))

        assertTrue(result is Result.Error)
        assertEquals(0, orders.acceptCount)
        assertTrue(orders.acceptedOrders.isEmpty())
        assertEquals(OrderRequestStatus.PENDING, requests.storedRequests.single().status)
    }

    @Test
    fun farmerCanAcceptPreHarvestRequestAfterHarvestConfirmedReady() = runBlocking {
        val listingRepository = FakeListingRepository(
            expectedHarvestListing().markHarvestReady(8.0)
        )
        val requests = FakeOrderRequestRepository(
            mutableListOf(validRequest().copy(isPreHarvest = true))
        )
        val orders = FakeOrderRepository(listingRepository, requests)
        val useCase = AcceptOrderRequestUseCase(
            orders,
            requests,
            listingRepository,
            FakeAuthRepository(UserRole.FARMER, "farmer-1")
        )

        val result = useCase(validRequest().copy(isPreHarvest = true))

        assertTrue(result is Result.Success)
        assertEquals(1, orders.acceptCount)
        val order = orders.acceptedOrders.single()
        assertEquals("request-1", order.orderId)
        assertEquals("", order.bidId)
        assertEquals("supplier-1", order.supplierId)
        assertEquals(5.0, order.quantity, 0.0)
        assertEquals(8.0, order.agreedPrice, 0.0)
        assertEquals(OrderStatus.AWAITING_PICKUP, order.status)
        assertEquals(HarvestStatus.READY, orders.currentListing.harvestStatus)
        assertEquals(3.0, orders.currentListing.availableStock(), 0.0)
        assertEquals(5.0, orders.currentListing.soldQuantity, 0.0)
        assertEquals(OrderRequestStatus.ACCEPTED, requests.storedRequests.single().status)
    }

    // ── Harvest estimate model guards ────────────────────────────────

    @Test
    fun updatingHarvestEstimateKeepsListingExpectedAndTracksTheChange() {
        val updated = expectedHarvestListing().updateHarvestEstimate(1_900_000_000_000L, 25.0)

        assertEquals(HarvestStatus.EXPECTED, updated.harvestStatus)
        assertEquals(1_900_000_000_000L, updated.expectedHarvestAt)
        assertEquals(25.0, updated.estimatedQuantity!!, 0.0)
        assertEquals(25.0, updated.estimatedAvailable(), 0.0)
        assertTrue(updated.harvestUpdatedAt != null)
    }

    @Test
    fun markingHarvestFailedStopsAllPurchasePathsAndKeepsStockUntouched() {
        val failed = expectedHarvestListing().markHarvestFailed("  Storm damage  ")

        assertEquals(HarvestStatus.FAILED, failed.harvestStatus)
        assertEquals("Storm damage", failed.harvestFailureReason)
        assertFalse(failed.acceptsDirectOrders())
        assertFalse(failed.acceptsNewBids())
        assertFalse(failed.isEligibleForMarketplace())
        assertEquals(0.0, failed.soldQuantity, 0.0)
        assertTrue(failed.harvestUpdatedAt != null)
    }

    @Test
    fun markingHarvestReadyRecordsActualQuantityAsTheOnlyRealStock() {
        val ready = expectedHarvestListing().markHarvestReady(6.0)

        assertEquals(HarvestStatus.READY, ready.harvestStatus)
        assertEquals(6.0, ready.availableStock(), 0.0)
        assertEquals(0.0, ready.soldQuantity, 0.0)
        assertEquals(null, ready.harvestFailureReason)
        try {
            expectedHarvestListing().markHarvestFailed(null).markHarvestReady(6.0)
            org.junit.Assert.fail("A failed harvest cannot be marked ready")
        } catch (expected: IllegalArgumentException) {
        }
    }

    private fun fixedPriceListing() = HarvestListing(
        listingId = "listing-1",
        farmerId = "farmer-1",
        farmerName = "Farmer One",
        cropName = "Rice",
        status = ListingStatus.OPEN,
        biddingEnabled = false,
        quantity = 10.0,
        availableQuantity = 10.0,
        reservePrice = 8.0
    )

    private fun expectedHarvestListing() = HarvestListing(
        listingId = "listing-1",
        farmerId = "farmer-1",
        farmerName = "Farmer One",
        cropName = "Rice",
        status = ListingStatus.OPEN,
        biddingEnabled = false,
        harvestStatus = HarvestStatus.EXPECTED,
        quantity = 20.0,
        availableQuantity = 0.0,
        estimatedQuantity = 20.0,
        expectedHarvestAt = 1_800_000_000_000L,
        reservePrice = 8.0
    )

    private fun validRequest() = OrderRequest(
        requestId = "request-1",
        listingId = "listing-1",
        farmerId = "farmer-1",
        supplierId = "supplier-1",
        requestedQuantity = 5.0,
        listedPrice = 8.0
    )

    private class FakeAuthRepository(
        private val role: UserRole?,
        override val currentUserId: String? = role?.name
    ) : AuthRepository {
        override val isLoggedIn = currentUserId != null

        override fun getCurrentUserProfile(): Flow<Result<UserProfile?>> = flowOf(
            Result.Success(role?.let { UserProfile(userId = currentUserId.orEmpty(), role = it) })
        )

        override suspend fun restoreSession(): Result<UserProfile?> =
            Result.Success(role?.let { UserProfile(userId = currentUserId.orEmpty(), role = it) })

        override suspend fun login(email: String, password: String) =
            Result.Error("Not used") as Result<UserProfile>

        override suspend fun signInWithGoogle(idToken: String): Result<GoogleSignInOutcome> =
            Result.Error("Not used")

        override suspend fun completeGoogleSignIn(role: UserRole): Result<UserProfile> =
            Result.Error("Not used")

        override suspend fun register(
            email: String,
            password: String,
            displayName: String,
            role: UserRole
        ) = Result.Error("Not used") as Result<UserProfile>

        override suspend fun logout() = Result.Success(Unit)
        override suspend fun updateProfile(profile: UserProfile) = Result.Success(Unit)
    }

    private class FakeListingRepository(var listing: HarvestListing) : HarvestListingRepository {
        override fun getAllListings(): Flow<Result<List<HarvestListing>>> =
            flowOf(Result.Success(listOf(listing).filter { !it.isDraft }))

        override fun getListingsByFarmer(farmerId: String): Flow<Result<List<HarvestListing>>> =
            flowOf(Result.Success(listOf(listing).filter { it.farmerId == farmerId && !it.isDraft }))

        override fun getDraftsByFarmer(farmerId: String): Flow<Result<List<HarvestListing>>> =
            flowOf(Result.Success(listOfNotNull(listing.takeIf { it.farmerId == farmerId && it.isDraft })))

        override fun getListingsByStatus(status: ListingStatus): Flow<Result<List<HarvestListing>>> =
            flowOf(Result.Success(listOf(listing).filter { it.status == status }))

        override fun getListingById(listingId: String): Flow<Result<HarvestListing?>> =
            flowOf(Result.Success(listing.takeIf { it.listingId == listingId }))

        override suspend fun saveListing(listing: HarvestListing): Result<Unit> {
            this.listing = listing
            return Result.Success(Unit)
        }

        override suspend fun saveDraft(listing: HarvestListing): Result<Unit> {
            this.listing = listing.copy(isDraft = true)
            return Result.Success(Unit)
        }

        override suspend fun deleteListing(listingId: String) = Result.Success(Unit)
        override suspend fun syncListings() = Result.Success(Unit)
        override suspend fun toggleBiddingMode(listingId: String, enabled: Boolean) =
            Result.Success(Unit)
    }

    private class FakeOrderRequestRepository(
        val storedRequests: MutableList<OrderRequest> = mutableListOf()
    ) : OrderRequestRepository {
        var placedCount = 0
        var statusUpdateCount = 0

        override fun getRequestsForFarmer(farmerId: String): Flow<Result<List<OrderRequest>>> =
            flowOf(Result.Success(storedRequests.filter { it.farmerId == farmerId }))

        override fun getRequestsForSupplier(supplierId: String): Flow<Result<List<OrderRequest>>> =
            flowOf(Result.Success(storedRequests.filter { it.supplierId == supplierId }))

        override fun getRequestsForListing(listingId: String): Flow<Result<List<OrderRequest>>> =
            flowOf(Result.Success(storedRequests.filter { it.listingId == listingId }))

        override fun getRequestById(requestId: String): Flow<Result<OrderRequest?>> =
            flowOf(Result.Success(storedRequests.firstOrNull { it.requestId == requestId }))

        override suspend fun placeOrderRequest(request: OrderRequest): Result<Unit> {
            placedCount++
            storedRequests += request
            return Result.Success(Unit)
        }

        override suspend fun updateRequestStatus(requestId: String, status: OrderRequestStatus): Result<Unit> {
            statusUpdateCount++
            val index = storedRequests.indexOfFirst { it.requestId == requestId }
            if (index >= 0) storedRequests[index] = storedRequests[index].copy(status = status)
            return Result.Success(Unit)
        }

        override suspend fun cancelRequest(requestId: String): Result<Unit> =
            updateRequestStatus(requestId, OrderRequestStatus.CANCELLED)

        override suspend fun syncRequestsForListing(listingId: String) = Result.Success(Unit)
        override suspend fun syncRequestsForUser(userId: String) = Result.Success(Unit)
    }

    /**
     * Mirrors the real [OrderRepository.acceptOrderRequestAndCreateOrder] transaction guards
     * (pending request + stock sufficiency) so accept-flow behaviour can be unit tested
     * without Firestore.
     */
    private class FakeOrderRepository(
        private val listingRepository: FakeListingRepository,
        private val requests: FakeOrderRequestRepository
    ) : OrderRepository {
        val acceptedOrders = mutableListOf<Order>()
        var acceptCount = 0
        var currentListing: HarvestListing
            private set

        init {
            currentListing = listingRepository.listing
        }

        override suspend fun acceptOrderRequestAndCreateOrder(requestId: String): Result<Unit> {
            acceptCount++
            val request = requests.storedRequests.firstOrNull { it.requestId == requestId }
                ?: return Result.Error("Order request not found")
            if (request.status != OrderRequestStatus.PENDING) {
                return Result.Error("This order request is no longer pending")
            }
            if (request.requestedQuantity <= 0.0 ||
                request.requestedQuantity > currentListing.availableStock()
            ) {
                return Result.Error("Insufficient listing stock for this order request")
            }
            acceptedOrders += Order(
                orderId = request.requestId,
                listingId = request.listingId,
                bidId = "",
                farmerId = request.farmerId,
                supplierId = request.supplierId,
                cropName = request.cropName,
                quantity = request.requestedQuantity,
                agreedPrice = request.listedPrice,
                status = OrderStatus.AWAITING_PICKUP
            )
            currentListing = currentListing.recordSale(request.requestedQuantity)
            requests.updateRequestStatus(requestId, OrderRequestStatus.ACCEPTED)
            return Result.Success(Unit)
        }

        override fun getOrdersForFarmer(farmerId: String): Flow<Result<List<Order>>> =
            flowOf(Result.Success(acceptedOrders.filter { it.farmerId == farmerId }))

        override fun getOrdersForSupplier(supplierId: String): Flow<Result<List<Order>>> =
            flowOf(Result.Success(acceptedOrders.filter { it.supplierId == supplierId }))

        override fun getOrderById(orderId: String): Flow<Result<Order?>> =
            flowOf(Result.Success(acceptedOrders.firstOrNull { it.orderId == orderId }))

        override suspend fun createOrder(order: Order): Result<Unit> {
            acceptedOrders += order
            return Result.Success(Unit)
        }

        override suspend fun acceptBidAndCreateOrder(order: Order): Result<Unit> {
            acceptedOrders += order
            return Result.Success(Unit)
        }

        override suspend fun updateOrderStatus(orderId: String, status: OrderStatus): Result<Unit> =
            Result.Success(Unit)

        override suspend fun syncOrders(userId: String) = Result.Success(Unit)
    }
}
