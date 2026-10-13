package com.agroroute.features.supplier.orders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.agroroute.core.model.Order
import com.agroroute.core.model.OrderRequest
import com.agroroute.core.model.OrderStatus
import com.agroroute.core.usecase.GetOrdersUseCase
import com.agroroute.core.repository.OrderRepository
import com.agroroute.core.repository.OrderRequestRepository
import com.agroroute.core.repository.AuthRepository
import com.agroroute.core.remote.NetworkConnectivityMonitor
import com.agroroute.core.util.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import javax.inject.Inject

/** UI state for supplier order screens. */
data class SupplierOrderUiState(
    val orders: List<Order> = emptyList(),
    val requests: List<OrderRequest> = emptyList(),
    val selectedOrder: Order? = null,
    val isLoading: Boolean = false,
    val isSubmitting: Boolean = false,
    val error: String? = null,
    val pickup: com.agroroute.core.model.OrderPickupVerification? = null,
    val pickupLoading: Boolean = false,
    val pickupError: String? = null,
    val actionSuccess: Boolean = false
)

/**
 * ViewModel for [SupplierOrderDetailScreen] and [SupplierOrdersScreen] — loads orders and handles status updates.
 */
@HiltViewModel
class SupplierOrderViewModel @Inject constructor(
    private val getOrdersUseCase: GetOrdersUseCase,
    private val orderRepository: OrderRepository,
    private val orderRequestRepository: OrderRequestRepository,
    private val authRepository: AuthRepository,
    networkConnectivityMonitor: NetworkConnectivityMonitor
) : ViewModel() {

    private val _uiState = MutableStateFlow(SupplierOrderUiState())
    val uiState: StateFlow<SupplierOrderUiState> = _uiState.asStateFlow()
    private var ordersObserver: Job? = null
    private var requestsObserver: Job? = null
    private var observedSupplierId: String? = null
    val currentSupplierId: String get() = authRepository.currentUserId.orEmpty()

    init {
        networkConnectivityMonitor.isOnline
            .drop(1)
            .filter { it }
            .onEach {
                if (currentSupplierId.isNotBlank()) orderRepository.syncOrders(currentSupplierId)
            }
            .launchIn(viewModelScope)
    }

    fun loadOrderById(orderId: String) {
        loadPickupVerification(orderId)
        getOrdersUseCase.byId(orderId)
            .onEach { result ->
                when (result) {
                    is Result.Loading -> _uiState.update { it.copy(isLoading = true) }
                    is Result.Success -> {
                        val order = result.data
                        if (order?.supplierId == currentSupplierId) {
                            _uiState.update { it.copy(isLoading = false, selectedOrder = order) }
                        } else {
                            _uiState.update { it.copy(isLoading = false, selectedOrder = null, error = "Order not found") }
                        }
                    }
                    is Result.Error   -> _uiState.update { it.copy(isLoading = false, error = result.message) }
                }
            }
            .launchIn(viewModelScope)
    }

    fun loadOrdersForSupplier(supplierId: String) {
        if (supplierId.isBlank() || (observedSupplierId == supplierId &&
                ordersObserver?.isActive == true && requestsObserver?.isActive == true)) return
        ordersObserver?.cancel()
        requestsObserver?.cancel()
        observedSupplierId = supplierId
        viewModelScope.launch {
            when (val sync = orderRepository.syncOrders(supplierId)) {
                is Result.Error -> _uiState.update { it.copy(error = sync.message) }
                else -> Unit
            }
        }
        ordersObserver = getOrdersUseCase.forSupplier(supplierId)
            .onEach { result ->
                when (result) {
                    is Result.Loading -> _uiState.update { it.copy(isLoading = true) }
                    is Result.Success -> _uiState.update { it.copy(isLoading = false, orders = result.data) }
                    is Result.Error   -> _uiState.update { it.copy(isLoading = false, error = result.message) }
                }
            }
            .launchIn(viewModelScope)

        viewModelScope.launch {
            when (val sync = orderRequestRepository.syncRequestsForUser(supplierId)) {
                is Result.Error -> _uiState.update { it.copy(error = sync.message) }
                else -> Unit
            }
        }
        requestsObserver = orderRequestRepository.getRequestsForSupplier(supplierId)
            .onEach { result ->
                when (result) {
                    is Result.Loading -> _uiState.update { it.copy(isLoading = true) }
                    is Result.Success -> _uiState.update {
                        it.copy(isLoading = false, requests = result.data.sortedByDescending { r -> r.createdAt })
                    }
                    is Result.Error -> _uiState.update { it.copy(isLoading = false, error = result.message) }
                }
            }
            .launchIn(viewModelScope)
    }

    fun refreshOrdersForSupplier(supplierId: String) {
        if (supplierId.isBlank()) return
        viewModelScope.launch {
            when (val sync = orderRepository.syncOrders(supplierId)) {
                is Result.Error -> _uiState.update { it.copy(error = sync.message) }
                else -> Unit
            }
        }
        viewModelScope.launch {
            when (val sync = orderRequestRepository.syncRequestsForUser(supplierId)) {
                is Result.Error -> _uiState.update { it.copy(error = sync.message) }
                else -> Unit
            }
        }
    }

    fun cancelRequest(requestId: String) {
        if (_uiState.value.isSubmitting || _uiState.value.pickupLoading) return
        _uiState.update { it.copy(isSubmitting = true) }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            when (val result = orderRequestRepository.cancelRequest(requestId)) {
                is Result.Success -> {
                    _uiState.update { it.copy(isSubmitting = false, isLoading = false, actionSuccess = true) }
                }
                is Result.Error -> _uiState.update { it.copy(isSubmitting = false, isLoading = false, error = result.message) }
                Result.Loading -> _uiState.update { it.copy(isSubmitting = false, isLoading = false) }
            }
        }
    }

    fun updateOrderStatus(orderId: String, status: OrderStatus) {
        if (_uiState.value.isSubmitting || _uiState.value.pickupLoading) return
        _uiState.update { it.copy(isSubmitting = true) }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            when (val r = orderRepository.updateOrderStatus(orderId, status)) {
                is Result.Success -> {
                    _uiState.update { it.copy(isSubmitting = false, isLoading = false, actionSuccess = true) }
                    loadPickupVerification(orderId)
                }
                is Result.Error   -> _uiState.update { it.copy(isSubmitting = false, isLoading = false, error = r.message) }
                is Result.Loading -> _uiState.update { it.copy(isSubmitting = false, isLoading = false) }
            }
        }
    }

    fun loadPickupVerification(orderId: String, generateNewCode: Boolean = false) {
        if (_uiState.value.pickupLoading || _uiState.value.isSubmitting) return
        _uiState.update { it.copy(pickupLoading = true) }
        viewModelScope.launch {
            _uiState.update { it.copy(pickupLoading = true, pickupError = null, pickup = null) }
            orderRepository.syncOrders(currentSupplierId)
            when (val result = if (generateNewCode) orderRepository.refreshPickupCode(orderId)
                else orderRepository.getPickupVerification(orderId)) {
                is Result.Success -> _uiState.update { it.copy(pickupLoading = false, pickup = result.data) }
                is Result.Error -> _uiState.update { it.copy(pickupLoading = false, pickupError = result.message) }
                Result.Loading -> Unit
            }
        }
    }

    fun clearError() = _uiState.update { it.copy(error = null) }

    fun updateDeliveryLocation(orderId: String, location: String) {
        if (_uiState.value.isSubmitting || _uiState.value.pickupLoading) return
        _uiState.update { it.copy(isSubmitting = true) }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            when (val result = orderRepository.updateDeliveryLocation(orderId, location)) {
                is Result.Success -> _uiState.update { it.copy(isSubmitting = false, isLoading = false) }
                is Result.Error -> _uiState.update { it.copy(isSubmitting = false, isLoading = false, error = result.message) }
                Result.Loading -> _uiState.update { it.copy(isSubmitting = false, isLoading = false) }
            }
        }
    }
}
