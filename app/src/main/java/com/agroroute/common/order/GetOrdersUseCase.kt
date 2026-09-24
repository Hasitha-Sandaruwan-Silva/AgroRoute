package com.agroroute.common.order

import com.agroroute.common.model.Order
import com.agroroute.common.util.Result
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Retrieves orders for the current user, scoped by role (farmer or supplier).
 */
class GetOrdersUseCase @Inject constructor(
    private val repository: OrderRepository
) {
    /** Returns orders where the user is the farmer. */
    fun forFarmer(farmerId: String): Flow<Result<List<Order>>> =
        repository.getOrdersForFarmer(farmerId)

    /** Returns orders where the user is the supplier. */
    fun forSupplier(supplierId: String): Flow<Result<List<Order>>> =
        repository.getOrdersForSupplier(supplierId)

    /** Returns a single order detail. */
    fun byId(orderId: String): Flow<Result<Order?>> =
        repository.getOrderById(orderId)
}
