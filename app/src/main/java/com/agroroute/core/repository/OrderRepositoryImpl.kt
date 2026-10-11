package com.agroroute.core.repository

import com.agroroute.core.database.OrderDao
import com.agroroute.core.database.BidDao
import com.agroroute.core.database.HarvestListingDao
import com.agroroute.core.database.OrderRequestDao
import com.agroroute.core.database.OrderCompletionRecordDao
import com.agroroute.core.database.UserProfileDao
import com.agroroute.core.model.Bid
import com.agroroute.core.model.BidStatus
import com.agroroute.core.model.HarvestListing
import com.agroroute.core.model.HarvestStatus
import com.agroroute.core.model.OrderRequest
import com.agroroute.core.model.OrderRequestStatus
import com.agroroute.core.model.OrderCompletionRecord
import com.agroroute.core.model.UserProfile
import com.agroroute.core.model.availableStock
import com.agroroute.core.model.recordSale
import com.agroroute.core.model.ListingStatus
import com.agroroute.core.model.Order
import com.agroroute.core.model.withLocationSnapshots
import com.agroroute.core.model.requireDeliveryLocation
import com.agroroute.core.model.OrderStatus
import com.agroroute.core.model.UserRole
import com.agroroute.core.notification.NotificationEvent
import com.agroroute.core.notification.NotificationEvent.Payload
import com.agroroute.core.notification.NotificationService
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
    private val authRepository: AuthRepository,
    private val orderCompletionRecordDao: OrderCompletionRecordDao,
    private val userProfileDao: UserProfileDao,
    private val notificationService: NotificationService,
    private val completionService: com.agroroute.core.remote.OrderCompletionService
) : OrderRepository {
    override suspend fun getPickupVerification(orderId: String) = safeCall {
        completionService.pickup(orderId, "view")
    }

    override suspend fun verifyPickupCode(orderId: String, code: String) = safeCall {
        val verification = completionService.pickup(orderId, "verify", code)
        refreshTrustedOrder(orderId)
        verification
    }

    private suspend fun refreshTrustedOrder(orderId: String): Order {
        val order = firestoreService.getOnce<Order>(Constants.COLLECTION_ORDERS, orderId)
            ?: error("Order not found")
        dao.upsertOrder(order)
        for (participant in listOf(order.farmerId, order.supplierId)) {
            firestoreService.getOnce<UserProfile>(Constants.COLLECTION_USERS, participant)
                ?.let { userProfileDao.upsertUserProfile(it) }
        }
        return order
    }

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
        order.withLocationSnapshots(order.pickupLocation, order.deliveryLocation)
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

        val updatedRecords = firestoreService.runTransaction<Triple<HarvestListing, Bid, Order>> { transaction ->
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
            val snapshotOrder = order.withLocationSnapshots(listing.location, bid.deliveryLocation)
            transaction.set(listingRef, updatedListing)
            transaction.set(bidRef, updatedBid)
            transaction.set(orderRef, snapshotOrder)
            Triple(updatedListing, updatedBid, snapshotOrder)
        }

        listingDao.upsertListing(updatedRecords.first)
        bidDao.upsertBid(updatedRecords.second)
        dao.upsertOrder(updatedRecords.third)

        // Accepting a bid turns a supplier's bid into an order. The supplier is the
        // recipient the farmer is not; the farmer (actorUserId) is excluded.
        notificationService.sendPushToServer(
            recipientUserId = order.supplierId,
            event = NotificationEvent.BID_ACCEPTED,
            payload = Payload(
                cropName = order.cropName,
                supplierName = order.supplierId,
                orderId = order.orderId
            ),
            actorUserId = order.farmerId
        )
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
            ).withLocationSnapshots(listing.location, request.deliveryLocation)
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

        // The farmer accepted a request placed by a supplier. The supplier is the
        // recipient; the farmer (actorUserId) is excluded.
        notificationService.sendPushToServer(
            recipientUserId = updatedRecords.third.supplierId,
            event = NotificationEvent.ORDER_REQUEST_ACCEPTED,
            payload = Payload(
                cropName = updatedRecords.second.cropName,
                supplierName = updatedRecords.third.supplierName,
                orderId = updatedRecords.second.orderId,
                requestId = updatedRecords.third.requestId,
                listingId = updatedRecords.second.listingId
            ),
            actorUserId = updatedRecords.third.farmerId
        )
    }

    override suspend fun updateOrderStatus(orderId: String, status: OrderStatus): Result<Unit> = safeCall {
        val userId = authRepository.currentUserId ?: error("Sign in to update this order")
        val profile = authRepository.getCurrentUserProfile().first().getOrNull()
            ?: error("Account profile is unavailable")
        check(profile.userId == userId) { "Account profile does not match the signed-in user" }
        if (status == OrderStatus.IN_TRANSIT) {
            check(profile.role == UserRole.SUPPLIER) { "Only the Supplier can confirm pickup" }
            completionService.pickup(orderId, "collect")
            val collected = refreshTrustedOrder(orderId)
            notificationService.sendPushToServer(
                recipientUserId = collected.farmerId,
                event = NotificationEvent.ORDER_IN_TRANSIT,
                payload = Payload(cropName = collected.cropName, supplierName = collected.supplierId, orderId = orderId),
                actorUserId = userId
            )
            return@safeCall
        }
        if (status == OrderStatus.DELIVERED) {
            check(profile.role == UserRole.SUPPLIER) { "Only the supplier can mark the order as delivered" }
            completionService.complete(orderId)
            val completed = firestoreService.getOnce<Order>(Constants.COLLECTION_ORDERS, orderId)
                ?: error("Completed order not found")
            dao.upsertOrder(completed)
            // Refresh trusted points without allowing the client to write them.
            for (participantId in listOf(completed.farmerId, completed.supplierId)) {
                firestoreService.getOnce<UserProfile>(Constants.COLLECTION_USERS, participantId)
                    ?.let { userProfileDao.upsertUserProfile(it) }
            }
            notificationService.sendPushToServer(
                recipientUserId = completed.farmerId,
                event = NotificationEvent.ORDER_COMPLETED,
                payload = Payload(cropName = completed.cropName, supplierName = completed.supplierId, orderId = orderId),
                actorUserId = userId
            )
            return@safeCall
        }
        if (status == OrderStatus.CANCELLED) {
            completionService.cancel(orderId)
            val cancelledOrder = refreshTrustedOrder(orderId)
            firestoreService.getOnce<HarvestListing>(Constants.COLLECTION_LISTINGS, cancelledOrder.listingId)
                ?.let { listingDao.upsertListing(it) }

            // The counterparty of the order (not the actor who changed it) receives the
            // notification so the sender is never alerted about their own status change.
            val counterparty = if (profile.role == UserRole.FARMER) cancelledOrder.supplierId else cancelledOrder.farmerId
            notificationService.sendPushToServer(
                recipientUserId = counterparty,
                event = NotificationEvent.ORDER_CANCELLED,
                payload = Payload(
                    cropName = cancelledOrder.cropName,
                    supplierName = if (profile.role == UserRole.FARMER) cancelledOrder.supplierId else cancelledOrder.farmerId,
                    orderId = cancelledOrder.orderId
                ),
                actorUserId = userId
            )
        } else {
            error("Invalid status transition")
        }
    }

    override suspend fun updateDeliveryLocation(orderId: String, location: String): Result<Unit> = safeCall {
        val userId = authRepository.currentUserId ?: error("Sign in to choose a delivery location")
        val profile = authRepository.getCurrentUserProfile().first().getOrNull()
        check(profile?.role == UserRole.SUPPLIER && profile.userId == userId) { "Only the supplier can edit delivery location" }
        require(location.isNotBlank() && location.length <= 2000) { "Choose a valid delivery location" }
        requireDeliveryLocation(location)
        val ref = firestoreService.firestore.collection(Constants.COLLECTION_ORDERS).document(orderId)
        val updated = firestoreService.runTransaction<Order> { transaction ->
            val order = transaction.get(ref).toObject(Order::class.java) ?: error("Order not found")
            check(order.supplierId == userId) { "You can only update your own order" }
            check(order.status in listOf(OrderStatus.AWAITING_PICKUP, OrderStatus.IN_TRANSIT)) { "This order can no longer be edited" }
            val result = order.copy(deliveryLocation = location.trim(), updatedAt = System.currentTimeMillis())
            transaction.update(ref, mapOf("deliveryLocation" to result.deliveryLocation, "updatedAt" to result.updatedAt))
            result
        }
        dao.upsertOrder(updated)
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
        val orders = firestoreService.queryOnceFromServer(
            Constants.COLLECTION_ORDERS,
            ownerField,
            userId,
            Order::class.java
        )
        dao.upsertOrders(orders.distinctBy { it.orderId })
    }
}
