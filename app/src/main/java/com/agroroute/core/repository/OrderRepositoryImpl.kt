package com.agroroute.core.repository

import com.agroroute.core.database.OrderDao
import com.agroroute.core.database.BidDao
import com.agroroute.core.database.HarvestListingDao
import com.agroroute.core.database.OrderRequestDao
import com.agroroute.core.model.Bid
import com.agroroute.core.model.BidStatus
import com.agroroute.core.model.HarvestListing
import com.agroroute.core.model.HarvestStatus
import com.agroroute.core.model.OrderRequest
import com.agroroute.core.model.OrderRequestStatus
import com.agroroute.core.model.availableStock
import com.agroroute.core.model.recordSale
import com.agroroute.core.model.ListingStatus
import com.agroroute.core.model.Order
import com.agroroute.core.model.OrderStatus
import com.agroroute.core.model.UserRole
import com.agroroute.core.repository.AuthRepository
import com.agroroute.core.model.releaseSale
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
 * Implements [OrderRepository] using the sync-on-fetch strategy —
 * Room is the reactive source of truth; writes go to Firestore then Room.
 */
@Singleton
class OrderRepositoryImpl @Inject constructor(
    @PublishedApi
    internal val dao: OrderDao,
    @PublishedApi internal val firestoreService: FirestoreService,
    private val bidDao: BidDao,
    private val listingDao: HarvestListingDao,
    private val orderRequestDao: OrderRequestDao,
    private val authRepository: AuthRepository
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

    override suspend fun acceptBidAndCreateOrder(order: Order): Result<Unit> = safeCall {
        val userId = authRepository.currentUserId ?: error("Sign in as the listing's Farmer to accept a bid")
        val profile = authRepository.getCurrentUserProfile().first().getOrNull()
        check(profile?.role == UserRole.FARMER && profile.userId == userId && order.farmerId == userId) {
            "Only the listing's Farmer can accept this bid"
        }
        val firestore = firestoreService.firestore
        val listingRef = firestore.collection(Constants.COLLECTION_LISTINGS).document(order.listingId)
        val bidRef = firestore.collection(Constants.COLLECTION_BIDS).document(order.bidId)
        val orderRef = firestore.collection(Constants.COLLECTION_ORDERS).document(order.orderId)

        val updatedRecords = firestoreService.runTransaction<Pair<HarvestListing, Bid>> { transaction ->
            val listing = transaction.get(listingRef).toObject(HarvestListing::class.java)
                ?: error("Listing ${order.listingId} not found")
            val bid = transaction.get(bidRef).toObject(Bid::class.java)
                ?: error("Bid ${order.bidId} not found")
            check(listing.status == ListingStatus.OPEN && listing.biddingEnabled && !listing.isDraft) {
                "This listing is not accepting bids"
            }
            check(listing.harvestStatus == HarvestStatus.READY) {
                "This harvest has not been confirmed ready for sale"
            }
            check(bid.listingId == listing.listingId && bid.status == BidStatus.PENDING) {
                "This bid is no longer available"
            }
            check(order.farmerId == listing.farmerId && order.supplierId == bid.supplierId) {
                "Bid and listing ownership does not match"
            }
            check(order.quantity > 0.0 && order.quantity <= listing.availableStock()) {
                "Insufficient listing stock"
            }

            val updatedListing = listing.recordSale(order.quantity)
            val updatedBid = bid.copy(status = BidStatus.ACCEPTED, updatedAt = System.currentTimeMillis())
            transaction.set(listingRef, updatedListing)
            transaction.set(bidRef, updatedBid)
            transaction.set(orderRef, order)
            updatedListing to updatedBid
        }

        listingDao.upsertListing(updatedRecords.first)
        bidDao.upsertBid(updatedRecords.second)
        dao.upsertOrder(order)
    }

    override suspend fun acceptOrderRequestAndCreateOrder(requestId: String): Result<Unit> = safeCall {
        val userId = authRepository.currentUserId ?: error("Sign in as the listing's Farmer to accept this request")
        val profile = authRepository.getCurrentUserProfile().first().getOrNull()
        check(profile?.role == UserRole.FARMER && profile.userId == userId) {
            "Only the listing's Farmer can accept this order request"
        }
        val firestore = firestoreService.firestore
        val requestRef = firestore.collection(Constants.COLLECTION_ORDER_REQUESTS).document(requestId)
        val listingRef = firestore.collection(Constants.COLLECTION_LISTINGS)
        val orderRef = firestore.collection(Constants.COLLECTION_ORDERS).document(requestId)

        val updatedRecords = firestoreService.runTransaction<Triple<HarvestListing, Order, OrderRequest>> { transaction ->
            val request = transaction.get(requestRef).toObject(OrderRequest::class.java)
                ?: error("Order request $requestId not found")
            check(request.farmerId == userId) { "Only the listing's Farmer can accept this order request" }
            check(request.status == OrderRequestStatus.PENDING) { "This order request is no longer pending" }

            val listingDoc = listingRef.document(request.listingId)
            val listing = transaction.get(listingDoc).toObject(HarvestListing::class.java)
                ?: error("Listing ${request.listingId} not found")
            check(listing.farmerId == userId) { "Only the listing's Farmer can accept this order request" }
            check(listing.status == ListingStatus.OPEN && !listing.isDraft && !listing.biddingEnabled) {
                "This listing is not accepting direct order requests"
            }
            check(listing.harvestStatus == HarvestStatus.READY) {
                "Mark this harvest ready and enter the actual quantity before accepting requests"
            }
            check(request.requestedQuantity > 0.0 && request.requestedQuantity <= listing.availableStock()) {
                "Insufficient listing stock for this order request"
            }

            val order = Order(
                orderId = request.requestId,
                listingId = request.listingId,
                bidId = "",
                farmerId = request.farmerId,
                supplierId = request.supplierId,
                cropName = listing.cropName,
                quantity = request.requestedQuantity,
                quantityUnit = listing.quantityUnit,
                agreedPrice = request.listedPrice,
                status = OrderStatus.AWAITING_PICKUP,
                pickupLocation = listing.location
            )
            val updatedListing = listing.recordSale(request.requestedQuantity)
            val updatedRequest = request.copy(
                status = OrderRequestStatus.ACCEPTED,
                updatedAt = System.currentTimeMillis()
            )
            transaction.set(listingDoc, updatedListing)
            transaction.set(orderRef, order)
            transaction.set(requestRef, updatedRequest)
            Triple(updatedListing, order, updatedRequest)
        }

        listingDao.upsertListing(updatedRecords.first)
        dao.upsertOrder(updatedRecords.second)
        orderRequestDao.upsertRequest(updatedRecords.third)
    }

    override suspend fun updateOrderStatus(orderId: String, status: OrderStatus): Result<Unit> = safeCall {
        val userId = authRepository.currentUserId ?: error("Sign in to update this order")
        val profile = authRepository.getCurrentUserProfile().first().getOrNull()
            ?: error("Account profile is unavailable")
        check(profile.userId == userId) { "Account profile does not match the signed-in user" }
        if (status == OrderStatus.CANCELLED) {
            val orderRef = firestoreService.firestore.collection(Constants.COLLECTION_ORDERS).document(orderId)
            val updatedRecords = firestoreService.runTransaction<Pair<Order, HarvestListing?>> { transaction ->
                val existing = transaction.get(orderRef).toObject(Order::class.java)
                    ?: error("Order $orderId not found")
                check(
                    (profile.role == UserRole.FARMER && existing.farmerId == userId) ||
                        (profile.role == UserRole.SUPPLIER && existing.supplierId == userId)
                ) { "Only an order participant can update this order" }
                if (existing.status == OrderStatus.CANCELLED) return@runTransaction existing to null

                val listingRef = firestoreService.firestore
                    .collection(Constants.COLLECTION_LISTINGS)
                    .document(existing.listingId)
                val listing = transaction.get(listingRef).toObject(HarvestListing::class.java)
                val updatedListing = listing?.releaseSale(existing.quantity)
                val updatedOrder = existing.copy(status = status, updatedAt = System.currentTimeMillis())
                transaction.set(orderRef, updatedOrder)
                if (updatedListing != null) transaction.set(listingRef, updatedListing)
                updatedOrder to updatedListing
            }
            dao.upsertOrder(updatedRecords.first)
            val updatedListing = updatedRecords.second
            if (updatedListing != null) listingDao.upsertListing(updatedListing)
        } else {
            val orderRef = firestoreService.firestore.collection(Constants.COLLECTION_ORDERS).document(orderId)
            val updatedOrder = firestoreService.runTransaction<Order> { transaction ->
                val existing = transaction.get(orderRef).toObject(Order::class.java)
                    ?: error("Order $orderId not found")
                check(
                    (profile.role == UserRole.FARMER && existing.farmerId == userId) ||
                        (profile.role == UserRole.SUPPLIER && existing.supplierId == userId)
                ) { "Only an order participant can update this order" }
                when (status) {
                    OrderStatus.IN_TRANSIT -> {
                        check(profile.role == UserRole.FARMER && existing.farmerId == userId) {
                            "Only the farmer can mark the order as in transit"
                        }
                        check(existing.status == OrderStatus.AWAITING_PICKUP) {
                            "Order must be awaiting pickup to be marked as in transit"
                        }
                    }
                    OrderStatus.DELIVERED -> {
                        check(profile.role == UserRole.SUPPLIER && existing.supplierId == userId) {
                            "Only the supplier can mark the order as delivered"
                        }
                        check(existing.status == OrderStatus.IN_TRANSIT) {
                            "Order must be in transit to be marked as delivered"
                        }
                    }
                    else -> error("Invalid status transition")
                }
                existing.copy(status = status, updatedAt = System.currentTimeMillis())
            }
            firestoreService.set(Constants.COLLECTION_ORDERS, orderId, updatedOrder)
            dao.upsertOrder(updatedOrder)
        }
    }

    override suspend fun syncOrders(userId: String): Result<Unit> = safeCall {
        check(authRepository.currentUserId == userId) { "Cannot sync another account's orders" }
        val profile = authRepository.getCurrentUserProfile().first().getOrNull()
            ?: error("Account profile is unavailable")
        check(profile.userId == userId) { "Account profile does not match the signed-in user" }
        val ownerField = when (profile.role) {
            UserRole.FARMER -> "farmerId"
            UserRole.SUPPLIER -> "supplierId"
        }
        val orders = firestoreService.queryFlow<Order>(
            Constants.COLLECTION_ORDERS,
            ownerField,
            userId
        ).first()
        dao.upsertOrders(orders.distinctBy { it.orderId })
    }
}
