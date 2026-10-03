package com.agroroute.features.farmer.orderrequests

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.agroroute.core.model.HarvestListing
import com.agroroute.core.model.OrderRequest
import com.agroroute.core.model.UserRole
import com.agroroute.core.remote.NetworkConnectivityMonitor
import com.agroroute.core.repository.AuthRepository
import com.agroroute.core.repository.HarvestListingRepository
import com.agroroute.core.repository.OrderRequestRepository
import com.agroroute.core.util.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** UI state for the farmer's order-request inbox. */
data class FarmerOrderRequestsUiState(
    val requests: List<OrderRequest> = emptyList(),
    val listingsById: Map<String, HarvestListing> = emptyMap(),
    val isLoading: Boolean = false,
    val error: String? = null
)

/**
 * ViewModel for [FarmerOrderRequestsScreen] — loads order requests for the
 * signed-in Farmer's published listings and handles accept/reject actions.
 */
@HiltViewModel
class FarmerOrderRequestsViewModel @Inject constructor(
    private val orderRequestRepository: OrderRequestRepository,
    private val listingRepository: HarvestListingRepository,
    private val acceptOrderRequestUseCase: AcceptOrderRequestUseCase,
    private val rejectOrderRequestUseCase: RejectOrderRequestUseCase,
    private val authRepository: AuthRepository,
    networkConnectivityMonitor: NetworkConnectivityMonitor
) : ViewModel() {

    private val _uiState = MutableStateFlow(FarmerOrderRequestsUiState())
    val uiState: StateFlow<FarmerOrderRequestsUiState> = _uiState.asStateFlow()
    private var inboxJob: Job? = null
    private var inboxLoaded = false

    init {
        networkConnectivityMonitor.isOnline
            .drop(1)
            .filter { it }
            .onEach { if (inboxLoaded) loadRequests() }
            .launchIn(viewModelScope)
    }

    /** Syncs then collects order requests owned by the signed-in Farmer. */
    fun loadRequests() {
        inboxLoaded = true
        inboxJob?.cancel()
        inboxJob = viewModelScope.launch {
            val farmerId = verifiedFarmerId()
            if (farmerId == null) {
                _uiState.update { it.copy(isLoading = false, error = "Sign in as a Farmer to view order requests") }
                return@launch
            }

            _uiState.update { it.copy(isLoading = true, error = null) }
            val listingSyncError = (listingRepository.syncListings() as? Result.Error)?.message
            val listingsResult = listingRepository.getListingsByFarmer(farmerId).first()
            val listings = when (listingsResult) {
                is Result.Success -> listingsResult.data.filter { it.farmerId == farmerId && !it.isDraft }
                is Result.Error -> {
                    _uiState.update {
                        it.copy(isLoading = false, error = listingsResult.message ?: listingSyncError)
                    }
                    return@launch
                }
                Result.Loading -> emptyList()
            }
            val listingsById = listings.associateBy { it.listingId }

            val requestSyncError = (orderRequestRepository.syncRequestsForUser(farmerId) as? Result.Error)?.message
            orderRequestRepository.getRequestsForFarmer(farmerId).collect { result ->
                when (result) {
                    is Result.Loading -> _uiState.update { it.copy(isLoading = true) }
                    is Result.Success -> _uiState.update {
                        it.copy(
                            isLoading = false,
                            requests = result.data.sortedByDescending { r -> r.createdAt },
                            listingsById = listingsById,
                            error = requestSyncError ?: listingSyncError
                        )
                    }
                    is Result.Error -> _uiState.update { it.copy(isLoading = false, error = result.message) }
                }
            }
        }
    }

    fun acceptRequest(request: OrderRequest) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            when (val result = acceptOrderRequestUseCase(request)) {
                is Result.Success -> _uiState.update { it.copy(isLoading = false) }
                is Result.Error -> _uiState.update { it.copy(isLoading = false, error = result.message) }
                Result.Loading -> Unit
            }
        }
    }

    fun rejectRequest(request: OrderRequest) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            when (val result = rejectOrderRequestUseCase(request)) {
                is Result.Success -> _uiState.update { it.copy(isLoading = false) }
                is Result.Error -> _uiState.update { it.copy(isLoading = false, error = result.message) }
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
