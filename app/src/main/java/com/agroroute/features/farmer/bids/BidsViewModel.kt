package com.agroroute.features.farmer.bids

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.agroroute.core.model.Bid
import com.agroroute.core.model.HarvestListing
import com.agroroute.core.model.UserRole
import com.agroroute.core.repository.AuthRepository
import com.agroroute.core.util.Result
import com.agroroute.core.repository.HarvestListingRepository
import com.agroroute.core.repository.BidRepository
import com.agroroute.core.remote.NetworkConnectivityMonitor
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import javax.inject.Inject

/** UI state for the farmer's bid management screen. */
data class BidsUiState(
    val bids: List<Bid> = emptyList(),
    val listing: HarvestListing? = null,
    val listingsById: Map<String, HarvestListing> = emptyMap(),
    val isLoading: Boolean = false,
    val isSubmitting: Boolean = false,
    val error: String? = null,
    val actionSuccess: Boolean = false
)

internal fun publishedListingsOwnedBy(
    listings: List<HarvestListing>,
    farmerId: String
): List<HarvestListing> = listings.filter { it.farmerId == farmerId && !it.isDraft }

/**
 * ViewModel for [ViewBidsScreen] — loads bids for a given listing and handles bid acceptance.
 */
@HiltViewModel
class BidsViewModel @Inject constructor(
    private val bidRepository: BidRepository,
    private val listingRepository: HarvestListingRepository,
    private val acceptBidUseCase: AcceptBidUseCase,
    private val rejectBidUseCase: RejectBidUseCase,
    private val authRepository: AuthRepository,
    networkConnectivityMonitor: NetworkConnectivityMonitor
) : ViewModel() {

    private val _uiState = MutableStateFlow(BidsUiState())
    val uiState: StateFlow<BidsUiState> = _uiState.asStateFlow()
    val currentFarmerId: String get() = authRepository.currentUserId.orEmpty()
    private var farmerBidsJob: Job? = null
    private var farmerInboxLoaded = false

    init {
        networkConnectivityMonitor.isOnline
            .drop(1)
            .filter { it }
            .onEach {
                if (farmerInboxLoaded) {
                    loadFarmerBids()
                } else {
                    val listingId = _uiState.value.listing?.listingId
                    if (listingId != null && verifiedFarmerId() != null) {
                        bidRepository.syncBidsForListing(listingId)
                    }
                }
            }
            .launchIn(viewModelScope)
    }

    /** Loads a listing's bids only after verifying it belongs to the signed-in Farmer. */
    fun loadBids(listingId: String) {
        viewModelScope.launch {
            val farmerId = verifiedFarmerId()
            if (farmerId == null) {
                _uiState.update { it.copy(isLoading = false, error = "Sign in as a Farmer to view bids") }
                return@launch
            }

            _uiState.update { it.copy(isLoading = true, error = null) }
            val listingResult = listingRepository.getListingById(listingId).first()
            val listing = when (listingResult) {
                is Result.Success -> listingResult.data
                is Result.Error -> {
                    _uiState.update { it.copy(isLoading = false, error = listingResult.message) }
                    return@launch
                }
                Result.Loading -> null
            }
            if (listing == null || listing.farmerId != farmerId || listing.isDraft) {
                _uiState.update {
                    it.copy(isLoading = false, listing = null, bids = emptyList(), error = "Listing not found")
                }
                return@launch
            }

            _uiState.update {
                it.copy(listing = listing, listingsById = mapOf(listingId to listing))
            }
            val syncResult = bidRepository.syncBidsForListing(listingId)
            bidRepository.getBidsForListing(listingId).collect { result ->
                when (result) {
                    is Result.Loading -> _uiState.update { it.copy(isLoading = true) }
                    is Result.Success -> _uiState.update {
                        it.copy(
                            isLoading = false,
                            bids = result.data,
                            error = (syncResult as? Result.Error)?.message
                        )
                    }
                    is Result.Error -> _uiState.update { it.copy(isLoading = false, error = result.message) }
                }
            }
        }
    }

    /** Loads and synchronizes bids for every published listing owned by the signed-in Farmer. */
    fun loadFarmerBids() {
        farmerInboxLoaded = true
        farmerBidsJob?.cancel()
        farmerBidsJob = viewModelScope.launch {
            val farmerId = verifiedFarmerId()
            if (farmerId == null) {
                _uiState.update { it.copy(isLoading = false, error = "Sign in as a Farmer to view bids") }
                return@launch
            }

            _uiState.update { it.copy(isLoading = true, error = null) }
            val listingSyncError = (listingRepository.syncListings() as? Result.Error)?.message
            val listingsResult = listingRepository.getListingsByFarmer(farmerId).first()
            val listings = when (listingsResult) {
                is Result.Success -> publishedListingsOwnedBy(listingsResult.data, farmerId)
                is Result.Error -> {
                    _uiState.update {
                        it.copy(isLoading = false, error = listingsResult.message ?: listingSyncError)
                    }
                    return@launch
                }
                Result.Loading -> emptyList()
            }
            val listingsById = listings.associateBy { it.listingId }
            if (listings.isEmpty()) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        bids = emptyList(),
                        listing = null,
                        listingsById = emptyMap(),
                        error = listingSyncError
                    )
                }
                return@launch
            }

            val syncErrors = listings.mapNotNull { listing ->
                (bidRepository.syncBidsForListing(listing.listingId) as? Result.Error)?.message
            }.toMutableList().apply { listingSyncError?.let(::add) }
            combine(listings.map { bidRepository.getBidsForListing(it.listingId) }) { results ->
                val bids = mutableListOf<Bid>()
                var streamError: String? = null
                results.forEach { result ->
                    when (result) {
                        is Result.Success -> bids += result.data
                        is Result.Error -> streamError = result.message
                        Result.Loading -> Unit
                    }
                }
                BidsUiState(
                    bids = bids.sortedByDescending { it.createdAt },
                    listingsById = listingsById,
                    isLoading = false,
                    error = streamError ?: syncErrors.firstOrNull()
                )
            }.collect { state ->
                _uiState.value = state
            }
        }
    }

    fun acceptBid(bid: Bid) {
        val listing = _uiState.value.listingsById[bid.listingId] ?: _uiState.value.listing ?: return
        acceptBid(bid, listing)
    }

    fun acceptBid(bid: Bid, listing: HarvestListing) {
        if (_uiState.value.isSubmitting) return
        _uiState.update { it.copy(isSubmitting = true) }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            when (val r = acceptBidUseCase(bid, listing)) {
                is Result.Success -> _uiState.update { it.copy(isSubmitting = false, isLoading = false, actionSuccess = true) }
                is Result.Error   -> _uiState.update { it.copy(isSubmitting = false, isLoading = false, error = r.message) }
                is Result.Loading -> Unit
            }
        }
    }

    fun rejectBid(bid: Bid) {
        if (_uiState.value.isSubmitting) return
        _uiState.update { it.copy(isSubmitting = true) }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            when (val result = rejectBidUseCase(bid)) {
                is Result.Success -> _uiState.update { it.copy(isSubmitting = false, isLoading = false) }
                is Result.Error -> _uiState.update { it.copy(isSubmitting = false, isLoading = false, error = result.message) }
                Result.Loading -> Unit
            }
        }
    }

    fun clearError() = _uiState.update { it.copy(error = null) }

    private suspend fun verifiedFarmerId(): String? {
        val farmerId = authRepository.currentUserId ?: return null
        val profile = authRepository.getCurrentUserProfile().first().getOrNull() ?: return null
        return farmerId.takeIf { profile.role == UserRole.FARMER && profile.userId == farmerId }
    }
}
