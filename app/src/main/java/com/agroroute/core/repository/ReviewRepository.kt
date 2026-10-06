package com.agroroute.core.repository

import com.agroroute.core.model.Review
import com.agroroute.core.util.Result
import kotlinx.coroutines.flow.Flow

/**
 * Domain contract for all [Review]-related operations.
 */
interface ReviewRepository {

    /** Get all reviews for a specific user (reviews they received). */
    fun getReviewsForUser(userId: String): Flow<Result<List<Review>>>

    /** Get reviews for a specific order. */
    fun getReviewsForOrder(orderId: String): Flow<Result<List<Review>>>

    /** Check if a user has already reviewed the other participant for an order. */
    suspend fun hasUserReviewedOrder(orderId: String, reviewerId: String): Result<Boolean>

    /** Submit a rating and review for the other participant. */
    suspend fun submitReview(review: Review): Result<Unit>

    /** Sync reviews for a user from Firestore to Room. */
    suspend fun syncReviewsForUser(userId: String): Result<Unit>
}
