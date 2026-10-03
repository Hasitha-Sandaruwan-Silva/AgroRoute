package com.agroroute.features.supplier.marketplace

import com.agroroute.core.model.HarvestListing
import com.agroroute.core.model.HarvestStatus
import com.agroroute.core.model.ListingStatus
import com.agroroute.core.model.acceptsDirectOrders
import com.agroroute.core.model.acceptsNewBids
import com.agroroute.core.model.hasConfirmedStock
import com.agroroute.core.model.markHarvestFailed
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

    @Test
    fun includesExpectedHarvestListingWithoutConfirmedStock() {
        val listing = HarvestListing(
            status = ListingStatus.OPEN,
            biddingEnabled = false,
            harvestStatus = HarvestStatus.EXPECTED,
            quantity = 20.0,
            availableQuantity = 0.0,
            estimatedQuantity = 20.0
        )

        assertTrue(isSupplierMarketplaceEligible(listing))
        assertTrue(listing.acceptsDirectOrders())
        assertFalse(listing.hasConfirmedStock())
    }

    @Test
    fun excludesFailedHarvestListingFromMarketplace() {
        val listing = HarvestListing(
            status = ListingStatus.OPEN,
            harvestStatus = HarvestStatus.EXPECTED,
            quantity = 20.0,
            estimatedQuantity = 20.0
        ).markHarvestFailed("Storm damage")

        assertFalse(isSupplierMarketplaceEligible(listing))
        assertFalse(listing.acceptsNewBids())
        assertFalse(listing.acceptsDirectOrders())
    }
}