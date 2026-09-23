package com.agroroute.ui.orders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.agroroute.data.model.Order
import com.agroroute.data.model.OrderStatus
import com.agroroute.domain.repository.OrderRepository
import com.agroroute.domain.usecase.GetOrdersUseCase
import com.agroroute.util.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/** UI state for order screens. */
data class OrderUiState(
    val orders: List<Order> = emptyList(),
    val selectedOrder: Order? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val actionSuccess: Boolean = false
)

/**
 * ViewModel for [OrderDetailScreen] and [OrderStatusScreen] — loads orders and handles status updates.
 */
@HiltViewModel
class OrderViewModel @Inject constructor(
    private val getOrdersUseCase: GetOrdersUseCase,
    private val orderRepository: OrderRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(OrderUiState())
    val uiState: StateFlow<OrderUiState> = _uiState.asStateFlow()

    fun loadOrderById(orderId: String) {
        getOrdersUseCase.byId(orderId)
            .onEach { result ->
                when (result) {
                    is Result.Loading -> _uiState.update { it.copy(isLoading = true) }
                    is Result.Success -> _uiState.update { it.copy(isLoading = false, selectedOrder = result.data) }
                    is Result.Error   -> _uiState.update { it.copy(isLoading = false, error = result.message) }
                }
            }
            .launchIn(viewModelScope)
    }

    fun loadOrdersForFarmer(farmerId: String) {
        getOrdersUseCase.forFarmer(farmerId)
            .onEach { result ->
                when (result) {
                    is Result.Loading -> _uiState.update { it.copy(isLoading = true) }
                    is Result.Success -> _uiState.update { it.copy(isLoading = false, orders = result.data) }
                    is Result.Error   -> _uiState.update { it.copy(isLoading = false, error = result.message) }
                }
            }
            .launchIn(viewModelScope)
    }

    fun loadOrdersForSupplier(supplierId: String) {
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
