package com.agroroute.features.reviews

import com.agroroute.core.database.ReviewDao
import com.agroroute.core.model.*
import com.agroroute.core.remote.FirestoreService
import com.agroroute.core.repository.*
import com.agroroute.core.util.Result
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ReviewEligibilityTest {
    private val dispatcher = StandardTestDispatcher()
    private val auth = mockk<AuthRepository>()
    private val orders = mockk<OrderRepository>()
    private val reviews = mockk<ReviewRepository>()
    @Before fun setup() { Dispatchers.setMain(dispatcher) }
    @After fun cleanup() { Dispatchers.resetMain() }
    private fun model(status: OrderStatus, user: String = "farmer", reviewed: Boolean = false): ReviewViewModel {
        every { auth.currentUserId } returns user
        coEvery { orders.syncOrders(user) } returns Result.Success(Unit)
        every { orders.getOrderById("order") } returns flowOf(Result.Success(Order(orderId = "order",
            farmerId = "farmer", supplierId = "supplier", status = status)))
        coEvery { reviews.hasUserReviewedOrder("order", user) } returns Result.Success(reviewed)
        return ReviewViewModel(reviews, auth, orders)
    }
    @Test fun deliveredParticipantsCanReviewTheirCounterpart() = runTest(dispatcher) {
        for (user in listOf("farmer", "supplier")) {
            val vm = model(OrderStatus.DELIVERED, user)
            vm.checkHasReviewed("order", if (user == "farmer") "supplier" else "farmer")
            assertTrue(vm.uiState.value.isChecking)
            advanceUntilIdle()
            assertTrue(vm.uiState.value.canReview)
        }
    }
    @Test fun undeliveredNonparticipantAndWrongCounterpartAreBlocked() = runTest(dispatcher) {
        for ((status, user, counterpart) in listOf(Triple(OrderStatus.IN_TRANSIT, "farmer", "supplier"),
            Triple(OrderStatus.CANCELLED, "supplier", "farmer"), Triple(OrderStatus.DELIVERED, "outsider", "farmer"),
            Triple(OrderStatus.DELIVERED, "farmer", "wrong"))) {
            val vm = model(status, user)
            vm.checkHasReviewed("order", counterpart); advanceUntilIdle()
            assertFalse(vm.uiState.value.canReview)
        }
        coVerify(exactly = 0) { reviews.hasUserReviewedOrder(any(), any()) }
    }
    @Test fun alreadyReviewedCannotSubmitAgain() = runTest(dispatcher) {
        val vm = model(OrderStatus.DELIVERED, reviewed = true)
        vm.checkHasReviewed("order", "supplier"); advanceUntilIdle()
        assertTrue(vm.uiState.value.hasReviewed)
        vm.submitReview("order", "supplier", 5, "Review")
        advanceUntilIdle()
        coVerify(exactly = 0) { reviews.submitReview(any()) }
    }
    @Test fun failedServerCheckDoesNotEnableReviewAndCanRetry() = runTest(dispatcher) {
        val vm = model(OrderStatus.DELIVERED)
        coEvery { orders.syncOrders("farmer") } returns Result.Error("Unavailable")
        vm.checkHasReviewed("order", "supplier"); advanceUntilIdle()
        assertFalse(vm.uiState.value.canReview); assertNotNull(vm.uiState.value.error)
        coEvery { orders.syncOrders("farmer") } returns Result.Success(Unit)
        vm.checkHasReviewed("order", "supplier"); advanceUntilIdle()
        assertTrue(vm.uiState.value.canReview)
    }
    @Test fun duplicateTapsSubmitOnceAndConfirmedSuccessMarksReviewed() = runTest(dispatcher) {
        val vm = model(OrderStatus.DELIVERED)
        coEvery { reviews.submitReview(any()) } returns Result.Success(Unit)
        vm.checkHasReviewed("order", "supplier"); advanceUntilIdle()
        vm.submitReview("order", "supplier", 5, "Review")
        vm.submitReview("order", "supplier", 5, "Review")
        advanceUntilIdle()
        coVerify(exactly = 1) { reviews.submitReview(any()) }
        assertTrue(vm.uiState.value.hasReviewed); assertFalse(vm.uiState.value.canReview)
    }
    @Test fun outgoingReviewFromAnotherDeviceIsRecognizedAndCached() = runTest {
        val dao = mockk<ReviewDao>(relaxed = true)
        val remote = mockk<FirestoreService>()
        val outgoing = Review(reviewId = "order_farmer", orderId = "order", reviewerId = "farmer", revieweeId = "supplier")
        every { auth.currentUserId } returns "farmer"
        coEvery { dao.getReviewByOrderAndReviewer("order", "farmer") } returns null
        coEvery { remote.queryOnceFromServer("reviews", "reviewerId", "farmer", Review::class.java) } returns listOf(outgoing)
        val result = ReviewRepositoryImpl(dao, remote, auth, orders).hasUserReviewedOrder("order", "farmer")
        assertEquals(Result.Success(true), result)
        coVerify { dao.upsertReviews(listOf(outgoing)) }
    }
    @Test fun signOutBetweenEligibilityAndSubmissionLeavesUsableFeedback() = runTest(dispatcher) {
        val vm = model(OrderStatus.DELIVERED)
        vm.checkHasReviewed("order", "supplier"); advanceUntilIdle()
        every { auth.currentUserId } returns null
        vm.submitReview("order", "supplier", 5, "Review"); advanceUntilIdle()
        assertFalse(vm.uiState.value.isLoading)
        assertFalse(vm.uiState.value.canReview)
        assertNotNull(vm.uiState.value.error)
        coVerify(exactly = 0) { reviews.submitReview(any()) }
    }
}
