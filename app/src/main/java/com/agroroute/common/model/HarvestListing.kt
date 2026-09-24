package com.agroroute.common.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.agroroute.common.database.Converters

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
    val expiresAt: Long? = null
)
