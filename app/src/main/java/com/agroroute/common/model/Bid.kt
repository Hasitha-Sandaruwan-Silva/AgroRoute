package com.agroroute.common.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.agroroute.common.database.Converters

/**
 * Represents a price offer placed by a supplier against a [HarvestListing].
 */
@Entity(tableName = "bids")
@TypeConverters(Converters::class)
data class Bid(
    @PrimaryKey
    val bidId: String = "",
    val listingId: String = "",
    val supplierId: String = "",
    val supplierName: String = "",
    /** Offered price per unit (LKR). */
    val offerPrice: Double = 0.0,
    val message: String = "",
    val status: BidStatus = BidStatus.PENDING,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
