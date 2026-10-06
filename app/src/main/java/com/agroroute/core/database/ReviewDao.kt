package com.agroroute.core.database

import androidx.room.Dao
import androidx.room.Query
import com.agroroute.core.model.Review
import kotlinx.coroutines.flow.Flow

/**
 * DAO for reviews (mutual ratings between order participants).
 */
@Dao
interface ReviewDao {

    @Query("SELECT * FROM reviews WHERE reviewId = :reviewId")
    fun getReviewById(reviewId: String): Flow<Review?>

    @Query("SELECT * FROM reviews WHERE orderId = :orderId")
    fun getReviewsByOrderId(orderId: String): Flow<List<Review>>

    @Query("SELECT * FROM reviews WHERE orderId = :orderId AND reviewerId = :reviewerId")
    suspend fun getReviewByOrderAndReviewer(orderId: String, reviewerId: String): Review?

    @Query("SELECT * FROM reviews WHERE revieweeId = :revieweeId")
    fun getReviewsForUser(revieweeId: String): Flow<List<Review>>

    @Query("SELECT AVG(rating) FROM reviews WHERE revieweeId = :userId")
    fun getAverageRatingForUser(userId: String): Flow<Float?>

    @Query("SELECT * FROM reviews WHERE revieweeId = :userId")
    suspend fun getReviewsForUserSync(userId: String): List<Review>

    @androidx.room.Upsert
    suspend fun upsertReview(review: Review)

    @androidx.room.Upsert
    suspend fun upsertReviews(reviews: List<Review>)
}
