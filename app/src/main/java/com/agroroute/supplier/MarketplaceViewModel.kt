package com.agroroute.supplier

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.agroroute.common.model.HarvestListing
import com.agroroute.common.util.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import javax.inject.Inject

/** UI state for the supplier marketplace screen. */
data class MarketplaceUiState(
    val listings: List<HarvestListing> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

/**
 * ViewModel for [BrowseListingsScreen] — exposes open listings for the supplier marketplace.
 */
@HiltViewModel
class MarketplaceViewModel @Inject constructor(
    private val getListingsUseCase: GetListingsUseCase
) : ViewModel() {

    val uiState: StateFlow<MarketplaceUiState> =
        getListingsUseCase.getOpenListings()
            .map { result ->
                when (result) {
                    is Result.Loading -> MarketplaceUiState(isLoading = true)
                    is Result.Success -> MarketplaceUiState(listings = result.data)
                    is Result.Error   -> MarketplaceUiState(error = result.message)
                }
            }
            .stateIn(
                scope          = viewModelScope,
                started        = SharingStarted.WhileSubscribed(5_000),
                initialValue   = MarketplaceUiState(isLoading = true)
            )
}
