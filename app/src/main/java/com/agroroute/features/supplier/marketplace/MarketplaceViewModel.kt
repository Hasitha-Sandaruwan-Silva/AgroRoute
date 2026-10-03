package com.agroroute.features.supplier.marketplace

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.agroroute.core.model.HarvestListing
import com.agroroute.core.model.ListingStatus
import com.agroroute.core.model.availableStock
import com.agroroute.core.remote.NetworkConnectivityMonitor
import com.agroroute.core.util.Result
import com.agroroute.core.usecase.GetListingsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/** UI state for the supplier marketplace screen. */
data class MarketplaceUiState(
    val listings: List<HarvestListing> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

internal fun isSupplierMarketplaceEligible(listing: HarvestListing): Boolean =
    !listing.isDraft &&
        listing.status == ListingStatus.OPEN &&
        listing.availableStock() > 0.0

/**
 * ViewModel for [BrowseListingsScreen] — exposes open listings for the supplier marketplace.
 */
@HiltViewModel
class MarketplaceViewModel @Inject constructor(
    private val getListingsUseCase: GetListingsUseCase,
    networkConnectivityMonitor: NetworkConnectivityMonitor
) : ViewModel() {

    init {
        networkConnectivityMonitor.isOnline
            .onEach { online -> if (online) getListingsUseCase.syncListings() }
            .launchIn(viewModelScope)
    }

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
