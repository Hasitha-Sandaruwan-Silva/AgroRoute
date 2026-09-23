package com.agroroute.domain.repository

import com.agroroute.data.model.Order
import com.agroroute.data.model.OrderStatus
import com.agroroute.util.Result
import kotlinx.coroutines.flow.Flow

/**
 * Domain contract for all [Order]-related operations — implemented in the data layer.
 */
interface OrderRepository {

    /** Emits all orders for the currently signed-in farmer. */
    fun getOrdersForFarmer(farmerId: String): Flow<Result<List<Order>>>

    /** Emits all orders for the currently signed-in supplier. */
    fun getOrdersForSupplier(supplierId: String): Flow<Result<List<Order>>>

    /** Emits a single order by ID. */
    fun getOrderById(orderId: String): Flow<Result<Order?>>

    /** Creates a new order in Firestore and mirrors to Room. */
    suspend fun createOrder(order: Order): Result<Unit>

    /** Updates the [OrderStatus] for an existing order. */
    suspend fun updateOrderStatus(orderId: String, status: OrderStatus): Result<Unit>

    /** Fetches all orders for a user from Firestore and refreshes Room cache. */
    suspend fun syncOrders(userId: String): Result<Unit>
}
