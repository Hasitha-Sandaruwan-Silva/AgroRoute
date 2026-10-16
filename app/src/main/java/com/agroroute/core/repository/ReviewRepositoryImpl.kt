package com.agroroute.core.repository

import com.agroroute.core.database.ReviewDao
import com.agroroute.core.model.Order
import com.agroroute.core.model.OrderStatus
import com.agroroute.core.model.Review
import com.agroroute.core.model.UserRole
import com.agroroute.core.remote.FirestoreService
import com.agroroute.core.util.Constants
import com.agroroute.core.util.Result
import com.agroroute.core.util.safeCall
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implements [ReviewRepository] using the sync-on-fetch strategy.
 */
@Singleton
class ReviewRepositoryImpl @Inject constructor(
    private val dao: ReviewDao,
    private val firestoreService: FirestoreService,
    private val authRepository: AuthRepository,
    private val orderRepository: OrderRepository
) : ReviewRepository {

    override fun getReviewsForUser(userId: String): Flow<Result<List<Review>>> =
        dao.getReviewsForUser(userId)
            .map<List<Review>, Result<List<Review>>> { Result.Success(it) }
            .catch { e -> emit(Result.Error("Couldn't load reviews. Check your connection and try again.", e)) }

    override fun getReviewsForOrder(orderId: String): Flow<Result<List<Review>>> =
        dao.getReviewsByOrderId(orderId)
            .map<List<Review>, Result<List<Review>>> { Result.Success(it) }
            .catch { e -> emit(Result.Error("Couldn't load reviews. Check your connection and try again.", e)) }

    override suspend fun hasUserReviewedOrder(orderId: String, reviewerId: String): Result<Boolean> = safeCall {
        val existing = dao.getReviewByOrderAndReviewer(orderId, reviewerId)
        if (existing != null) true else {
            check(authRepository.currentUserId == reviewerId) { "Sign in to check your reviews" }
            // Incoming-review sync does not include outgoing reviews made on another device.
            val outgoing = firestoreService.queryOnceFromServer(
                Constants.COLLECTION_REVIEWS, "reviewerId", reviewerId, Review::class.java)
            dao.upsertReviews(outgoing)
            outgoing.any { it.orderId == orderId }
        }
    }

    override suspend fun submitReview(review: Review): Result<Unit> = safeCall {
        val userId = authRepository.currentUserId ?: error("Sign in to submit a review")
        val profile = authRepository.getCurrentUserProfile().first().getOrNull()
            ?: error("Account profile is unavailable")
        check(review.reviewerId == userId) { "You can only submit reviews as yourself" }

        // Enforce deterministic reviewId for database-level single review per reviewer per order
        val targetReview = review.copy(
            reviewId = "${review.orderId}_${review.reviewerId}",
            comment = review.comment.trim()
        )

        // Verify the order exists and is completed
        val orderFlow = orderRepository.getOrderById(targetReview.orderId)
        val orderResult = orderFlow.first()
        val order = when (orderResult) {
            is Result.Success -> orderResult.data
            is Result.Error -> error(orderResult.message ?: "Failed to load order")
            is Result.Loading -> error("Order is still loading")
        } ?: error("Order not found")

        // Verify order is completed (DELIVERED)
        check(order.status == OrderStatus.DELIVERED) {
            "You can only review completed orders"
        }

        // Verify reviewer is a participant in the order
        check(targetReview.reviewerId == order.farmerId || targetReview.reviewerId == order.supplierId) {
            "You can only review orders you participated in"
        }

        // Verify reviewee is the other participant
        val otherParticipantId = if (targetReview.reviewerId == order.farmerId) order.supplierId else order.farmerId
        check(targetReview.revieweeId == otherParticipantId) {
            "You can only review the other participant in the order"
        }

        // Check if user already reviewed this order
        val existingReview = dao.getReviewByOrderAndReviewer(targetReview.orderId, targetReview.reviewerId)
        check(existingReview == null) {
            "You have already reviewed this order"
        }

        // Validate rating
        check(targetReview.rating in 1..5) {
            "Rating must be between 1 and 5 stars"
        }
        check(targetReview.comment.isNotBlank()) {
            "A written review is required"
        }

        // Write to Firestore
        firestoreService.set(Constants.COLLECTION_REVIEWS, targetReview.reviewId, targetReview)

        // Write to Room
        dao.upsertReview(targetReview)
    }

    override suspend fun syncReviewsForUser(userId: String): Result<Unit> = safeCall {
        check(authRepository.currentUserId == userId) { "You can only refresh your own reviews" }
        val reviews = firestoreService.queryFlow<Review>(
            Constants.COLLECTION_REVIEWS,
            "revieweeId",
            userId
        ).first()
        dao.upsertReviews(reviews.distinctBy { it.reviewId })
    }
}
