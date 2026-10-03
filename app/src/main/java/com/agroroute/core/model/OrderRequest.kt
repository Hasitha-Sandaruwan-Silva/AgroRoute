package com.agroroute.core.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import androidx.room.Index
import com.agroroute.core.database.Converters

/**
 * Represents a direct order request from a supplier for a fixed-price listing.
 * Used when bidding is disabled on a listing.
 */
@Entity(
    tableName = "order_requests",
    indices = [
        Index(value = ["listingId"]),
        Index(value = ["supplierId"]),
        Index(value = ["farmerId"]),
        Index(value = ["status"])
    ]
)
@TypeConverters(Converters::class)
data class OrderRequest(
    @PrimaryKey
    val requestId: String = "",
    val listingId: String = "",
    val farmerId: String = "",
    val farmerName: String = "",
    val supplierId: String = "",
    val supplierName: String = "",
    val cropName: String = "",
    val requestedQuantity: Double = 0.0,
    val quantityUnit: QuantityUnit = QuantityUnit.KG,
    /** Listed price per unit (from HarvestListing.reservePrice). */
    val listedPrice: Double = 0.0,
    val status: OrderRequestStatus = OrderRequestStatus.PENDING,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
