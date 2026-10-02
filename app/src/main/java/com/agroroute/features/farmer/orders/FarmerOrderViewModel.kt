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
    val error: String? = null,
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
    networkConnectivityMonitor: NetworkConnectivityMonitor
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
                                val bid = bidRepository.getBidById(order.bidId).first().getOrNull()
                                bid?.supplierName?.takeIf(String::isNotBlank)?.let { order.orderId to it }
                            }.toMap()
                            _uiState.update { it.copy(supplierNames = supplierNames) }
                        }
                    }
                    is Result.Error   -> _uiState.update { it.copy(isLoading = false, error = result.message) }
                }
            }
            .launchIn(viewModelScope)
    }

    fun updateOrderStatus(orderId: String, status: OrderStatus) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            when (val r = orderRepository.updateOrderStatus(orderId, status)) {
                is Result.Success -> _uiState.update { it.copy(isLoading = false, actionSuccess = true) }
                is Result.Error   -> _uiState.update { it.copy(isLoading = false, error = r.message) }
                is Result.Loading -> Unit
            }
        }
    }

    fun clearError() = _uiState.update { it.copy(error = null) }
}
