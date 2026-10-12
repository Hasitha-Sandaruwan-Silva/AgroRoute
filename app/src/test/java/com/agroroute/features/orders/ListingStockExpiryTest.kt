package com.agroroute.features.orders

import com.agroroute.core.model.*
import com.agroroute.core.components.listingAcceptanceBlockedReason
import com.agroroute.core.location.SelectedLocation
import org.junit.Assert.*
import org.junit.Test

class ListingStockExpiryTest {
    private val location = "Farm\n[7, 80]"
    private val listing = HarvestListing(quantity = 100.0, availableQuantity = 100.0, location = location)
    @Test fun exactExpiryBoundaryAndNoDeadlineCompatibility() {
        assertFalse(listing.isExpired(Long.MAX_VALUE))
        val timed = listing.copy(expiresAt = 100)
        assertFalse(timed.isExpired(99)); assertTrue(timed.isExpired(100)); assertTrue(timed.isExpired(101))
        val expired = listing.copy(expiresAt = 1)
        assertFalse(expired.acceptsNewBids()); assertFalse(expired.isEligibleForMarketplace())
        assertFalse(expired.copy(biddingEnabled = false).acceptsDirectOrders())
        assertEquals("Expired", expired.availabilityLabel())
    }
    @Test fun exactCompetingBidScenarioRestoresActionability() {
        val bids = listOf(40.0, 30.0, 70.0).map { Bid(requestedQuantity = it, deliveryLocation = location) }
        val remaining = listing.recordSale(bids[0].requestedQuantity!!)
        assertEquals(60.0, remaining.availableStock(), 0.0)
        assertNull(listingAcceptanceBlockedReason(remaining, true, bids[1].requestedQuantity, location))
        assertTrue(listingAcceptanceBlockedReason(remaining, true, bids[2].requestedQuantity, location)!!.contains("60.0 KG available"))
        assertTrue(bids.all { it.status == BidStatus.PENDING })
        val restored = remaining.releaseSale(40.0, true)
        assertEquals(100.0, restored.availableStock(), 0.0)
        for (bid in bids.drop(1)) assertNull(listingAcceptanceBlockedReason(restored, true, bid.requestedQuantity, location))
    }
    @Test fun soldOutRestorationPreservesDirectPurchaseModeAndExpiry() {
        val direct = listing.copy(biddingEnabled = false).recordSale(100.0)
        assertEquals("Sold Out", direct.availabilityLabel()); assertFalse(direct.acceptsDirectOrders())
        val restored = direct.copy(expiresAt = 1).releaseSale(100.0, false)
        assertFalse(restored.biddingEnabled); assertTrue(restored.isExpired()); assertFalse(restored.acceptsDirectOrders())
        assertEquals(100.0, restored.availableStock(), 0.0)
    }
    @Test fun coordinateRepairCannotChangeCommercialOrHistoricalFields() {
        val legacy = listing.copy(location = "Farm address")
        assertTrue(isPickupCoordinateRepair(legacy, legacy.copy(location = location)))
        for (changed in listOf(legacy.copy(location = location, reservePrice = 1.0), legacy.copy(location = location, quantity = 101.0),
            legacy.copy(location = location, expiresAt = 9999999999999), legacy.copy(location = location, soldQuantity = 1.0)))
            assertFalse(isPickupCoordinateRepair(legacy, changed))
        assertFalse(isPickupCoordinateRepair(listing, listing.copy(location = "Other\n[8, 81]")))
        assertFalse(isPickupCoordinateRepair(legacy.copy(expiresAt = 1), legacy.copy(expiresAt = 1, location = location)))
    }
    @Test fun deviceFixNeverSilentlyChoosesOriginOrMalformedCoordinates() {
        assertFalse(com.agroroute.core.location.usableDeviceCoordinates(0.0, 0.0))
        assertFalse(com.agroroute.core.location.usableDeviceCoordinates(Double.NaN, 80.0))
        assertFalse(com.agroroute.core.location.usableDeviceCoordinates(7.0, 181.0))
        assertTrue(com.agroroute.core.location.usableDeviceCoordinates(7.0, 80.0))
    }
    @Test fun coordinateBoundariesMalformedLegacyAndMissingValues() {
        for (lat in listOf(-90.0,90.0)) for (lng in listOf(-180.0,180.0)) {
            val selected = SelectedLocation("Boundary", lat, lng)
            assertEquals(selected, SelectedLocation.fromStoredText(selected.toStoredText()))
        }
        for (value in listOf("", "Address only", "[91, 80]", "[7, 181]", "[NaN, 80]", "[7, Infinity]", "[80, 1800]", "[1,2] trailing"))
            assertNull(SelectedLocation.fromStoredText(value))
        val fallback = SelectedLocation("", 7.0, 80.0)
        assertEquals(fallback, SelectedLocation.fromStoredText(fallback.toStoredText()))
    }
}
