package com.agroroute.features.orders

import com.agroroute.core.components.listingAcceptanceBlockedReason
import com.agroroute.core.components.presentationError
import com.agroroute.core.location.SelectedLocation
import com.agroroute.core.model.*
import org.junit.Assert.*
import org.junit.Test

class ListingActionPresentationTest {
    private val location = SelectedLocation("Saved", 7.0, 80.0).toStoredText()
    private val listing = HarvestListing(quantity = 10.0, location = location, biddingEnabled = false)
    @Test fun eligibleReadyStockAndModeEnableAcceptance() {
        assertNull(listingAcceptanceBlockedReason(listing, false, 10.0, location))
        assertNull(listingAcceptanceBlockedReason(listing.copy(biddingEnabled = true), true, deliveryLocation = location))
    }
    @Test fun everyExistingAcceptanceRestrictionIsPresented() {
        for (blocked in listOf(null, listing.copy(isDraft = true), listing.copy(status = ListingStatus.CLOSED),
            listing.copy(status = ListingStatus.FULFILLED), listing.copy(harvestStatus = HarvestStatus.EXPECTED),
            listing.copy(harvestStatus = HarvestStatus.FAILED), listing.copy(biddingEnabled = true),
            listing.copy(availableQuantity = 0.0), listing.copy(location = "Legacy address"))) {
            assertNotNull(listingAcceptanceBlockedReason(blocked, false, 1.0, location))
        }
        assertNotNull(listingAcceptanceBlockedReason(listing, false, 11.0, location))
        assertNotNull(listingAcceptanceBlockedReason(listing, false, 1.0, "Legacy address"))
    }
    @Test fun expiryMetadataDoesNotInventNewRestriction() {
        assertNull(listingAcceptanceBlockedReason(listing.copy(expiresAt = 1), false, 1.0, location))
    }
    @Test fun providerDiagnosticsAreReadableAndBusinessFeedbackIsPreserved() {
        assertEquals("Pickup verification unavailable for this legacy order",
            presentationError("Pickup verification unavailable for this legacy order"))
        assertEquals("Incorrect pickup code", presentationError("Incorrect pickup code"))
        assertEquals("Could not connect. Check your connection and try again.",
            presentationError("UNAVAILABLE: failed to connect to Firestore"))
        assertFalse(presentationError("FirebaseFirestoreException: PERMISSION_DENIED").contains("Firebase"))
    }
}
