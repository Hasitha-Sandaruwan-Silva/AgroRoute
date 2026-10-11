package com.agroroute.features.farmer.orders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.agroroute.core.model.Order
import com.agroroute.core.model.OrderStatus
import com.agroroute.core.repository.BidRepository
import com.agroroute.core.usecase.GetOrdersUseCase
import com.agroroute.core.repository.OrderRepository
import com.agroroute.core.repository.AuthRepository
import com.agroroute.core.remote.NetworkConnectivityMonitor
import com.agroroute.core.util.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/** UI state for farmer order screens. */
data class FarmerOrderUiState(
    val orders: List<Order> = emptyList(),
    val supplierNames: Map<String, String> = emptyMap(),
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
 * ViewModel for [FarmerOrderDetailScreen] and [FarmerOrdersScreen] — loads orders for farmer and handles status updates.
 */
@HiltViewModel
class FarmerOrderViewModel @Inject constructor(
    private val getOrdersUseCase: GetOrdersUseCase,
    private val orderRepository: OrderRepository,
    private val bidRepository: BidRepository,
    private val authRepository: AuthRepository,
    networkConnectivityMonitor: NetworkConnectivityMonitor,
    private val orderRequestRepository: com.agroroute.core.repository.OrderRequestRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(FarmerOrderUiState())
    val uiState: StateFlow<FarmerOrderUiState> = _uiState.asStateFlow()
    val currentFarmerId: String get() = authRepository.currentUserId.orEmpty()

    init {
        networkConnectivityMonitor.isOnline
            .drop(1)
            .filter { it }
            .onEach {
                if (currentFarmerId.isNotBlank()) orderRepository.syncOrders(currentFarmerId)
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
                        if (order?.farmerId == currentFarmerId) {
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

    fun loadOrdersForFarmer(farmerId: String) {
        viewModelScope.launch {
            when (val sync = orderRepository.syncOrders(farmerId)) {
                is Result.Error -> _uiState.update { it.copy(error = sync.message) }
                else -> Unit
            }
        }
        getOrdersUseCase.forFarmer(farmerId)
            .onEach { result ->
                when (result) {
                    is Result.Loading -> _uiState.update { it.copy(isLoading = true) }
                    is Result.Success -> {
                        _uiState.update { it.copy(isLoading = false, orders = result.data) }
                        viewModelScope.launch {
                            val supplierNames = result.data.mapNotNull { order ->
                                val name = if (order.bidId.isNotBlank()) bidRepository.getBidById(order.bidId).first().getOrNull()?.supplierName
                                    else orderRequestRepository.getRequestById(order.orderId).first().getOrNull()?.supplierName
                                name?.takeIf(String::isNotBlank)?.let { order.orderId to it }
                            }.toMap()
                            _uiState.update { it.copy(supplierNames = supplierNames) }
                        }
                    }
                    is Result.Error   -> _uiState.update { it.copy(isLoading = false, error = result.message) }
                }
            }
            .launchIn(viewModelScope)
    }

    fun refreshOrdersForFarmer(farmerId: String) {
        if (farmerId.isBlank()) return
        viewModelScope.launch {
            when (val sync = orderRepository.syncOrders(farmerId)) {
                is Result.Error -> _uiState.update { it.copy(error = sync.message) }
                else -> Unit
            }
        }
    }

    fun updateOrderStatus(orderId: String, status: OrderStatus) {
        if (_uiState.value.isSubmitting || _uiState.value.pickupLoading) return
        _uiState.update { it.copy(isSubmitting = true) }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            when (val r = orderRepository.updateOrderStatus(orderId, status)) {
                is Result.Success -> _uiState.update { it.copy(isSubmitting = false, isLoading = false, actionSuccess = true) }
                is Result.Error   -> _uiState.update { it.copy(isSubmitting = false, isLoading = false, error = r.message) }
                is Result.Loading -> _uiState.update { it.copy(isSubmitting = false, isLoading = false) }
            }
        }
    }

    fun loadPickupVerification(orderId: String) {
        if (_uiState.value.pickupLoading || _uiState.value.isSubmitting) return
        _uiState.update { it.copy(pickupLoading = true) }
        viewModelScope.launch {
            _uiState.update { it.copy(pickupLoading = true, pickupError = null, pickup = null) }
            orderRepository.syncOrders(currentFarmerId)
            when (val result = orderRepository.getPickupVerification(orderId)) {
                is Result.Success -> _uiState.update { it.copy(pickupLoading = false, pickup = result.data) }
                is Result.Error -> _uiState.update { it.copy(pickupLoading = false, pickupError = result.message) }
                Result.Loading -> Unit
            }
        }
    }
    fun verifyPickupCode(orderId: String, code: String) {
        if (_uiState.value.pickupLoading || _uiState.value.isSubmitting) return
        if (!Regex("[0-9]{6}").matches(code)) {
            _uiState.update { it.copy(pickupError = "Enter the six-digit pickup code.") }
            return
        }
        _uiState.update { it.copy(pickupLoading = true, pickupError = null) }
        viewModelScope.launch {
            when (val result = orderRepository.verifyPickupCode(orderId, code)) {
                is Result.Success -> _uiState.update { it.copy(pickupLoading = false, pickup = result.data) }
                is Result.Error -> _uiState.update { it.copy(pickupLoading = false, pickupError = result.message) }
                Result.Loading -> Unit
            }
        }
    }

    fun clearError() = _uiState.update { it.copy(error = null) }
}
