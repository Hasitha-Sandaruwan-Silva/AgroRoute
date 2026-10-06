package com.agroroute.core.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Records that an order has been completed and points awarded.
 * Ensures points are awarded exactly once per order (idempotent).
 */
@Entity(tableName = "order_completion_records")
data class OrderCompletionRecord(
    @PrimaryKey
    val orderId: String = "",
    val farmerId: String = "",
    val supplierId: String = "",
    val pointsAwardedAt: Long = System.currentTimeMillis()
)
