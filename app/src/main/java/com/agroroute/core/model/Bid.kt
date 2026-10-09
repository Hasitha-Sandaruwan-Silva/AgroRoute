package com.agroroute.core.model

import androidx.room.Entity
import androidx.room.ColumnInfo
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.agroroute.core.database.Converters

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
    val updatedAt: Long = System.currentTimeMillis(),
    /** Supplier-selected address and coordinates for this bid, independent of their profile. */
    @ColumnInfo(defaultValue = "''")
    val deliveryLocation: String = ""
)
