package com.agroroute.features.farmer.listing

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.agroroute.core.model.HarvestListing
import com.agroroute.core.repository.AuthRepository
import com.agroroute.core.remote.NetworkConnectivityMonitor
import com.agroroute.core.util.Result
import com.agroroute.core.usecase.GetListingsUseCase
import kotlinx.coroutines.flow.first
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/** UI state for the farmer listing screens. */
data class ListingUiState(
    val listings: List<HarvestListing> = emptyList(),
    val drafts: List<HarvestListing> = emptyList(),
    val farmerName: String = "",
    val pendingBiddingListingIds: Set<String> = emptySet(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val actionSuccess: Boolean = false,
    val draftSaved: Boolean = false
)

/**
 * ViewModel for [CreateListingScreen] and [MyListingsScreen] — manages listing CRUD
 * through use cases only, with no direct dependency on Room or Firestore.
 */
@HiltViewModel
class ListingViewModel @Inject constructor(
    private val getListingsUseCase: GetListingsUseCase,
    private val getListingDraftsUseCase: GetListingDraftsUseCase,
    private val saveListingDraftUseCase: SaveListingDraftUseCase,
    private val createListingUseCase: CreateListingUseCase,
    private val toggleBiddingModeUseCase: ToggleBiddingModeUseCase,
    private val scheduleHarvestReminderUseCase: ScheduleHarvestReminderUseCase,
    private val closeListingUseCase: CloseListingUseCase,
    private val authRepository: AuthRepository,
    networkConnectivityMonitor: NetworkConnectivityMonitor
) : ViewModel() {

    private val _uiState = MutableStateFlow(ListingUiState())
    val uiState: StateFlow<ListingUiState> = _uiState.asStateFlow()
    val currentFarmerId: String get() = authRepository.currentUserId.orEmpty()

    init {
        networkConnectivityMonitor.isOnline
            .drop(1)
            .filter { it }
            .onEach {
                if (currentFarmerId.isNotBlank()) getListingsUseCase.syncListings()
            }
            .launchIn(viewModelScope)
    }

    fun loadCurrentFarmerName() {
        viewModelScope.launch {
            val name = authRepository.getCurrentUserProfile().first().getOrNull()?.displayName.orEmpty()
            _uiState.update { it.copy(farmerName = name) }
        }
    }

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
        viewModelScope.launch {
            when (val result = getListingsUseCase.syncListings()) {
                is Result.Error -> _uiState.update { it.copy(error = result.message) }
                else -> Unit
            }
        }
    }

    fun loadFarmerDrafts(farmerId: String) {
        getListingDraftsUseCase(farmerId)
            .onEach { result ->
                when (result) {
                    is Result.Success -> _uiState.update { it.copy(drafts = result.data) }
                    is Result.Error -> _uiState.update { it.copy(error = result.message) }
                    Result.Loading -> Unit
                }
            }
            .launchIn(viewModelScope)
    }

    fun saveDraft(listing: HarvestListing) {
        viewModelScope.launch {
            when (val result = saveListingDraftUseCase(listing)) {
                is Result.Success -> _uiState.update { it.copy(draftSaved = true, error = null) }
                is Result.Error -> _uiState.update { it.copy(error = result.message) }
                Result.Loading -> Unit
            }
        }
    }

    fun clearDraftSaved() = _uiState.update { it.copy(draftSaved = false) }

    fun createListing(listing: HarvestListing) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null, actionSuccess = false) }
            when (val r = createListingUseCase(listing)) {
                is Result.Success -> {
                    scheduleHarvestReminderUseCase(listing)
                    _uiState.update { it.copy(isLoading = false, actionSuccess = true) }
                }
                is Result.Error   -> _uiState.update { it.copy(isLoading = false, error = r.message) }
                is Result.Loading -> Unit
            }
        }
    }

    fun scheduleHarvestReminder(listing: HarvestListing) = scheduleHarvestReminderUseCase(listing)

    fun toggleBidding(listingId: String, enabled: Boolean) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    pendingBiddingListingIds = it.pendingBiddingListingIds + listingId,
                    error = null
                )
            }
            when (val r = toggleBiddingModeUseCase(listingId, enabled)) {
                is Result.Error -> _uiState.update {
                    it.copy(
                        pendingBiddingListingIds = it.pendingBiddingListingIds - listingId,
                        error = r.message
                    )
                }
                else -> _uiState.update {
                    it.copy(pendingBiddingListingIds = it.pendingBiddingListingIds - listingId)
                }
            }
        }
    }

    fun closeListing(listing: HarvestListing) {
        viewModelScope.launch {
            when (val result = closeListingUseCase(listing)) {
                is Result.Error -> _uiState.update { it.copy(error = result.message) }
                else -> Unit
            }
        }
    }

    fun clearError() = _uiState.update { it.copy(error = null) }
    fun clearActionSuccess() = _uiState.update { it.copy(actionSuccess = false) }
}
