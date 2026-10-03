package com.agroroute.features.farmer.listing

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.agroroute.core.model.HarvestListing
import com.agroroute.core.model.UserRole
import com.agroroute.core.repository.AuthRepository
import com.agroroute.core.repository.HarvestListingRepository
import com.agroroute.core.repository.ListingPhotoStorageException
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
    val editingListing: HarvestListing? = null,
    val farmerName: String = "",
    val pendingBiddingListingIds: Set<String> = emptySet(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val photoUploadError: String? = null,
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
    private val updateListingUseCase: UpdateListingUseCase,
    private val toggleBiddingModeUseCase: ToggleBiddingModeUseCase,
    private val scheduleHarvestReminderUseCase: ScheduleHarvestReminderUseCase,
    private val closeListingUseCase: CloseListingUseCase,
    private val authRepository: AuthRepository,
    private val listingRepository: HarvestListingRepository,
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
            _uiState.update {
                it.copy(isLoading = true, error = null, photoUploadError = null, actionSuccess = false)
            }
            when (val r = createListingUseCase(listing)) {
                is Result.Success -> {
                    scheduleHarvestReminderUseCase(listing)
                    _uiState.update { it.copy(isLoading = false, photoUploadError = null, actionSuccess = true) }
                }
                is Result.Error   -> _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = r.message,
                        photoUploadError = (r.cause as? ListingPhotoStorageException)?.message
                    )
                }
                is Result.Loading -> Unit
            }
        }
    }

    fun loadListingForEdit(listingId: String) {
        if (_uiState.value.editingListing?.listingId == listingId) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val farmerId = currentFarmerId
            val profile = authRepository.getCurrentUserProfile().first().getOrNull()
            if (farmerId.isBlank() || profile?.role != UserRole.FARMER || profile.userId != farmerId) {
                _uiState.update { it.copy(isLoading = false, error = "Only the signed-in Farmer can edit listings") }
                return@launch
            }
            when (val sync = getListingsUseCase.syncListings()) {
                is Result.Error -> {
                    _uiState.update { it.copy(isLoading = false, error = sync.message) }
                    return@launch
                }
                Result.Loading -> {
                    _uiState.update { it.copy(isLoading = false, error = "Could not load listing. Try again.") }
                    return@launch
                }
                is Result.Success -> Unit
            }
            when (val result = getListingsUseCase.getByFarmer(farmerId).first()) {
                is Result.Success -> {
                    val listing = result.data.firstOrNull {
                        it.listingId == listingId && it.farmerId == farmerId && !it.isDraft
                    }
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            editingListing = listing,
                            error = if (listing == null) "Listing not found or you do not own it" else null
                        )
                    }
                }
                is Result.Error -> _uiState.update { it.copy(isLoading = false, error = result.message) }
                Result.Loading -> _uiState.update {
                    it.copy(isLoading = false, error = "Could not load listing. Try again.")
                }
            }
        }
    }

    fun updateListing(listing: HarvestListing) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null, photoUploadError = null, actionSuccess = false) }
            when (val result = updateListingUseCase(listing)) {
                is Result.Success -> {
                    scheduleHarvestReminderUseCase(listing)
                    _uiState.update {
                        it.copy(isLoading = false, photoUploadError = null, actionSuccess = true)
                    }
                }
                is Result.Error -> _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = result.message,
                        photoUploadError = (result.cause as? ListingPhotoStorageException)?.message
                    )
                }
                Result.Loading -> Unit
            }
        }
    }

    fun clearPhotoUploadError() = _uiState.update { it.copy(photoUploadError = null) }

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

    /** Marks an expected harvest as ready with the actual available quantity. */
    fun markHarvestReady(listingId: String, actualQuantity: Double) {
        viewModelScope.launch {
            when (val result = listingRepository.markHarvestReady(listingId, actualQuantity)) {
                is Result.Error -> _uiState.update { it.copy(error = result.message) }
                else -> Unit
            }
        }
    }

    /** Marks an expected harvest as failed/unavailable; the repository cancels pending
     *  pre-harvest requests without touching stock or creating orders. */
    fun markHarvestFailed(listingId: String, reason: String?) {
        viewModelScope.launch {
            when (val result = listingRepository.markHarvestFailed(listingId, reason)) {
                is Result.Error -> _uiState.update { it.copy(error = result.message) }
                else -> Unit
            }
        }
    }

    fun clearError() = _uiState.update { it.copy(error = null) }
    fun clearActionSuccess() = _uiState.update { it.copy(actionSuccess = false) }
}
