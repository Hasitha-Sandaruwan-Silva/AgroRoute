package com.agroroute.features.supplier.marketplace

import com.agroroute.core.model.HarvestListing
import com.agroroute.core.model.ListingStatus
import com.agroroute.core.model.acceptsNewBids
import com.agroroute.core.repository.HarvestListingRepository
import com.agroroute.core.util.Result
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MarketplaceEligibilityTest {

    @Test
    fun includesPublishedOpenInStockListingWithoutSupplierBid() {
        val listing = HarvestListing(
            listingId = "no-bid-yet",
            farmerId = "farmer-1",
            status = ListingStatus.OPEN,
            biddingEnabled = true,
            quantity = 8.0,
            availableQuantity = 5.0
        )

        assertTrue(isSupplierMarketplaceEligible(listing))
        assertTrue(listing.acceptsNewBids())
    }

    @Test
    fun includesPublishedOpenInStockListingWhenBiddingIsDisabledButHidesBidAction() {
        val listing = HarvestListing(
            status = ListingStatus.OPEN,
            biddingEnabled = false,
            quantity = 8.0,
            availableQuantity = 5.0
        )

        assertTrue(isSupplierMarketplaceEligible(listing))
        assertFalse(listing.acceptsNewBids())
    }

    @Test
    fun excludesDraftClosedFulfilledAndOutOfStockListings() {
        val open = HarvestListing(status = ListingStatus.OPEN, quantity = 5.0)

        assertFalse(isSupplierMarketplaceEligible(open.copy(isDraft = true)))
        assertFalse(isSupplierMarketplaceEligible(open.copy(status = ListingStatus.CLOSED)))
        assertFalse(isSupplierMarketplaceEligible(open.copy(status = ListingStatus.FULFILLED)))
        assertFalse(isSupplierMarketplaceEligible(open.copy(availableQuantity = 0.0)))
    }


}