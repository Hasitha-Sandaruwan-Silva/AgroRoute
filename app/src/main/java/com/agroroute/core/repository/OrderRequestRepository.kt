package com.agroroute.core.repository

import com.agroroute.core.model.OrderRequest
import com.agroroute.core.model.OrderRequestStatus
import com.agroroute.core.util.Result
import kotlinx.coroutines.flow.Flow

/**
 * Domain contract for all [OrderRequest]-related operations — implemented in the data layer.
 */
interface OrderRequestRepository {

    /** Emits all order requests for a given farmer. */
    fun getRequestsForFarmer(farmerId: String): Flow<Result<List<OrderRequest>>>

    /** Room-backed realtime inbox for operational Farmer screens. */
    fun observeRequestsForFarmer(farmerId: String): Flow<Result<List<OrderRequest>>> = getRequestsForFarmer(farmerId)

    /** Emits all order requests for a given supplier. */
    fun getRequestsForSupplier(supplierId: String): Flow<Result<List<OrderRequest>>>

    /** Emits all order requests for a given listing. */
    fun getRequestsForListing(listingId: String): Flow<Result<List<OrderRequest>>>

    /** Emits a single order request by ID. */
    fun getRequestById(requestId: String): Flow<Result<OrderRequest?>>

    /** Submits a new order request to Firestore and caches locally. */
    suspend fun placeOrderRequest(request: OrderRequest): Result<Unit>

    /**
     * Updates the status of an existing order request.
     * When accepting, creates an [Order] via [OrderRepository] — callers handle this in the use-case layer.
     */
    suspend fun updateRequestStatus(requestId: String, status: OrderRequestStatus): Result<Unit>

    /** Cancels a pending request (supplier action). */
    suspend fun cancelRequest(requestId: String): Result<Unit>

    /** Fetches order requests for a listing from Firestore and refreshes Room cache. */
    suspend fun syncRequestsForListing(listingId: String): Result<Unit>

    /** Fetches all order requests for a user from Firestore and refreshes Room cache. */
    suspend fun syncRequestsForUser(userId: String): Result<Unit>
}
