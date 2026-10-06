package com.agroroute.features.reviews

import com.agroroute.core.database.ReviewDao
import com.agroroute.core.model.Order
import com.agroroute.core.model.OrderStatus
import com.agroroute.core.model.Review
import com.agroroute.core.model.UserProfile
import com.agroroute.core.model.UserRole
import com.agroroute.core.remote.FirestoreService
import com.agroroute.core.repository.AuthRepository
import com.agroroute.core.repository.OrderRepository
import com.agroroute.core.repository.ReviewRepositoryImpl
import com.agroroute.core.util.Result
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ReviewConstraintsTest {

    private lateinit var reviewRepository: ReviewRepositoryImpl
    private val fakeAuthRepository = mockk<AuthRepository>(relaxed = true)
    private val fakeOrderRepository = mockk<OrderRepository>(relaxed = true)
    private val fakeReviewDao = mockk<ReviewDao>(relaxed = true)
    private val firestoreService = mockk<FirestoreService>(relaxed = true)

    private val testOrderId = "order123"
    private val testFarmerId = "farmer123"
    private val testSupplierId = "supplier123"

    @Before
    fun setup() {
        reviewRepository = ReviewRepositoryImpl(
            dao = fakeReviewDao,
            firestoreService = firestoreService,
            authRepository = fakeAuthRepository,
            orderRepository = fakeOrderRepository
        )
    }

    @Test
    fun `review can be submitted by farmer after order completion`() = runTest {
        val order = Order(
            orderId = testOrderId,
            farmerId = testFarmerId,
            supplierId = testSupplierId,
            status = OrderStatus.DELIVERED
        )
        val review = Review(
            reviewId = "${testOrderId}_${testFarmerId}",
            orderId = testOrderId,
            reviewerId = testFarmerId,
            revieweeId = testSupplierId,
            rating = 5,
            comment = "Great supplier!"
        )

        every { fakeAuthRepository.currentUserId } returns testFarmerId
        every { fakeAuthRepository.getCurrentUserProfile() } returns flowOf(Result.Success(UserProfile(userId = testFarmerId, role = UserRole.FARMER)))
        every { fakeOrderRepository.getOrderById(testOrderId) } returns flowOf(Result.Success(order))
        coEvery { fakeReviewDao.getReviewByOrderAndReviewer(testOrderId, testFarmerId) } returns null

        val result = reviewRepository.submitReview(review)

        assertTrue(result is Result.Success)
    }

    @Test
    fun `review can be submitted by supplier after order completion`() = runTest {
        val order = Order(
            orderId = testOrderId,
            farmerId = testFarmerId,
            supplierId = testSupplierId,
            status = OrderStatus.DELIVERED
        )
        val review = Review(
            reviewId = "${testOrderId}_${testSupplierId}",
            orderId = testOrderId,
            reviewerId = testSupplierId,
            revieweeId = testFarmerId,
            rating = 4,
            comment = "Good farmer"
        )

        every { fakeAuthRepository.currentUserId } returns testSupplierId
        every { fakeAuthRepository.getCurrentUserProfile() } returns flowOf(Result.Success(UserProfile(userId = testSupplierId, role = UserRole.SUPPLIER)))
        every { fakeOrderRepository.getOrderById(testOrderId) } returns flowOf(Result.Success(order))
        coEvery { fakeReviewDao.getReviewByOrderAndReviewer(testOrderId, testSupplierId) } returns null

        val result = reviewRepository.submitReview(review)

        assertTrue(result is Result.Success)
    }

    @Test
    fun `review cannot be submitted before order completion`() = runTest {
        val order = Order(
            orderId = testOrderId,
            farmerId = testFarmerId,
            supplierId = testSupplierId,
            status = OrderStatus.IN_TRANSIT
        )
        val review = Review(
            reviewId = "${testOrderId}_${testFarmerId}",
            orderId = testOrderId,
            reviewerId = testFarmerId,
            revieweeId = testSupplierId,
            rating = 5,
            comment = "Test"
        )

        every { fakeAuthRepository.currentUserId } returns testFarmerId
        every { fakeAuthRepository.getCurrentUserProfile() } returns flowOf(Result.Success(UserProfile(userId = testFarmerId, role = UserRole.FARMER)))
        every { fakeOrderRepository.getOrderById(testOrderId) } returns flowOf(Result.Success(order))

        val result = reviewRepository.submitReview(review)

        assertTrue(result is Result.Error)
    }

    @Test
    fun `review cannot be submitted for cancelled orders`() = runTest {
        val order = Order(
            orderId = testOrderId,
            farmerId = testFarmerId,
            supplierId = testSupplierId,
            status = OrderStatus.CANCELLED
        )
        val review = Review(
            reviewId = "${testOrderId}_${testFarmerId}",
            orderId = testOrderId,
            reviewerId = testFarmerId,
            revieweeId = testSupplierId,
            rating = 5,
            comment = "Test"
        )

        every { fakeAuthRepository.currentUserId } returns testFarmerId
        every { fakeAuthRepository.getCurrentUserProfile() } returns flowOf(Result.Success(UserProfile(userId = testFarmerId, role = UserRole.FARMER)))
        every { fakeOrderRepository.getOrderById(testOrderId) } returns flowOf(Result.Success(order))

        val result = reviewRepository.submitReview(review)

        assertTrue(result is Result.Error)
    }

    @Test
    fun `review cannot be submitted twice by same reviewer for same order`() = runTest {
        val order = Order(
            orderId = testOrderId,
            farmerId = testFarmerId,
            supplierId = testSupplierId,
            status = OrderStatus.DELIVERED
        )
        val existingReview = Review(
            reviewId = "${testOrderId}_${testFarmerId}",
            orderId = testOrderId,
            reviewerId = testFarmerId,
            revieweeId = testSupplierId,
            rating = 5,
            comment = "Previous review"
        )
        val newReview = Review(
            reviewId = "${testOrderId}_${testFarmerId}",
            orderId = testOrderId,
            reviewerId = testFarmerId,
            revieweeId = testSupplierId,
            rating = 4,
            comment = "Updated review"
        )

        every { fakeAuthRepository.currentUserId } returns testFarmerId
        every { fakeAuthRepository.getCurrentUserProfile() } returns flowOf(Result.Success(UserProfile(userId = testFarmerId, role = UserRole.FARMER)))
        every { fakeOrderRepository.getOrderById(testOrderId) } returns flowOf(Result.Success(order))
        coEvery { fakeReviewDao.getReviewByOrderAndReviewer(testOrderId, testFarmerId) } returns existingReview

        val result = reviewRepository.submitReview(newReview)

        assertTrue(result is Result.Error)
    }

    @Test
    fun `review cannot be submitted by non-participant`() = runTest {
        val order = Order(
            orderId = testOrderId,
            farmerId = testFarmerId,
            supplierId = testSupplierId,
            status = OrderStatus.DELIVERED
        )
        val outsiderId = "outsider123"
        val review = Review(
            reviewId = "${testOrderId}_${outsiderId}",
            orderId = testOrderId,
            reviewerId = outsiderId,
            revieweeId = testFarmerId,
            rating = 5,
            comment = "Test"
        )

        every { fakeAuthRepository.currentUserId } returns outsiderId
        every { fakeAuthRepository.getCurrentUserProfile() } returns flowOf(Result.Success(UserProfile(userId = outsiderId, role = UserRole.FARMER)))
        every { fakeOrderRepository.getOrderById(testOrderId) } returns flowOf(Result.Success(order))

        val result = reviewRepository.submitReview(review)

        assertTrue(result is Result.Error)
    }

    @Test
    fun `rating must be between 1 and 5`() = runTest {
        val order = Order(
            orderId = testOrderId,
            farmerId = testFarmerId,
            supplierId = testSupplierId,
            status = OrderStatus.DELIVERED
        )
        val invalidReview = Review(
            reviewId = "${testOrderId}_${testFarmerId}",
            orderId = testOrderId,
            reviewerId = testFarmerId,
            revieweeId = testSupplierId,
            rating = 0,
            comment = "Test"
        )

        every { fakeAuthRepository.currentUserId } returns testFarmerId
        every { fakeAuthRepository.getCurrentUserProfile() } returns flowOf(Result.Success(UserProfile(userId = testFarmerId, role = UserRole.FARMER)))
        every { fakeOrderRepository.getOrderById(testOrderId) } returns flowOf(Result.Success(order))

        val result = reviewRepository.submitReview(invalidReview)

        assertTrue(result is Result.Error)
    }

    @Test
    fun `submitting review never changes user points`() = runTest {
        val order = Order(
            orderId = testOrderId,
            farmerId = testFarmerId,
            supplierId = testSupplierId,
            status = OrderStatus.DELIVERED
        )
        val initialReview = Review(
            reviewId = "${testOrderId}_${testFarmerId}",
            orderId = testOrderId,
            reviewerId = testFarmerId,
            revieweeId = testSupplierId,
            rating = 5,
            comment = "Great!"
        )

        every { fakeAuthRepository.currentUserId } returns testFarmerId
        every { fakeAuthRepository.getCurrentUserProfile() } returns flowOf(Result.Success(UserProfile(userId = testFarmerId, role = UserRole.FARMER)))
        every { fakeOrderRepository.getOrderById(testOrderId) } returns flowOf(Result.Success(order))
        coEvery { fakeReviewDao.getReviewByOrderAndReviewer(testOrderId, testFarmerId) } returns null

        val result = reviewRepository.submitReview(initialReview)

        assertTrue(result is Result.Success)
    }
}
