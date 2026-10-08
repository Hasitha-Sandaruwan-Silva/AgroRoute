package com.agroroute.features.leaderboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.agroroute.core.model.UserProfile
import com.agroroute.core.repository.LeaderboardRepository
import com.agroroute.core.repository.ReviewRepository
import com.agroroute.core.util.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import javax.inject.Inject

/** Representation of a user on the leaderboard along with their review metrics. */
data class LeaderboardUserItem(
    val user: UserProfile,
    val averageRating: Float = 0f,
    val reviewCount: Int = 0
)

/** UI state for leaderboard screen. */
data class LeaderboardUiState(
    val items: List<LeaderboardUserItem> = emptyList(),
    val users: List<UserProfile> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

/**
 * ViewModel for [LeaderboardScreen] — loads and displays farmer/supplier leaderboards.
 */
@HiltViewModel
class LeaderboardViewModel @Inject constructor(
    private val leaderboardRepository: LeaderboardRepository,
    private val reviewRepository: ReviewRepository
) : ViewModel() {

    private var leaderboardLoadJob: Job? = null

    private val _uiState = MutableStateFlow(LeaderboardUiState())
    val uiState: StateFlow<LeaderboardUiState> = _uiState.asStateFlow()

    fun loadLeaderboard(role: String) {
        leaderboardLoadJob?.cancel()
        leaderboardLoadJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            // Sync latest data from Firestore
            when (val syncResult = leaderboardRepository.syncLeaderboard()) {
                is Result.Error -> _uiState.update { it.copy(error = syncResult.message) }
                else -> Unit
            }

            // Load from local Room database
            val leaderboardFlow = if (role == "FARMER") {
                leaderboardRepository.getFarmerLeaderboard()
            } else {
                leaderboardRepository.getSupplierLeaderboard()
            }

            leaderboardFlow.collect { result ->
                when (result) {
                    is Result.Success -> {
                        val usersList = result.data
                        val itemsList = usersList.map { user ->
                            val reviewsResult = reviewRepository.getReviewsForUser(user.userId).first()
                            val reviews = (reviewsResult as? Result.Success)?.data.orEmpty()
                            val avg = if (reviews.isNotEmpty()) reviews.map { it.rating }.average().toFloat() else 0f
                            LeaderboardUserItem(
                                user = user,
                                averageRating = avg,
                                reviewCount = reviews.size
                            )
                        }
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                users = usersList,
                                items = itemsList
                            )
                        }
                    }
                    is Result.Error -> _uiState.update { it.copy(isLoading = false, error = result.message) }
                    is Result.Loading -> Unit
                }
            }
        }
    }

    fun clearError() = _uiState.update { it.copy(error = null) }
}
