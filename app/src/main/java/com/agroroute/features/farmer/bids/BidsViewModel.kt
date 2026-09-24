package com.agroroute.features.farmer.bids

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.agroroute.core.model.Bid
import com.agroroute.core.model.HarvestListing
import com.agroroute.core.util.Result
import com.agroroute.core.repository.HarvestListingRepository
import com.agroroute.core.repository.BidRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/** UI state for the farmer's bid management screen. */
data class BidsUiState(
    val bids: List<Bid> = emptyList(),
    val listing: HarvestListing? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val actionSuccess: Boolean = false
)

/**
 * ViewModel for [ViewBidsScreen] — loads bids for a given listing and handles bid acceptance.
 */
@HiltViewModel
class BidsViewModel @Inject constructor(
    private val bidRepository: BidRepository,
    private val listingRepository: HarvestListingRepository,
    private val acceptBidUseCase: AcceptBidUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(BidsUiState())
    val uiState: StateFlow<BidsUiState> = _uiState.asStateFlow()

    /** Loads bids and the parent listing for the given [listingId]. */
    fun loadBids(listingId: String) {
        bidRepository.getBidsForListing(listingId)
            .onEach { result ->
                when (result) {
                    is Result.Loading -> _uiState.update { it.copy(isLoading = true) }
                    is Result.Success -> _uiState.update { it.copy(isLoading = false, bids = result.data) }
                    is Result.Error   -> _uiState.update { it.copy(isLoading = false, error = result.message) }
                }
            }
            .launchIn(viewModelScope)

        listingRepository.getListingById(listingId)
            .onEach { result ->
                if (result is Result.Success) _uiState.update { it.copy(listing = result.data) }
            }
            .launchIn(viewModelScope)
    }

    fun acceptBid(bid: Bid) {
        val listing = _uiState.value.listing ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            when (val r = acceptBidUseCase(bid, listing)) {
                is Result.Success -> _uiState.update { it.copy(isLoading = false, actionSuccess = true) }
                is Result.Error   -> _uiState.update { it.copy(isLoading = false, error = r.message) }
                is Result.Loading -> Unit
            }
        }
    }

    fun clearError() = _uiState.update { it.copy(error = null) }
}
