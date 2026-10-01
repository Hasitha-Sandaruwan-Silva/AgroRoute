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
import com.agroroute.core.repository.AuthRepository
import com.agroroute.core.repository.BidRepository
import com.agroroute.core.repository.HarvestListingRepository
import com.agroroute.core.util.Result
import com.agroroute.features.supplier.bidding.PlaceBidUseCase
import com.agroroute.features.farmer.bids.RejectBidUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
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
            FakeAuthRepository(UserRole.SUPPLIER)
        )(validBid())

        assertTrue(supplierResult is Result.Success)
        assertEquals(1, supplierBidRepository.placeCount)
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
    fun categoryIsRequiredBeforeListingCanBePublished() = runBlocking {
        val repository = FakeListingRepository(HarvestListing())
        val incomplete = HarvestListing(
            listingId = "listing-1",
            cropName = "Rice",
            quantity = 5.0,
            reservePrice = 2.0,
            category = null
        )

        val result = CreateListingUseCase(repository)(incomplete)

        assertTrue(result is Result.Error)
        assertEquals(null, repository.savedListing)
    }

    @Test
    fun farmerCanRejectPendingBidButSupplierCannot() = runBlocking {
        val farmerBidRepository = FakeBidRepository()
        val pendingBid = validBid().copy(status = BidStatus.PENDING)
        val farmerResult = RejectBidUseCase(
            farmerBidRepository,
            FakeAuthRepository(UserRole.FARMER)
        )(pendingBid)

        assertTrue(farmerResult is Result.Success)
        assertEquals(1, farmerBidRepository.statusUpdateCount)

        val supplierBidRepository = FakeBidRepository()
        val supplierResult = RejectBidUseCase(
            supplierBidRepository,
            FakeAuthRepository(UserRole.SUPPLIER)
        )(pendingBid)

        assertTrue(supplierResult is Result.Error)
        assertEquals(0, supplierBidRepository.statusUpdateCount)
    }

    private fun validBid() = Bid(
        bidId = "bid-1",
        listingId = "listing-1",
        supplierId = "supplier-1",
        offerPrice = 4.0
    )

    private class FakeAuthRepository(private val role: UserRole?) : AuthRepository {
        override val isLoggedIn = role != null
        override val currentUserId = role?.name

        override fun getCurrentUserProfile(): Flow<Result<UserProfile?>> = flowOf(
            Result.Success(role?.let { UserProfile(userId = it.name, role = it) })
        )

        override suspend fun login(email: String, password: String) =
            Result.Error("Not used") as Result<UserProfile>

        override suspend fun register(
            email: String,
            password: String,
            displayName: String,
            role: UserRole
        ) = Result.Error("Not used") as Result<UserProfile>

        override suspend fun logout() = Result.Success(Unit)
        override suspend fun updateProfile(profile: UserProfile) = Result.Success(Unit)
    }

    private class FakeBidRepository : BidRepository {
        var placeCount = 0
        var statusUpdateCount = 0

        override fun getBidsForListing(listingId: String): Flow<Result<List<Bid>>> =
            flowOf(Result.Success(emptyList()))

        override fun getBidsBySupplier(supplierId: String): Flow<Result<List<Bid>>> =
            flowOf(Result.Success(emptyList()))

        override fun getBidById(bidId: String): Flow<Result<Bid?>> =
            flowOf(Result.Success(null))

        override suspend fun placeBid(bid: Bid): Result<Unit> {
            placeCount++
            return Result.Success(Unit)
        }

        override suspend fun updateBidStatus(bidId: String, status: BidStatus): Result<Unit> {
            statusUpdateCount++
            return Result.Success(Unit)
        }
        override suspend fun withdrawBid(bidId: String) = Result.Success(Unit)
        override suspend fun syncBidsForListing(listingId: String) = Result.Success(Unit)
    }

    private class FakeListingRepository(private val listing: HarvestListing) : HarvestListingRepository {
        var savedDraft: HarvestListing? = null
        var savedListing: HarvestListing? = null
        override fun getAllListings(): Flow<Result<List<HarvestListing>>> = flowOf(Result.Success(emptyList()))
        override fun getListingsByFarmer(farmerId: String): Flow<Result<List<HarvestListing>>> =
            flowOf(Result.Success(emptyList()))
        override fun getDraftsByFarmer(farmerId: String): Flow<Result<List<HarvestListing>>> =
            flowOf(Result.Success(emptyList()))
        override fun getListingsByStatus(status: ListingStatus): Flow<Result<List<HarvestListing>>> =
            flowOf(Result.Success(emptyList()))
        override fun getListingById(listingId: String): Flow<Result<HarvestListing?>> =
            flowOf(Result.Success(listing.takeIf { it.listingId == listingId }))
        override suspend fun saveListing(listing: HarvestListing): Result<Unit> {
            savedListing = listing
            return Result.Success(Unit)
        }
        override suspend fun saveDraft(listing: HarvestListing): Result<Unit> {
            savedDraft = listing.copy(isDraft = true)
            return Result.Success(Unit)
        }
        override suspend fun deleteListing(listingId: String) = Result.Success(Unit)
        override suspend fun syncListings() = Result.Success(Unit)
        override suspend fun toggleBiddingMode(listingId: String, enabled: Boolean) = Result.Success(Unit)
    }
}
