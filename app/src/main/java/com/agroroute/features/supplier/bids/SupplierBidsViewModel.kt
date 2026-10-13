package com.agroroute.features.supplier.bids

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.agroroute.core.model.Bid
import com.agroroute.core.repository.AuthRepository
import com.agroroute.core.util.Result
import com.agroroute.core.usecase.GetBidsBySupplierUseCase
import com.agroroute.core.usecase.WithdrawBidUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import javax.inject.Inject

data class SupplierBidsUiState(
    val isLoading: Boolean = false,
    val bids: List<Bid> = emptyList(),
    val isWithdrawing: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class SupplierBidsViewModel @Inject constructor(
    private val getBidsBySupplierUseCase: GetBidsBySupplierUseCase,
    private val withdrawBidUseCase: WithdrawBidUseCase,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SupplierBidsUiState())
    val uiState: StateFlow<SupplierBidsUiState> = _uiState.asStateFlow()
    private var bidsObserver: Job? = null
    private var observedSupplierId: String? = null

    val currentSupplierId: String
        get() = authRepository.currentUserId ?: ""

    fun loadBids(supplierId: String) {
        if (supplierId.isBlank() || (observedSupplierId == supplierId && bidsObserver?.isActive == true)) return
        bidsObserver?.cancel()
        observedSupplierId = supplierId
        bidsObserver = getBidsBySupplierUseCase(supplierId)
            .onEach { result ->
                when (result) {
                    is Result.Success -> _uiState.update {
                        it.copy(isLoading = false, bids = result.data)
                    }
                    is Result.Error -> _uiState.update {
                        it.copy(isLoading = false, error = result.message)
                    }
                    Result.Loading -> _uiState.update { it.copy(isLoading = true) }
                }
            }
            .launchIn(viewModelScope)
    }

    fun withdrawBid(bidId: String) {
        if (_uiState.value.isWithdrawing) return
        _uiState.update { it.copy(isWithdrawing = true, error = null) }
        viewModelScope.launch {
            _uiState.update { it.copy(isWithdrawing = true, error = null) }
            when (val result = withdrawBidUseCase(bidId)) {
                is Result.Success -> _uiState.update {
                    it.copy(isWithdrawing = false)
                }
                is Result.Error -> _uiState.update {
                    it.copy(isWithdrawing = false, error = result.message)
                }
                Result.Loading -> Unit
            }
        }
    }

    fun clearError() = _uiState.update { it.copy(error = null) }
}
