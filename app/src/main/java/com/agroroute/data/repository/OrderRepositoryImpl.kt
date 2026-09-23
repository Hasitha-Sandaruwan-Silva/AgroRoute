package com.agroroute.data.repository

import com.agroroute.data.local.dao.OrderDao
import com.agroroute.data.model.Order
import com.agroroute.data.model.OrderStatus
import com.agroroute.data.remote.FirestoreService
import com.agroroute.domain.repository.OrderRepository
import com.agroroute.util.Constants
import com.agroroute.util.Result
import com.agroroute.util.safeCall
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implements [OrderRepository] using the sync-on-fetch strategy —
 * Room is the reactive source of truth; writes go to Firestore then Room.
 */
@Singleton
class OrderRepositoryImpl @Inject constructor(
    @PublishedApi
    internal val dao: OrderDao,
    @PublishedApi
    internal val firestoreService: FirestoreService
) : OrderRepository {

    override fun getOrdersForFarmer(farmerId: String): Flow<Result<List<Order>>> =
        dao.getOrdersForFarmer(farmerId)
            .map<List<Order>, Result<List<Order>>> { Result.Success(it) }
            .catch { e -> emit(Result.Error(e.message ?: "Failed to load orders", e)) }

    override fun getOrdersForSupplier(supplierId: String): Flow<Result<List<Order>>> =
        dao.getOrdersForSupplier(supplierId)
            .map<List<Order>, Result<List<Order>>> { Result.Success(it) }
            .catch { e -> emit(Result.Error(e.message ?: "Failed to load orders", e)) }

    override fun getOrderById(orderId: String): Flow<Result<Order?>> =
        dao.getOrderById(orderId)
            .map<Order?, Result<Order?>> { Result.Success(it) }
            .catch { e -> emit(Result.Error(e.message ?: "Failed to load order", e)) }

    override suspend fun createOrder(order: Order): Result<Unit> = safeCall {
        firestoreService.set(Constants.COLLECTION_ORDERS, order.orderId, order)
        dao.upsertOrder(order)
    }

    override suspend fun updateOrderStatus(orderId: String, status: OrderStatus): Result<Unit> = safeCall {
        val existing = firestoreService.getOnce<Order>(Constants.COLLECTION_ORDERS, orderId)
            ?: error("Order $orderId not found")
        val updated = existing.copy(status = status, updatedAt = System.currentTimeMillis())
        firestoreService.set(Constants.COLLECTION_ORDERS, orderId, updated)
        dao.upsertOrder(updated)
    }

    override suspend fun syncOrders(userId: String): Result<Unit> = safeCall {
        // Fetch orders where user is farmer
        val farmerOrders = firestoreService.queryFlow<Order>(Constants.COLLECTION_ORDERS, "farmerId", userId).first()
        // Fetch orders where user is supplier
        val supplierOrders = firestoreService.queryFlow<Order>(Constants.COLLECTION_ORDERS, "supplierId", userId).first()
        dao.upsertOrders((farmerOrders + supplierOrders).distinctBy { it.orderId })
    }
}
