package com.agroroute.core.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import androidx.room.ColumnInfo
import com.agroroute.core.database.Converters

/**
 * Represents a crop/harvest listing created by a farmer — core marketplace entity.
 */
@Entity(tableName = "harvest_listings")
@TypeConverters(Converters::class)
data class HarvestListing(
    @PrimaryKey
    val listingId: String = "",
    val farmerId: String = "",
    val farmerName: String = "",
    val cropName: String = "",
    val description: String = "",
    val quantity: Double = 0.0,
    val quantityUnit: QuantityUnit = QuantityUnit.KG,
    /** Farmer's minimum acceptable price per unit (LKR). */
    val reservePrice: Double = 0.0,
    val imageUrls: List<String> = emptyList(),
    val location: String = "",
    val status: ListingStatus = ListingStatus.OPEN,
    val biddingEnabled: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val expiresAt: Long? = null,
    val category: ListingCategory? = null,
    val expectedHarvestAt: Long? = null,
    val qualityGrade: String? = null,
    val availableQuantity: Double? = null,
    @ColumnInfo(defaultValue = "0.0")
    val soldQuantity: Double = 0.0,
    @ColumnInfo(defaultValue = "0")
    val isDraft: Boolean = false,
    @ColumnInfo(defaultValue = "READY")
    val harvestStatus: HarvestStatus = HarvestStatus.READY,
    /** Estimated quantity for an EXPECTED (future-harvest) listing. Always an estimate, never actual stock. */
    val estimatedQuantity: Double? = null,
    /** Optional free-text reason recorded when a farmer marks a harvest FAILED. */
    val harvestFailureReason: String? = null,
    /** Last time the farmer changed anything harvest-related (estimate, ready, failed). */
    val harvestUpdatedAt: Long? = null
)

fun HarvestListing.availableStock(): Double =
    (availableQuantity ?: (quantity - soldQuantity)).coerceAtLeast(0.0)

/** True only when the harvest is confirmed harvested and there is real stock on hand. */
fun HarvestListing.hasConfirmedStock(): Boolean =
    harvestStatus == HarvestStatus.READY && availableStock() > 0.0

/** True when the listing may appear in the supplier marketplace. */
fun HarvestListing.isEligibleForMarketplace(): Boolean =
    status == ListingStatus.OPEN && !isDraft &&
        (hasConfirmedStock() || harvestStatus == HarvestStatus.EXPECTED)

fun HarvestListing.acceptsNewBids(): Boolean =
    status == ListingStatus.OPEN && biddingEnabled && !isDraft &&
        (hasConfirmedStock() || harvestStatus == HarvestStatus.EXPECTED)

fun HarvestListing.acceptsDirectOrders(): Boolean =
    status == ListingStatus.OPEN && !biddingEnabled && !isDraft &&
        (hasConfirmedStock() || harvestStatus == HarvestStatus.EXPECTED)

/** Max quantity a pre-harvest (EXPECTED) request may ask for — the farmer's estimate. */
fun HarvestListing.estimatedAvailable(): Double =
    (estimatedQuantity ?: quantity).coerceAtLeast(0.0)

/** Marks the harvest as ready for sale with the actual, measured available quantity. */
fun HarvestListing.markHarvestReady(actualQuantity: Double): HarvestListing {
    require(actualQuantity > 0.0) { "Actual available quantity must be greater than zero" }
    require(harvestStatus != HarvestStatus.FAILED) { "A failed harvest cannot be marked ready" }
    return copy(
        quantity = actualQuantity,
        availableQuantity = actualQuantity,
        soldQuantity = 0.0,
        harvestStatus = HarvestStatus.READY,
        harvestFailureReason = null,
        harvestUpdatedAt = System.currentTimeMillis()
    )
}

/** Marks the harvest as lost/unavailable and stops new purchase requests. */
fun HarvestListing.markHarvestFailed(reason: String?): HarvestListing {
    require(harvestStatus != HarvestStatus.FAILED) { "Harvest is already marked as failed" }
    return copy(
        harvestStatus = HarvestStatus.FAILED,
        biddingEnabled = false,
        harvestFailureReason = reason?.trim()?.ifBlank { null },
        harvestUpdatedAt = System.currentTimeMillis()
    )
}

/** Updates the estimated harvest date and/or estimated quantity for an EXPECTED listing. */
fun HarvestListing.updateHarvestEstimate(
    expectedHarvestAt: Long?,
    estimatedQuantity: Double?
): HarvestListing {
    require(harvestStatus == HarvestStatus.EXPECTED) { "Only an expected harvest can have its estimate updated" }
    require(expectedHarvestAt != null) { "An estimated harvest date is required" }
    require(estimatedQuantity == null || estimatedQuantity > 0.0) {
        "Estimated quantity must be greater than zero"
    }
    return copy(
        expectedHarvestAt = expectedHarvestAt,
        estimatedQuantity = estimatedQuantity,
        quantity = estimatedQuantity ?: quantity,
        harvestUpdatedAt = System.currentTimeMillis()
    )
}

fun HarvestListing.setBiddingEnabled(enabled: Boolean): HarvestListing {
    require(status == ListingStatus.OPEN) { "Only open listings can be paused or resumed" }
    require(harvestStatus != HarvestStatus.FAILED) { "A failed harvest cannot accept bids" }
    require(
        !enabled || (!isDraft && (hasConfirmedStock() || harvestStatus == HarvestStatus.EXPECTED))
    ) {
        "A listing needs available stock, or an expected harvest, before bidding can resume"
    }
    return copy(biddingEnabled = enabled)
}

fun HarvestListing.closeForNewBids(): HarvestListing {
    require(status == ListingStatus.OPEN) { "Only open listings can be closed" }
    return copy(status = ListingStatus.CLOSED, biddingEnabled = false)
}

fun HarvestListing.recordSale(quantitySoldNow: Double): HarvestListing {
    require(quantitySoldNow > 0.0) { "Sale quantity must be greater than zero" }
    val currentAvailable = availableStock()
    require(quantitySoldNow <= currentAvailable) { "Insufficient listing stock" }

    val remaining = (currentAvailable - quantitySoldNow).coerceAtLeast(0.0)
    val isSoldOut = remaining <= 1e-9
    return copy(
        availableQuantity = remaining,
        soldQuantity = soldQuantity + quantitySoldNow,
        status = if (isSoldOut) ListingStatus.FULFILLED else status,
        biddingEnabled = if (isSoldOut) false else biddingEnabled
    )
}

fun HarvestListing.releaseSale(quantityToRelease: Double): HarvestListing {
    require(quantityToRelease > 0.0) { "Released quantity must be greater than zero" }
    val updatedSold = (soldQuantity - quantityToRelease).coerceAtLeast(0.0)
    val updatedAvailable = (availableStock() + quantityToRelease)
        .coerceAtMost((quantity - updatedSold).coerceAtLeast(0.0))
    val reopen = status == ListingStatus.FULFILLED && updatedAvailable > 0.0
    return copy(
        availableQuantity = updatedAvailable,
        soldQuantity = updatedSold,
        status = if (reopen) ListingStatus.OPEN else status,
        biddingEnabled = if (reopen) true else biddingEnabled
    )
}
