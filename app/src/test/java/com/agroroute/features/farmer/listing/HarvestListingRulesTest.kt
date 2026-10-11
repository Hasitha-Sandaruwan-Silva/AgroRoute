package com.agroroute.features.farmer.listing

import com.agroroute.core.model.Bid
import com.agroroute.core.model.BidStatus
import com.agroroute.core.model.HarvestListing
import com.agroroute.core.model.ListingCategory
import com.agroroute.core.model.ListingStatus
import com.agroroute.core.model.Order
import com.agroroute.core.model.OrderStatus
import com.agroroute.core.model.UserProfile
import com.agroroute.core.model.UserRole
import com.agroroute.core.model.acceptsNewBids
import com.agroroute.core.model.availableStock
import com.agroroute.core.model.closeForNewBids
import com.agroroute.core.model.recordSale
import com.agroroute.core.model.releaseSale
import com.agroroute.core.model.setBiddingEnabled
import com.agroroute.core.navigation.Screen
import com.agroroute.core.navigation.roleHomeDestination
import com.agroroute.core.repository.AuthRepository
import com.agroroute.core.repository.GoogleSignInOutcome
import com.agroroute.core.repository.BidRepository
import com.agroroute.core.repository.HarvestListingRepository
import com.agroroute.core.repository.ListingPhotoStorage
import com.agroroute.core.repository.ListingPhotoStorageException
import com.agroroute.core.repository.OrderRepository
import com.agroroute.core.repository.listingPhotoStoragePath
import com.agroroute.core.util.Result
import com.agroroute.features.farmer.home.FarmerHomeDestination
import com.agroroute.features.farmer.shell.farmerRoutes
import com.agroroute.features.farmer.bids.highestPendingOffer
import com.agroroute.features.farmer.shell.farmerTabRoutes
import com.agroroute.features.supplier.bidding.PlaceBidUseCase
import com.agroroute.features.farmer.bids.RejectBidUseCase
import com.agroroute.features.farmer.bids.AcceptBidUseCase
import com.agroroute.features.farmer.bids.publishedListingsOwnedBy
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import com.agroroute.core.components.shouldUseListingImageFallback
import com.agroroute.core.components.shouldShowListingImageLoading
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HarvestListingRulesTest {

    @Test
    fun legacyListingUsesOriginalQuantityAsAvailableStock() {
        val listing = HarvestListing(quantity = 12.0)

        assertEquals(12.0, listing.availableStock(), 0.0)
        assertEquals(null, listing.expectedHarvestAt)
        assertEquals(null, listing.qualityGrade)
        assertEquals(null, listing.availableQuantity)
        assertFalse(listing.isDraft)
    }

    @Test
    fun recordingFullSaleMarksListingUnavailable() {
        val updated = HarvestListing(
        location = "North Farm\n[7.000000, 80.000000]",
            status = ListingStatus.OPEN,
            biddingEnabled = true,
            quantity = 7.0,
            availableQuantity = 7.0
        ).recordSale(7.0)

        assertEquals(0.0, updated.availableStock(), 0.0)
        assertEquals(7.0, updated.soldQuantity, 0.0)
        assertEquals(ListingStatus.FULFILLED, updated.status)
        assertFalse(updated.biddingEnabled)
        assertFalse(updated.acceptsNewBids())
    }

    @Test
    fun cancellingCommittedQuantityRestoresStockAndReopensSoldOutListing() {
        val soldOut = HarvestListing(
        location = "North Farm\n[7.000000, 80.000000]",
            status = ListingStatus.FULFILLED,
            biddingEnabled = false,
            quantity = 8.0,
            availableQuantity = 0.0,
            soldQuantity = 8.0
        )

        val restored = soldOut.releaseSale(8.0)

        assertEquals(8.0, restored.availableStock(), 0.0)
        assertEquals(0.0, restored.soldQuantity, 0.0)
        assertEquals(ListingStatus.OPEN, restored.status)
        assertTrue(restored.acceptsNewBids())
    }

    @Test
    fun pausedAndClosedListingsAreNotEligibleForBids() {
        val paused = HarvestListing(biddingEnabled = false)
        val closed = HarvestListing(status = ListingStatus.CLOSED)

        assertFalse(paused.acceptsNewBids())
        assertFalse(closed.acceptsNewBids())
    }

    @Test
    fun pauseResumeAndCloseUseExistingListingStatusSafely() {
        val open = HarvestListing(quantity = 4.0, availableQuantity = 4.0)
        val paused = open.setBiddingEnabled(false)
        val resumed = paused.setBiddingEnabled(true)
        val closed = resumed.closeForNewBids()

        assertFalse(paused.acceptsNewBids())
        assertTrue(resumed.acceptsNewBids())
        assertEquals(ListingStatus.CLOSED, closed.status)
        assertFalse(closed.biddingEnabled)
        assertFalse(closed.acceptsNewBids())
    }

    @Test(expected = IllegalArgumentException::class)
    fun recordingMoreThanAvailableStockIsRejected() {
        HarvestListing(quantity = 5.0, availableQuantity = 2.0).recordSale(3.0)
    }

    @Test
    fun onlySupplierRoleCanPassBidUseCase() = runBlocking {
        val listing = HarvestListing(
        location = "North Farm\n[7.000000, 80.000000]",
            listingId = "listing-1",
            quantity = 10.0,
            availableQuantity = 10.0
        )
        val farmerBidRepository = FakeBidRepository()
        val farmerResult = PlaceBidUseCase(
            farmerBidRepository,
            FakeListingRepository(listing),
            FakeAuthRepository(UserRole.FARMER)
        )(validBid())

        assertTrue(farmerResult is Result.Error)
        assertEquals(0, farmerBidRepository.placeCount)

        val supplierBidRepository = FakeBidRepository()
        val supplierResult = PlaceBidUseCase(
            supplierBidRepository,
            FakeListingRepository(listing),
            FakeAuthRepository(UserRole.SUPPLIER, "supplier-1")
        )(validBid().copy(status = BidStatus.ACCEPTED))

        assertTrue(supplierResult is Result.Success)
        assertEquals(1, supplierBidRepository.placeCount)
        assertEquals(BidStatus.PENDING, supplierBidRepository.storedBids.single().status)
    }

    @Test
    fun supplierCannotSubmitBidUsingAnotherSuppliersUid() = runBlocking {
        val bidRepository = FakeBidRepository()
        val result = PlaceBidUseCase(
            bidRepository,
            FakeListingRepository(
                HarvestListing(
        location = "North Farm\n[7.000000, 80.000000]",
                    listingId = "listing-1",
                    status = ListingStatus.OPEN,
                    quantity = 10.0,
                    availableQuantity = 10.0
                )
            ),
            FakeAuthRepository(UserRole.SUPPLIER, "authenticated-supplier")
        )(validBid().copy(supplierId = "different-supplier"))

        assertTrue(result is Result.Error)
        assertEquals(0, bidRepository.placeCount)
    }

    @Test
    fun farmerInboxFiltersListingsToPublishedListingsOwnedBySignedInFarmer() {
        val listings = listOf(
            HarvestListing(listingId = "owned", farmerId = "farmer-a"),
            HarvestListing(listingId = "other", farmerId = "farmer-b"),
            HarvestListing(listingId = "draft", farmerId = "farmer-a", isDraft = true)
        )

        val ownedListings = publishedListingsOwnedBy(listings, "farmer-a")

        assertEquals(listOf("owned"), ownedListings.map { it.listingId })
    }

    @Test
    fun highestOfferHighlightDoesNotAcceptAnyBid() {
        val lowerOffer = validBid().copy(bidId = "lower", offerPrice = 4.0)
        val higherOffer = validBid().copy(bidId = "higher", offerPrice = 9.0)
        val bids = listOf(lowerOffer, higherOffer)

        assertEquals(higherOffer, highestPendingOffer(bids))
        assertTrue(bids.all { it.status == BidStatus.PENDING })
    }

    @Test
    fun farmerAcceptanceCreatesOrderThroughExistingRepositoryFlow() = runBlocking {
        val listing = HarvestListing(
            listingId = "listing-1",
            farmerId = "FARMER",
            cropName = "Rice",
            quantity = 8.0,
            availableQuantity = 8.0,
            location = "North Farm\n[7.000000, 80.000000]"
        )
        val orders = FakeOrderRepository()
        val bid = validBid().copy(status = BidStatus.PENDING)

        val result = AcceptBidUseCase(
            orders,
            FakeListingRepository(listing),
            FakeAuthRepository(UserRole.FARMER)
        )(bid, listing)

        assertTrue(result is Result.Success)
        val createdOrder = orders.acceptedOrders.single()
        assertEquals(bid.bidId, createdOrder.orderId)
        assertEquals(bid.bidId, createdOrder.bidId)
        assertEquals(bid.supplierId, createdOrder.supplierId)
        assertEquals(listing.availableStock(), createdOrder.quantity, 0.0)
        assertEquals(bid.offerPrice, createdOrder.agreedPrice, 0.0)
    }

    @Test
    fun differentFarmerCannotAcceptBidOrCreateOrder() = runBlocking {
        val listing = HarvestListing(
        location = "North Farm\n[7.000000, 80.000000]",
            listingId = "listing-1",
            farmerId = "listing-owner",
            cropName = "Rice",
            quantity = 8.0,
            availableQuantity = 8.0
        )
        val orders = FakeOrderRepository()

        val result = AcceptBidUseCase(
            orders,
            FakeListingRepository(listing),
            FakeAuthRepository(UserRole.FARMER, "different-farmer")
        )(validBid().copy(status = BidStatus.PENDING), listing)

        assertTrue(result is Result.Error)
        assertTrue(orders.acceptedOrders.isEmpty())
    }

    @Test
    fun draftIsStoredLocallyAndKeepsItsStableId() = runBlocking {
        val repository = FakeListingRepository(HarvestListing())
        val listing = HarvestListing(listingId = "draft-1", cropName = "Rice")

        val result = SaveListingDraftUseCase(repository)(listing)

        assertTrue(result is Result.Success)
        assertEquals("draft-1", repository.savedDraft?.listingId)
        assertTrue(repository.savedDraft?.isDraft == true)
    }

    @Test
    fun futureHarvestDateSurvivesSaveLoadAndDraftResume() = runBlocking {
        val expectedHarvestAt = 1_800_000_000_000L
        val repository = FakeListingRepository(HarvestListing())
        val listing = HarvestListing(
        location = "North Farm\n[7.000000, 80.000000]",
            listingId = "listing-date",
            farmerId = "FARMER",
            cropName = "Rice",
            quantity = 12.0,
            reservePrice = 3.0,
            category = ListingCategory.ORGANIC,
            expectedHarvestAt = expectedHarvestAt
        )

        assertTrue(
            CreateListingUseCase(
                repository,
                FakeListingPhotoStorage(),
                FakeAuthRepository(UserRole.FARMER, "FARMER")
            )(listing) is Result.Success
        )
        val loadedListing = repository.getListingById(listing.listingId).first().getOrNull()
        assertEquals(expectedHarvestAt, loadedListing?.expectedHarvestAt)

        val draft = listing.copy(listingId = "draft-date", isDraft = true)
        assertTrue(SaveListingDraftUseCase(repository)(draft) is Result.Success)
        val loadedDraft = repository.getDraftsByFarmer("FARMER").first().getOrNull()?.single()
        assertEquals(expectedHarvestAt, loadedDraft?.expectedHarvestAt)
        assertEquals(expectedHarvestAt, loadedDraft?.toDraftFormValues()?.expectedHarvestAt)
    }

    @Test
    fun turningBiddingOnPersistsAndAllowsSupplierBid() = runBlocking {
        val farmerId = "farmer-owner"
        val listing = HarvestListing(
        location = "North Farm\n[7.000000, 80.000000]",
            listingId = "listing-1",
            farmerId = farmerId,
            status = ListingStatus.OPEN,
            biddingEnabled = false,
            quantity = 10.0,
            availableQuantity = 10.0
        )
        val listingRepository = FakeListingRepository(listing)

        val toggleResult = ToggleBiddingModeUseCase(
            listingRepository,
            FakeAuthRepository(UserRole.FARMER, farmerId)
        )(listing.listingId, true)

        assertTrue(toggleResult is Result.Success)
        assertTrue(listingRepository.listing.biddingEnabled)
        assertTrue(listingRepository.listing.acceptsNewBids())

        val bidRepository = FakeBidRepository()
        val bidResult = PlaceBidUseCase(
            bidRepository,
            listingRepository,
            FakeAuthRepository(UserRole.SUPPLIER, "supplier-1")
        )(validBid())

        assertTrue(bidResult is Result.Success)
        assertEquals(1, bidRepository.placeCount)
    }

    @Test
    fun turningBiddingOffPersistsRejectsNewBidsAndKeepsExistingBids() = runBlocking {
        val farmerId = "farmer-owner"
        val existingBid = validBid().copy(bidId = "existing-bid", status = BidStatus.PENDING)
        val listingRepository = FakeListingRepository(
            HarvestListing(
        location = "North Farm\n[7.000000, 80.000000]",
                listingId = "listing-1",
                farmerId = farmerId,
                status = ListingStatus.OPEN,
                biddingEnabled = true,
                quantity = 10.0,
                availableQuantity = 10.0
            )
        )
        val bidRepository = FakeBidRepository(mutableListOf(existingBid))

        val toggleResult = ToggleBiddingModeUseCase(
            listingRepository,
            FakeAuthRepository(UserRole.FARMER, farmerId)
        )("listing-1", false)

        assertTrue(toggleResult is Result.Success)
        val bidResult = PlaceBidUseCase(
            bidRepository,
            listingRepository,
            FakeAuthRepository(UserRole.SUPPLIER, "supplier-1")
        )(validBid())

        assertTrue(bidResult is Result.Error)
        assertEquals(0, bidRepository.placeCount)
        assertEquals(listOf(existingBid), bidRepository.getBidsForListing("listing-1").first().getOrNull())
    }

    @Test
    fun farmerCannotChangeAnotherFarmersListingBiddingState() = runBlocking {
        val listingRepository = FakeListingRepository(
            HarvestListing(
        location = "North Farm\n[7.000000, 80.000000]",
                listingId = "listing-1",
                farmerId = "listing-owner",
                status = ListingStatus.OPEN,
                biddingEnabled = true,
                quantity = 10.0,
                availableQuantity = 10.0
            )
        )

        val result = ToggleBiddingModeUseCase(
            listingRepository,
            FakeAuthRepository(UserRole.FARMER, "different-farmer")
        )("listing-1", false)

        assertTrue(result is Result.Error)
        assertTrue(listingRepository.listing.biddingEnabled)
        assertEquals(0, listingRepository.toggleCount)
    }

    @Test
    fun biddingCannotBeReopenedForDraftClosedOrSoldOutListings() = runBlocking {
        val farmerId = "farmer-owner"
        val ineligibleListings = listOf(
            HarvestListing(
        location = "North Farm\n[7.000000, 80.000000]",
                listingId = "draft-listing",
                farmerId = farmerId,
                biddingEnabled = false,
                isDraft = true,
                quantity = 10.0,
                availableQuantity = 10.0
            ),
            HarvestListing(
        location = "North Farm\n[7.000000, 80.000000]",
                listingId = "closed-listing",
                farmerId = farmerId,
                status = ListingStatus.CLOSED,
                biddingEnabled = false,
                quantity = 10.0,
                availableQuantity = 10.0
            ),
            HarvestListing(
        location = "North Farm\n[7.000000, 80.000000]",
                listingId = "sold-out-listing",
                farmerId = farmerId,
                biddingEnabled = false,
                quantity = 10.0,
                availableQuantity = 0.0
            )
        )

        ineligibleListings.forEach { listing ->
            val repository = FakeListingRepository(listing)
            val result = ToggleBiddingModeUseCase(
                repository,
                FakeAuthRepository(UserRole.FARMER, farmerId)
            )(listing.listingId, true)

            assertTrue(result is Result.Error)
            assertFalse(repository.listing.biddingEnabled)
            assertEquals(0, repository.toggleCount)
        }
    }

    @Test
    fun farmerListingActionOpensIncomingBidsNotSupplierPlaceBid() {
        val listingId = "listing-1"
        val route = farmerListingDetailsRoute(listingId)

        assertEquals(Screen.ViewBids.createRoute(listingId), route)
        assertTrue(Screen.ViewBids.route in farmerRoutes)
        assertFalse(Screen.FarmerBids.route in farmerTabRoutes)
        assertTrue(Screen.FarmerBids.route in farmerRoutes)
        assertFalse(route == Screen.PlaceBid.createRoute(listingId))
    }

    @Test
    fun farmerHomeActionsStayWithinFarmerDestinations() {
        val homeRoutes = FarmerHomeDestination.values().map { it.route }.toSet()

        assertTrue(homeRoutes.all { it in farmerRoutes })
        assertFalse(Screen.SupplierMarketplace.route in homeRoutes)
        assertFalse(Screen.PlaceBid.route in homeRoutes)
    }

    @Test
    fun firebaseRolesRouteToTheirOwnAreasOnly() {
        assertEquals(Screen.FarmerHome.route, roleHomeDestination(UserRole.FARMER))
        assertEquals(Screen.SupplierHome.route, roleHomeDestination(UserRole.SUPPLIER))
        assertEquals(null, roleHomeDestination(null))
        assertFalse(roleHomeDestination(UserRole.FARMER) == Screen.SupplierHome.route)
        assertFalse(roleHomeDestination(UserRole.SUPPLIER) == Screen.FarmerHome.route)
    }

    @Test
    fun listingImageShowsFallbackForMissingOrFailedImages() {
        assertTrue(shouldUseListingImageFallback(null, false))
        assertTrue(shouldUseListingImageFallback("https://storage.example/photo", true))
        assertFalse(shouldUseListingImageFallback("https://storage.example/photo", false))
    }

    @Test
    fun listingImageShowsLoadingOnlyWhileAnImageUrlIsBeingFetched() {
        assertTrue(shouldShowListingImageLoading("https://storage.example/photo", true))
        assertFalse(shouldShowListingImageLoading("https://storage.example/photo", false))
        assertFalse(shouldShowListingImageLoading(null, true))
        assertFalse(shouldShowListingImageLoading("", true))
    }

    @Test
    fun photoUploadRetryAppearsOnlyForSelectedPhotoAfterFailure() {
        assertTrue(shouldShowPhotoUploadRetry(true, false, "Photo upload failed"))
        assertFalse(shouldShowPhotoUploadRetry(true, true, "Photo upload failed"))
        assertFalse(shouldShowPhotoUploadRetry(false, false, "Photo upload failed"))
        assertFalse(shouldShowPhotoUploadRetry(true, false, null))
    }

    @Test
    fun photoStorageFailureKeepsItsStageAndOriginalCause() {
        val cause = IllegalStateException("Object does not exist at location")
        val failure = ListingPhotoStorageException(
            ListingPhotoStorageException.Stage.DOWNLOAD_URL,
            cause
        )

        assertEquals(ListingPhotoStorageException.Stage.DOWNLOAD_URL, failure.stage)
        assertEquals(cause, failure.cause)
        assertTrue(failure.message.orEmpty().contains("Object does not exist at location"))
    }

    @Test
    fun categoryIsRequiredBeforeListingCanBePublished() = runBlocking {
        val repository = FakeListingRepository(HarvestListing())
        val incomplete = HarvestListing(
        location = "North Farm\n[7.000000, 80.000000]",
            listingId = "listing-1",
            farmerId = "FARMER",
            cropName = "Rice",
            quantity = 5.0,
            reservePrice = 2.0,
            category = null
        )

        val result = CreateListingUseCase(
            repository,
            FakeListingPhotoStorage(),
            FakeAuthRepository(UserRole.FARMER, "FARMER")
        )(incomplete)

        assertTrue(result is Result.Error)
        assertEquals(null, repository.savedListing)
    }

    @Test
    fun localListingPhotoUploadsToStableScopedPathAndSavesReturnedUrl() = runBlocking {
        val repository = FakeListingRepository(HarvestListing())
        val storage = FakeListingPhotoStorage()
        val listing = HarvestListing(
        location = "North Farm\n[7.000000, 80.000000]",
            listingId = "listing-photo",
            farmerId = "farmer-uid",
            cropName = "Rice",
            quantity = 5.0,
            reservePrice = 2.0,
            category = ListingCategory.ORGANIC,
            imageUrls = listOf("content://picked/image")
        )

        val result = CreateListingUseCase(
            repository,
            storage,
            FakeAuthRepository(UserRole.FARMER, "farmer-uid")
        )(listing)

        assertTrue(result is Result.Success)
        assertEquals("https://storage.example/listing-photo", repository.savedListing?.imageUrls?.single())
        assertEquals("content://picked/image", storage.uploadedUri)
        assertEquals("farmer-uid", storage.uploadedFarmerId)
        assertEquals("listing-photo", storage.uploadedListingId)
        assertEquals("listing_images/farmer-uid/listing-photo/photo", listingPhotoStoragePath("farmer-uid", "listing-photo"))
    }

    @Test
    fun photoUploadFailurePreventsPublishingListing() = runBlocking {
        val repository = FakeListingRepository(HarvestListing())
        val storage = FakeListingPhotoStorage(
            failure = ListingPhotoStorageException(
                ListingPhotoStorageException.Stage.UPLOAD,
                IllegalStateException("offline")
            )
        )
        val listing = HarvestListing(
        location = "North Farm\n[7.000000, 80.000000]",
            listingId = "listing-photo",
            farmerId = "farmer-uid",
            cropName = "Rice",
            quantity = 5.0,
            reservePrice = 2.0,
            category = ListingCategory.ORGANIC,
            imageUrls = listOf("content://picked/image")
        )

        val result = CreateListingUseCase(
            repository,
            storage,
            FakeAuthRepository(UserRole.FARMER, "farmer-uid")
        )(listing)

        assertTrue(result is Result.Error)
        assertTrue((result as Result.Error).cause is ListingPhotoStorageException)
        assertEquals(null, repository.savedListing)
    }

    @Test
    fun farmerCanRejectPendingBidButSupplierCannot() = runBlocking {
        val pendingBid = validBid().copy(status = BidStatus.PENDING)
        val farmerBidRepository = FakeBidRepository(mutableListOf(pendingBid))
        val listingRepository = FakeListingRepository(HarvestListing(listingId = pendingBid.listingId, farmerId = "FARMER"))
        val farmerResult = RejectBidUseCase(
            farmerBidRepository,
            listingRepository,
            FakeAuthRepository(UserRole.FARMER)
        )(pendingBid)

        assertTrue(farmerResult is Result.Success)
        assertEquals(1, farmerBidRepository.statusUpdateCount)
        assertEquals(BidStatus.REJECTED, farmerBidRepository.storedBids.single().status)

        val supplierBidRepository = FakeBidRepository()
        val supplierResult = RejectBidUseCase(
            supplierBidRepository,
            listingRepository,
            FakeAuthRepository(UserRole.SUPPLIER)
        )(pendingBid)

        assertTrue(supplierResult is Result.Error)
        assertEquals(0, supplierBidRepository.statusUpdateCount)
    }

    @Test
    fun farmerCannotRejectBidOnAnotherFarmersListing() = runBlocking {
        val bids = FakeBidRepository()
        val listingRepository = FakeListingRepository(
            HarvestListing(listingId = "listing-1", farmerId = "listing-owner")
        )

        val result = RejectBidUseCase(
            bids,
            listingRepository,
            FakeAuthRepository(UserRole.FARMER, "different-farmer")
        )(validBid().copy(status = BidStatus.PENDING))

        assertTrue(result is Result.Error)
        assertEquals(0, bids.statusUpdateCount)
    }

    private fun validBid() = Bid(deliveryLocation = "Warehouse\n[6.900000, 79.900000]",
        bidId = "bid-1",
        listingId = "listing-1",
        supplierId = "supplier-1",
        offerPrice = 4.0
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
        override suspend fun sendPasswordResetEmail(email: String) = Result.Success(Unit)
    }

    private class FakeOrderRepository : OrderRepository {
        override suspend fun refreshPickupCode(orderId: String) = getPickupVerification(orderId)
        override suspend fun getPickupVerification(orderId: String): Result<com.agroroute.core.model.OrderPickupVerification> = Result.Error("Not used in this fixture")
        override suspend fun verifyPickupCode(orderId: String, code: String): Result<com.agroroute.core.model.OrderPickupVerification> = Result.Error("Not used in this fixture")
        val acceptedOrders = mutableListOf<Order>()

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

        override suspend fun acceptOrderRequestAndCreateOrder(requestId: String): Result<Unit> =
            Result.Success(Unit)

        override suspend fun updateOrderStatus(orderId: String, status: OrderStatus): Result<Unit> =
            Result.Success(Unit)

        override suspend fun updateDeliveryLocation(orderId: String, location: String): Result<Unit> = Result.Success(Unit)
        override suspend fun syncOrders(userId: String): Result<Unit> = Result.Success(Unit)
    }

    private class FakeListingPhotoStorage(
        private val failure: Exception? = null
    ) : ListingPhotoStorage {
        var uploadedUri: String? = null
        var uploadedFarmerId: String? = null
        var uploadedListingId: String? = null

        override suspend fun uploadListingPhoto(
            localUri: String,
            farmerId: String,
            listingId: String
        ): String {
            failure?.let { throw it }
            uploadedUri = localUri
            uploadedFarmerId = farmerId
            uploadedListingId = listingId
            return "https://storage.example/$listingId"
        }
    }

    private class FakeBidRepository(
        val storedBids: MutableList<Bid> = mutableListOf()
    ) : BidRepository {
        var placeCount = 0
        var statusUpdateCount = 0

        override fun getBidsForListing(listingId: String): Flow<Result<List<Bid>>> =
            flowOf(Result.Success(storedBids.filter { it.listingId == listingId }))

        override fun getBidsBySupplier(supplierId: String): Flow<Result<List<Bid>>> =
            flowOf(Result.Success(emptyList()))

        override fun getBidById(bidId: String): Flow<Result<Bid?>> =
            flowOf(Result.Success(storedBids.firstOrNull { it.bidId == bidId }))

        override suspend fun placeBid(bid: Bid): Result<Unit> {
            placeCount++
            storedBids += bid
            return Result.Success(Unit)
        }

        override suspend fun updateBidStatus(bidId: String, status: BidStatus): Result<Unit> {
            statusUpdateCount++
            val index = storedBids.indexOfFirst { it.bidId == bidId }
            if (index >= 0) storedBids[index] = storedBids[index].copy(status = status)
            return Result.Success(Unit)
        }
        override suspend fun withdrawBid(bidId: String) = Result.Success(Unit)
        override suspend fun syncBidsForListing(listingId: String) = Result.Success(Unit)
    }

    private class FakeListingRepository(var listing: HarvestListing) : HarvestListingRepository {
        var savedDraft: HarvestListing? = null
        var savedListing: HarvestListing? = null
        var toggleCount = 0
        override fun getAllListings(): Flow<Result<List<HarvestListing>>> = flowOf(Result.Success(emptyList()))
        override fun getListingsByFarmer(farmerId: String): Flow<Result<List<HarvestListing>>> =
            flowOf(Result.Success(listOf(listing).filter { it.farmerId == farmerId && !it.isDraft }))
        override fun getDraftsByFarmer(farmerId: String): Flow<Result<List<HarvestListing>>> =
            flowOf(Result.Success(listOfNotNull(listing.takeIf { it.farmerId == farmerId && it.isDraft })))
        override fun getListingsByStatus(status: ListingStatus): Flow<Result<List<HarvestListing>>> =
            flowOf(Result.Success(emptyList()))
        override fun getListingById(listingId: String): Flow<Result<HarvestListing?>> =
            flowOf(Result.Success(listing.takeIf { it.listingId == listingId }))
        override suspend fun saveListing(listing: HarvestListing): Result<Unit> {
            savedListing = listing
            this.listing = listing
            return Result.Success(Unit)
        }
        override suspend fun saveDraft(listing: HarvestListing): Result<Unit> {
            savedDraft = listing.copy(isDraft = true)
            this.listing = savedDraft!!
            return Result.Success(Unit)
        }
        override suspend fun deleteListing(listingId: String) = Result.Success(Unit)
        override suspend fun syncListings() = Result.Success(Unit)
        override suspend fun toggleBiddingMode(listingId: String, enabled: Boolean): Result<Unit> = try {
            check(listing.listingId == listingId) { "Listing not found" }
            listing = listing.setBiddingEnabled(enabled)
            toggleCount++
            Result.Success(Unit)
        } catch (error: IllegalArgumentException) {
            Result.Error(error.message ?: "Unable to toggle bidding", error)
        }
    }
}
