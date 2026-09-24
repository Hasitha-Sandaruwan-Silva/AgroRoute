package com.agroroute.core.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.agroroute.core.database.Converters

/**
 * Represents a confirmed transaction created when a farmer accepts a supplier's [Bid].
 */
@Entity(tableName = "orders")
@TypeConverters(Converters::class)
data class Order(
    @PrimaryKey
    val orderId: String = "",
    val listingId: String = "",
    val bidId: String = "",
    val farmerId: String = "",
    val supplierId: String = "",
    val cropName: String = "",
    val quantity: Double = 0.0,
    val quantityUnit: QuantityUnit = QuantityUnit.KG,
    /** Agreed price per unit (taken from the accepted bid). */
    val agreedPrice: Double = 0.0,
    val status: OrderStatus = OrderStatus.AWAITING_PICKUP,
    val pickupLocation: String = "",
    val deliveryLocation: String = "",
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
