package com.agroroute.core.components

import com.agroroute.core.location.SelectedLocation
import com.agroroute.core.model.*

/** Presentation of existing transaction restrictions; the transaction revalidates on submit. */
fun listingAcceptanceBlockedReason(listing: HarvestListing?, forBid: Boolean,
                                   quantity: Double? = null, deliveryLocation: String): String? = when {
    listing == null -> "Listing unavailable. Refresh to check its current state."
    listing.isExpired() -> "Listing expired"
    listing.harvestStatus == HarvestStatus.FAILED -> "This harvest failed and can no longer be sold"
    listing.harvestStatus == HarvestStatus.EXPECTED -> "Mark the harvest ready first"
    listing.status == ListingStatus.FULFILLED -> "This harvest is sold out"
    listing.isDraft || listing.status != ListingStatus.OPEN -> "This listing is no longer open"
    listing.biddingEnabled != forBid -> "The listing's purchase mode has changed"
    listing.availableStock() <= 0 -> "This harvest is sold out"
    quantity != null && (!quantity.isFinite() || quantity <= 0) -> "Choose a valid requested quantity"
    quantity != null && quantity > listing.availableStock() -> "Insufficient stock — ${listing.availableStock()} ${listing.quantityUnit.name} available"
    SelectedLocation.fromStoredText(listing.location) == null -> "Choose a valid Farmer pickup location first"
    SelectedLocation.fromStoredText(deliveryLocation) == null -> "Supplier delivery coordinates are missing"
    else -> null
}
