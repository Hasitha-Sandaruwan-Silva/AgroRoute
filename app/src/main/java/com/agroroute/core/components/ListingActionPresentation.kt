package com.agroroute.core.components

import com.agroroute.core.location.SelectedLocation
import com.agroroute.core.model.*

/** Presentation of existing transaction restrictions; the transaction revalidates on submit. */
fun listingAcceptanceBlockedReason(listing: HarvestListing?, forBid: Boolean,
                                   quantity: Double? = null, deliveryLocation: String): String? = when {
    listing == null -> "Listing unavailable. Refresh to check its current state."
    listing.harvestStatus == HarvestStatus.FAILED -> "This harvest failed and can no longer be sold"
    listing.harvestStatus == HarvestStatus.EXPECTED -> "Mark the harvest ready first"
    listing.isDraft || listing.status != ListingStatus.OPEN -> "This listing is no longer open"
    listing.biddingEnabled != forBid -> "The listing's purchase mode has changed"
    listing.availableStock() <= 0 -> "This harvest is sold out"
    quantity != null && quantity > listing.availableStock() -> "Requested quantity exceeds remaining stock"
    SelectedLocation.fromStoredText(listing.location) == null -> "Choose a valid Farmer pickup location first"
    SelectedLocation.fromStoredText(deliveryLocation) == null -> "Supplier delivery coordinates are missing"
    else -> null
}
