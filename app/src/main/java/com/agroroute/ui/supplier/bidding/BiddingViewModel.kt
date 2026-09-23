package com.agroroute.ui.supplier.bidding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.agroroute.data.model.Bid
import com.agroroute.domain.usecase.PlaceBidUseCase
import com.agroroute.util.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/** UI state for the bid placement screen. */
data class BiddingUiState(
    val isLoading: Boolean = false,
    val bidPlaced: Boolean = false,
    val error: String? = null
)

/**
 * ViewModel for [PlaceBidScreen] — handles bid submission through [PlaceBidUseCase].
 */
@HiltViewModel
class BiddingViewModel @Inject constructor(
    private val placeBidUseCase: PlaceBidUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(BiddingUiState())
    val uiState: StateFlow<BiddingUiState> = _uiState.asStateFlow()

    fun placeBid(bid: Bid) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            when (val r = placeBidUseCase(bid)) {
                is Result.Success -> _uiState.update { it.copy(isLoading = false, bidPlaced = true) }
                is Result.Error   -> _uiState.update { it.copy(isLoading = false, error = r.message) }
                is Result.Loading -> Unit
            }
        }
    }

    fun clearError() = _uiState.update { it.copy(error = null) }
}
