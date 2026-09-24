package com.agroroute.core.database

import androidx.room.*
import com.agroroute.core.model.Order
import com.agroroute.core.model.OrderStatus
import kotlinx.coroutines.flow.Flow

/**
 * DAO for all CRUD operations on the [Order] table.
 */
@Dao
interface OrderDao {

    @Query("SELECT * FROM orders ORDER BY createdAt DESC")
    fun getAllOrders(): Flow<List<Order>>

    @Query("SELECT * FROM orders WHERE farmerId = :farmerId ORDER BY createdAt DESC")
    fun getOrdersForFarmer(farmerId: String): Flow<List<Order>>

    @Query("SELECT * FROM orders WHERE supplierId = :supplierId ORDER BY createdAt DESC")
    fun getOrdersForSupplier(supplierId: String): Flow<List<Order>>

    @Query("SELECT * FROM orders WHERE orderId = :orderId")
    fun getOrderById(orderId: String): Flow<Order?>

    @Query("SELECT * FROM orders WHERE status = :status ORDER BY createdAt DESC")
    fun getOrdersByStatus(status: OrderStatus): Flow<List<Order>>

    @Upsert
    suspend fun upsertOrder(order: Order)

    @Upsert
    suspend fun upsertOrders(orders: List<Order>)

    @Query("DELETE FROM orders WHERE orderId = :orderId")
    suspend fun deleteOrder(orderId: String)
}
