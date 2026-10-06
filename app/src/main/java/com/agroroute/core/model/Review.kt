package com.agroroute.core.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Represents a rating and review that one participant gives another for a completed order.
 * Each order can have at most two reviews: Farmer rates Supplier, Supplier rates Farmer.
 */
@Entity(tableName = "reviews")
data class Review(
    @PrimaryKey
    val reviewId: String = "",
    val orderId: String = "",
    val reviewerId: String = "",
    val revieweeId: String = "",
    val rating: Int = 0, // 1-5 stars
    val comment: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
