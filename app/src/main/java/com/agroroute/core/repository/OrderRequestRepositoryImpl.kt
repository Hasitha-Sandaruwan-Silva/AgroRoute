package com.agroroute.core.repository

import com.agroroute.core.database.OrderRequestDao
import com.agroroute.core.model.HarvestListing
import com.agroroute.core.model.HarvestStatus
import com.agroroute.core.model.ListingStatus
import com.agroroute.core.model.OrderRequest
import com.agroroute.core.model.OrderRequestStatus
import com.agroroute.core.model.UserRole
import com.agroroute.core.model.availableStock
import com.agroroute.core.model.estimatedAvailable
import com.agroroute.core.repository.AuthRepository
import com.agroroute.core.remote.FirestoreService
import com.agroroute.core.util.Constants
import com.agroroute.core.util.Result
import com.agroroute.core.util.safeCall
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implements [OrderRequestRepository] using the sync-on-fetch strategy —
 * Room is the reactive source of truth; Firestore is the remote authority.
 */
@Singleton
class OrderRequestRepositoryImpl @Inject constructor(
    @PublishedApi
    internal val dao: OrderRequestDao,
    @PublishedApi
    internal val firestoreService: FirestoreService,
    private val authRepository: AuthRepository
) : OrderRequestRepository {

    override fun getRequestsForFarmer(farmerId: String): Flow<Result<List<OrderRequest>>> =
        dao.getRequestsForFarmer(farmerId)
            .map<List<OrderRequest>, Result<List<OrderRequest>>> { Result.Success(it) }
            .catch { e -> emit(Result.Error(e.message ?: "Failed to load order requests", e)) }

    override fun getRequestsForSupplier(supplierId: String): Flow<Result<List<OrderRequest>>> =
        dao.getRequestsForSupplier(supplierId)
            .map<List<OrderRequest>, Result<List<OrderRequest>>> { Result.Success(it) }
            .catch { e -> emit(Result.Error(e.message ?: "Failed to load order requests", e)) }

    override fun getRequestsForListing(listingId: String): Flow<Result<List<OrderRequest>>> =
        dao.getRequestsForListing(listingId)
            .map<List<OrderRequest>, Result<List<OrderRequest>>> { Result.Success(it) }
            .catch { e -> emit(Result.Error(e.message ?: "Failed to load order requests", e)) }

    override fun getRequestById(requestId: String): Flow<Result<OrderRequest?>> =
        dao.getRequestById(requestId)
            .map<OrderRequest?, Result<OrderRequest?>> { Result.Success(it) }
            .catch { e -> emit(Result.Error(e.message ?: "Failed to load order request", e)) }

    override suspend fun placeOrderRequest(request: OrderRequest): Result<Unit> = safeCall {
        val supplierId = authRepository.currentUserId ?: error("Sign in as a Supplier to place order requests")
        val profile = authRepository.getCurrentUserProfile().first().getOrNull()
        check(profile?.role == UserRole.SUPPLIER && profile.userId == supplierId && request.supplierId == supplierId) {
            "Only the signed-in Supplier can place order requests"
        }
        val pendingRequest = request.copy(status = OrderRequestStatus.PENDING)

        val firestore = firestoreService.firestore
        val listingRef = firestore.collection(Constants.COLLECTION_LISTINGS).document(pendingRequest.listingId)
        val requestRef = firestore.collection(Constants.COLLECTION_ORDER_REQUESTS).document(pendingRequest.requestId)
        firestoreService.runTransaction<Unit> { transaction ->
            val listing = transaction.get(listingRef).toObject(HarvestListing::class.java)
                ?: error("Listing ${pendingRequest.listingId} not found")
            check(listing.status == ListingStatus.OPEN && !listing.isDraft) {
                "This listing is not available"
            }
            check(!listing.biddingEnabled) {
                "This listing uses bidding; use the Place Bid flow instead"
            }
            if (pendingRequest.isPreHarvest) {
                // Pre-harvest request on an expected (future-harvest) listing: an estimate only.
                // It must not reserve stock or create an order.
                check(listing.harvestStatus == HarvestStatus.EXPECTED) {
                    "This listing is not an expected (future-harvest) listing"
                }
                check(pendingRequest.requestedQuantity > 0.0 &&
                    pendingRequest.requestedQuantity <= listing.estimatedAvailable()) {
                    "Requested quantity must be greater than zero and not exceed the estimated quantity"
                }
            } else {
                check(pendingRequest.requestedQuantity > 0.0 && pendingRequest.requestedQuantity <= listing.availableStock()) {
                    "Requested quantity must be greater than zero and not exceed available stock"
                }
            }
            transaction.set(requestRef, pendingRequest)
        }
        dao.upsertRequest(pendingRequest)
    }

    override suspend fun updateRequestStatus(requestId: String, status: OrderRequestStatus): Result<Unit> = safeCall {
        val userId = authRepository.currentUserId ?: error("Sign in to respond to order requests")
        val profile = authRepository.getCurrentUserProfile().first().getOrNull()
            ?: error("Account profile is unavailable")
        check(profile.userId == userId) { "Account profile does not match the signed-in user" }
        val firestore = firestoreService.firestore
        val requestRef = firestore.collection(Constants.COLLECTION_ORDER_REQUESTS).document(requestId)
        val updated = firestoreService.runTransaction { transaction ->
            val existing = transaction.get(requestRef).toObject(OrderRequest::class.java)
                ?: error("Order request $requestId not found")
            check(existing.status == OrderRequestStatus.PENDING) { "Only pending requests can change status" }
            when (status) {
                OrderRequestStatus.REJECTED -> {
                    check(profile.role == UserRole.FARMER) { "Only Farmers can reject incoming order requests" }
                    val listingRef = firestore.collection(Constants.COLLECTION_LISTINGS)
                        .document(existing.listingId)
                    val listing = transaction.get(listingRef).toObject(HarvestListing::class.java)
                        ?: error("Listing not found")
                    check(listing.farmerId == userId) { "Only the listing's Farmer can reject this request" }
                }
                OrderRequestStatus.CANCELLED -> {
                    check(profile.role == UserRole.SUPPLIER && existing.supplierId == userId) {
                        "Only the Supplier who placed this request can cancel it"
                    }
                }
                else -> error("This request status transition is not supported directly; use acceptRequestUseCase")
            }
            existing.copy(status = status, updatedAt = System.currentTimeMillis()).also {
                transaction.set(requestRef, it)
            }
        }
        dao.upsertRequest(updated)
    }

    override suspend fun cancelRequest(requestId: String): Result<Unit> =
        updateRequestStatus(requestId, OrderRequestStatus.CANCELLED)

    override suspend fun syncRequestsForListing(listingId: String): Result<Unit> = safeCall {
        val remote = firestoreService.queryFlow<OrderRequest>(
            Constants.COLLECTION_ORDER_REQUESTS,
            "listingId",
            listingId
        )
        remote.first().let { requests -> dao.upsertRequests(requests) }
    }

    override suspend fun syncRequestsForUser(userId: String): Result<Unit> = safeCall {
        check(authRepository.currentUserId == userId) { "Cannot sync another account's order requests" }
        val profile = authRepository.getCurrentUserProfile().first().getOrNull()
            ?: error("Account profile is unavailable")
        check(profile.userId == userId) { "Account profile does not match the signed-in user" }
        val ownerField = when (profile.role) {
            UserRole.FARMER -> "farmerId"
            UserRole.SUPPLIER -> "supplierId"
        }
        val requests = firestoreService.queryFlow<OrderRequest>(
            Constants.COLLECTION_ORDER_REQUESTS,
            ownerField,
            userId
        ).first()
        dao.upsertRequests(requests.distinctBy { it.requestId })
    }
}
