package com.agroroute.features.farmer.listing

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.agroroute.core.model.HarvestListing
import com.agroroute.core.util.Result
import com.agroroute.core.usecase.GetListingsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/** UI state for the farmer listing screens. */
data class ListingUiState(
    val listings: List<HarvestListing> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val actionSuccess: Boolean = false
)

/**
 * ViewModel for [CreateListingScreen] and [MyListingsScreen] — manages listing CRUD
 * through use cases only, with no direct dependency on Room or Firestore.
 */
@HiltViewModel
class ListingViewModel @Inject constructor(
    private val getListingsUseCase: GetListingsUseCase,
    private val createListingUseCase: CreateListingUseCase,
    private val toggleBiddingModeUseCase: ToggleBiddingModeUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(ListingUiState())
    val uiState: StateFlow<ListingUiState> = _uiState.asStateFlow()

    /** Loads listings for [farmerId] and observes future changes. */
    fun loadFarmerListings(farmerId: String) {
        getListingsUseCase.getByFarmer(farmerId)
            .onEach { result ->
                when (result) {
                    is Result.Loading -> _uiState.update { it.copy(isLoading = true) }
                    is Result.Success -> _uiState.update { it.copy(isLoading = false, listings = result.data, error = null) }
                    is Result.Error   -> _uiState.update { it.copy(isLoading = false, error = result.message) }
                }
            }
            .launchIn(viewModelScope)
    }

    fun createListing(listing: HarvestListing) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null, actionSuccess = false) }
            when (val r = createListingUseCase(listing)) {
                is Result.Success -> _uiState.update { it.copy(isLoading = false, actionSuccess = true) }
                is Result.Error   -> _uiState.update { it.copy(isLoading = false, error = r.message) }
                is Result.Loading -> Unit
            }
        }
    }

    fun toggleBidding(listingId: String, enabled: Boolean) {
        viewModelScope.launch {
            when (val r = toggleBiddingModeUseCase(listingId, enabled)) {
                is Result.Error -> _uiState.update { it.copy(error = r.message) }
                else -> Unit
            }
        }
    }

    fun clearError() = _uiState.update { it.copy(error = null) }
    fun clearActionSuccess() = _uiState.update { it.copy(actionSuccess = false) }
}
