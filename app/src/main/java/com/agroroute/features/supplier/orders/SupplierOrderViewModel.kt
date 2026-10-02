package com.agroroute.features.supplier.orders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.agroroute.core.model.Order
import com.agroroute.core.model.OrderStatus
import com.agroroute.core.usecase.GetOrdersUseCase
import com.agroroute.core.repository.OrderRepository
import com.agroroute.core.repository.AuthRepository
import com.agroroute.core.remote.NetworkConnectivityMonitor
import com.agroroute.core.util.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/** UI state for supplier order screens. */
data class SupplierOrderUiState(
    val orders: List<Order> = emptyList(),
    val selectedOrder: Order? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val actionSuccess: Boolean = false
)

/**
 * ViewModel for [SupplierOrderDetailScreen] and [SupplierOrdersScreen] — loads orders and handles status updates.
 */
@HiltViewModel
class SupplierOrderViewModel @Inject constructor(
    private val getOrdersUseCase: GetOrdersUseCase,
    private val orderRepository: OrderRepository,
    private val authRepository: AuthRepository,
    networkConnectivityMonitor: NetworkConnectivityMonitor
) : ViewModel() {

    private val _uiState = MutableStateFlow(SupplierOrderUiState())
    val uiState: StateFlow<SupplierOrderUiState> = _uiState.asStateFlow()
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
        viewModelScope.launch {
            when (val sync = orderRepository.syncOrders(supplierId)) {
                is Result.Error -> _uiState.update { it.copy(error = sync.message) }
                else -> Unit
            }
        }
        getOrdersUseCase.forSupplier(supplierId)
            .onEach { result ->
                when (result) {
                    is Result.Loading -> _uiState.update { it.copy(isLoading = true) }
                    is Result.Success -> _uiState.update { it.copy(isLoading = false, orders = result.data) }
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
