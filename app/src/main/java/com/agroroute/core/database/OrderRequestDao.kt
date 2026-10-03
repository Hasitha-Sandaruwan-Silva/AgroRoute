package com.agroroute.core.database

import androidx.room.*
import com.agroroute.core.model.OrderRequest
import com.agroroute.core.model.OrderRequestStatus
import kotlinx.coroutines.flow.Flow

/**
 * DAO for all CRUD operations on the [OrderRequest] table.
 */
@Dao
interface OrderRequestDao {

    @Query("SELECT * FROM order_requests WHERE farmerId = :farmerId ORDER BY createdAt DESC")
    fun getRequestsForFarmer(farmerId: String): Flow<List<OrderRequest>>

    @Query("SELECT * FROM order_requests WHERE supplierId = :supplierId ORDER BY createdAt DESC")
    fun getRequestsForSupplier(supplierId: String): Flow<List<OrderRequest>>

    @Query("SELECT * FROM order_requests WHERE listingId = :listingId ORDER BY createdAt DESC")
    fun getRequestsForListing(listingId: String): Flow<List<OrderRequest>>

    @Query("SELECT * FROM order_requests WHERE requestId = :requestId")
    fun getRequestById(requestId: String): Flow<OrderRequest?>

    @Query("SELECT * FROM order_requests WHERE farmerId = :farmerId AND status = :status ORDER BY createdAt DESC")
    fun getRequestsForFarmerByStatus(farmerId: String, status: OrderRequestStatus): Flow<List<OrderRequest>>

    @Query("SELECT * FROM order_requests WHERE supplierId = :supplierId AND status = :status ORDER BY createdAt DESC")
    fun getRequestsForSupplierByStatus(supplierId: String, status: OrderRequestStatus): Flow<List<OrderRequest>>

    @Upsert
    suspend fun upsertRequest(request: OrderRequest)

    @Upsert
    suspend fun upsertRequests(requests: List<OrderRequest>)

    @Query("DELETE FROM order_requests WHERE requestId = :requestId")
    suspend fun deleteRequest(requestId: String)

    @Query("DELETE FROM order_requests WHERE listingId = :listingId")
    suspend fun deleteRequestsForListing(listingId: String)
}
