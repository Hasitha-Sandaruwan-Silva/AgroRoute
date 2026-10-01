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
    val isDraft: Boolean = false
)

fun HarvestListing.availableStock(): Double =
    (availableQuantity ?: (quantity - soldQuantity)).coerceAtLeast(0.0)

fun HarvestListing.acceptsNewBids(): Boolean =
    status == ListingStatus.OPEN && biddingEnabled && !isDraft && availableStock() > 0.0

fun HarvestListing.setBiddingEnabled(enabled: Boolean): HarvestListing {
    require(status == ListingStatus.OPEN) { "Only open listings can be paused or resumed" }
    require(!enabled || (!isDraft && availableStock() > 0.0)) {
        "A listing needs available stock before bidding can resume"
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
