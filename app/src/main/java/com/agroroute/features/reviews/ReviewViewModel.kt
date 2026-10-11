package com.agroroute.features.reviews

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.agroroute.core.model.Review
import com.agroroute.core.repository.AuthRepository
import com.agroroute.core.repository.ReviewRepository
import com.agroroute.core.repository.OrderRepository
import com.agroroute.core.model.OrderStatus
import kotlinx.coroutines.flow.first
import com.agroroute.core.util.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** UI state for review submission. */
data class ReviewUiState(
    val isChecking: Boolean = true,
    val canReview: Boolean = false,
    val hasReviewed: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null,
    val submissionSuccess: Boolean = false
)

/**
 * ViewModel for [SubmitReviewScreen] — handles review submission.
 */
@HiltViewModel
class ReviewViewModel @Inject constructor(
    private val reviewRepository: ReviewRepository,
    private val authRepository: AuthRepository,
    private val orderRepository: OrderRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReviewUiState())
    val uiState: StateFlow<ReviewUiState> = _uiState.asStateFlow()

    fun checkHasReviewed(orderId: String, revieweeId: String? = null) {
        _uiState.update { it.copy(isChecking = true, canReview = false, hasReviewed = false, error = null) }
        viewModelScope.launch {
            val userId = authRepository.currentUserId ?: run {
                _uiState.update { it.copy(isChecking = false, error = "Sign in to check review eligibility") }
                return@launch
            }
            val sync = orderRepository.syncOrders(userId)
            if (sync is Result.Error) {
                _uiState.update { it.copy(isChecking = false, error = "Could not check this order. Please retry.") }
                return@launch
            }
            val order = orderRepository.getOrderById(orderId).first().getOrNull()
            val otherId = when (userId) {
                order?.farmerId -> order.supplierId
                order?.supplierId -> order.farmerId
                else -> null
            }
            if (order?.status != OrderStatus.DELIVERED || otherId == null ||
                (revieweeId != null && revieweeId != otherId)) {
                _uiState.update { it.copy(isChecking = false) }
                return@launch
            }
            when (val result = reviewRepository.hasUserReviewedOrder(orderId, userId)) {
                is Result.Success -> _uiState.update { it.copy(isChecking = false, hasReviewed = result.data, canReview = !result.data) }
                is Result.Error -> _uiState.update { it.copy(isChecking = false, error = "Could not check existing reviews. Please retry.") }
                is Result.Loading -> Unit
            }
        }
    }

    fun submitReview(orderId: String, revieweeId: String, rating: Int, comment: String) {
        if (_uiState.value.isLoading || _uiState.value.isChecking || !_uiState.value.canReview) return
        _uiState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            val userId = authRepository.currentUserId ?: run {
                _uiState.update { it.copy(isLoading = false, canReview = false, error = "You must be signed in to submit a review") }
                return@launch
            }

            _uiState.update { it.copy(isLoading = true, error = null) }

            val review = Review(
                reviewId = "${orderId}_${userId}",
                orderId = orderId,
                reviewerId = userId,
                revieweeId = revieweeId,
                rating = rating,
                comment = comment,
                createdAt = System.currentTimeMillis()
            )

            when (val result = reviewRepository.submitReview(review)) {
                is Result.Success -> _uiState.update { it.copy(isLoading = false, hasReviewed = true, canReview = false, submissionSuccess = true) }
                is Result.Error -> _uiState.update { it.copy(isLoading = false, error = result.message) }
                is Result.Loading -> Unit
            }
        }
    }

    fun clearError() = _uiState.update { it.copy(error = null) }
}
