package com.agroroute.core.repository

import com.agroroute.core.model.Order
import com.agroroute.core.model.OrderStatus
import com.agroroute.core.util.Result
import kotlinx.coroutines.flow.Flow

/**
 * Domain contract for all [Order]-related operations — implemented in the data layer.
 */
interface OrderRepository {
    suspend fun getPickupVerification(orderId: String): Result<com.agroroute.core.model.OrderPickupVerification>
    suspend fun verifyPickupCode(orderId: String, code: String): Result<com.agroroute.core.model.OrderPickupVerification>

    /** Emits all orders for the currently signed-in farmer. */
    fun getOrdersForFarmer(farmerId: String): Flow<Result<List<Order>>>

    /** Emits all orders for the currently signed-in supplier. */
    fun getOrdersForSupplier(supplierId: String): Flow<Result<List<Order>>>

    /** Emits a single order by ID. */
    fun getOrderById(orderId: String): Flow<Result<Order?>>

    /** Creates a new order in Firestore and mirrors to Room. */
    suspend fun createOrder(order: Order): Result<Unit>

    /** Atomically accepts a bid, consumes listing stock, and creates its order. */
    suspend fun acceptBidAndCreateOrder(order: Order): Result<Unit>

    /** Atomically accepts an order request, consumes listing stock, and creates its order. */
    suspend fun acceptOrderRequestAndCreateOrder(requestId: String): Result<Unit>

    /** Updates the [OrderStatus] for an existing order. */
    suspend fun updateOrderStatus(orderId: String, status: OrderStatus): Result<Unit>

    /** Supplier selects the delivery address while the order is awaiting pickup/in transit. */
    suspend fun updateDeliveryLocation(orderId: String, location: String): Result<Unit>

    /** Fetches all orders for a user from Firestore and refreshes Room cache. */
    suspend fun syncOrders(userId: String): Result<Unit>
}
