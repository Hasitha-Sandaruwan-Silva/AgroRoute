package com.agroroute.core.notification

import android.util.Log
import com.agroroute.core.model.Bid
import com.agroroute.core.model.BidStatus
import com.agroroute.core.model.HarvestListing
import com.agroroute.core.model.Order
import com.agroroute.core.model.OrderRequest
import com.agroroute.core.model.OrderRequestStatus
import com.agroroute.core.model.UserProfile
import com.agroroute.core.model.UserRole
import com.agroroute.core.remote.FirestoreService
import com.agroroute.core.util.Constants
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Watches Firestore collections relevant to the signed-in user and posts local notifications
 * for order / bid / order-request changes while the app process is alive
 * (foreground or background). Closed-app delivery still requires FCM from a server sender.
 */
@Singleton
class RealtimeEventNotifier @Inject constructor(
    private val firestoreService: FirestoreService,
    private val publisher: AppNotificationPublisher
) {
    private var sessionJob: Job? = null
    private val listingBidJobs = mutableMapOf<String, Job>()

    fun start(scope: CoroutineScope, profile: UserProfile) {
        stop()
        sessionJob = scope.launch {
            when (profile.role) {
                UserRole.FARMER -> observeFarmer(this, profile.userId)
                UserRole.SUPPLIER -> observeSupplier(this, profile.userId)
            }
        }
    }

    fun stop() {
        sessionJob?.cancel()
        sessionJob = null
        listingBidJobs.values.forEach { it.cancel() }
        listingBidJobs.clear()
    }

    private suspend fun observeFarmer(scope: CoroutineScope, farmerId: String) {
        scope.launch {
            val cropByListing = mutableMapOf<String, String>()
            var listingsPrimed = false
            firestoreService.queryFlow<HarvestListing>(
                Constants.COLLECTION_LISTINGS,
                "farmerId",
                farmerId
            ).catch { Log.w(TAG, "listings watch failed", it) }
                .collect { listings ->
                    val ids = listings.map { it.listingId }.toSet()
                    listings.forEach { cropByListing[it.listingId] = it.cropName }
                    (listingBidJobs.keys - ids).forEach { stale ->
                        listingBidJobs.remove(stale)?.cancel()
                    }
                    ids.forEach { listingId ->
                        if (listingId !in listingBidJobs) {
                            listingBidJobs[listingId] = scope.launch {
                                watchBidsForListing(
                                    listingId = listingId,
                                    role = UserRole.FARMER,
                                    cropHint = { cropByListing[listingId] }
                                )
                            }
                        }
                    }
                    listingsPrimed = true
                    Log.d(TAG, "farmer listings primed=$listingsPrimed count=${listings.size}")
                }
        }

        scope.launch {
            watchOrderRequests(field = "farmerId", userId = farmerId, role = UserRole.FARMER)
        }
        scope.launch {
            watchOrders(field = "farmerId", userId = farmerId)
        }
    }

    private suspend fun observeSupplier(scope: CoroutineScope, supplierId: String) {
        scope.launch {
            watchBidsForSupplier(supplierId)
        }
        scope.launch {
            watchOrderRequests(field = "supplierId", userId = supplierId, role = UserRole.SUPPLIER)
        }
        scope.launch {
            watchOrders(field = "supplierId", userId = supplierId)
        }
    }

    private suspend fun watchBidsForListing(
        listingId: String,
        role: UserRole,
        cropHint: () -> String?
    ) {
        var known = emptyMap<String, Bid>()
        var primed = false
        firestoreService.queryFlow<Bid>(Constants.COLLECTION_BIDS, "listingId", listingId)
            .catch { Log.w(TAG, "bids($listingId) watch failed", it) }
            .collect { bids ->
                if (!primed) {
                    known = bids.associateBy { it.bidId }
                    primed = true
                    return@collect
                }
                bids.forEach { bid ->
                    val previous = known[bid.bidId]
                    when {
                        previous == null && bid.status == BidStatus.PENDING && role == UserRole.FARMER ->
                            publisher.publish(NotificationMessageFactory.forNewBid(bid, cropHint()))
                        previous != null && previous.status != bid.status ->
                            NotificationMessageFactory.forBidStatus(bid, role)?.let(publisher::publish)
                    }
                }
                known = bids.associateBy { it.bidId }
            }
    }

    private suspend fun watchBidsForSupplier(supplierId: String) {
        var known = emptyMap<String, Bid>()
        var primed = false
        firestoreService.queryFlow<Bid>(Constants.COLLECTION_BIDS, "supplierId", supplierId)
            .catch { Log.w(TAG, "supplier bids watch failed", it) }
            .collect { bids ->
                if (!primed) {
                    known = bids.associateBy { it.bidId }
                    primed = true
                    return@collect
                }
                bids.forEach { bid ->
                    val previous = known[bid.bidId]
                    if (previous != null && previous.status != bid.status) {
                        NotificationMessageFactory.forBidStatus(bid, UserRole.SUPPLIER)
                            ?.let(publisher::publish)
                    }
                }
                known = bids.associateBy { it.bidId }
            }
    }

    private suspend fun watchOrderRequests(field: String, userId: String, role: UserRole) {
        var known = emptyMap<String, OrderRequest>()
        var primed = false
        firestoreService.queryFlow<OrderRequest>(
            Constants.COLLECTION_ORDER_REQUESTS,
            field,
            userId
        ).catch { Log.w(TAG, "order_requests watch failed", it) }
            .collect { requests ->
                if (!primed) {
                    known = requests.associateBy { it.requestId }
                    primed = true
                    return@collect
                }
                requests.forEach { request ->
                    val previous = known[request.requestId]
                    when {
                        previous == null &&
                            request.status == OrderRequestStatus.PENDING &&
                            role == UserRole.FARMER ->
                            publisher.publish(NotificationMessageFactory.forNewOrderRequest(request))
                        previous != null && previous.status != request.status ->
                            NotificationMessageFactory.forOrderRequestStatus(request, role)
                                ?.let(publisher::publish)
                    }
                }
                known = requests.associateBy { it.requestId }
            }
    }

    private suspend fun watchOrders(field: String, userId: String) {
        var known = emptyMap<String, Order>()
        var primed = false
        firestoreService.queryFlow<Order>(Constants.COLLECTION_ORDERS, field, userId)
            .catch { Log.w(TAG, "orders watch failed", it) }
            .collect { orders ->
                if (!primed) {
                    known = orders.associateBy { it.orderId }
                    primed = true
                    return@collect
                }
                orders.forEach { order ->
                    val previous = known[order.orderId]
                    when {
                        previous == null ->
                            publisher.publish(NotificationMessageFactory.forNewOrder(order))
                        previous.status != order.status ->
                            NotificationMessageFactory.forOrderStatus(order)?.let(publisher::publish)
                    }
                }
                known = orders.associateBy { it.orderId }
            }
    }

    companion object {
        private const val TAG = "RealtimeEventNotifier"
    }
}
