package com.agroroute.core.repository

import com.agroroute.core.database.OrderDao
import com.agroroute.core.database.BidDao
import com.agroroute.core.database.HarvestListingDao
import com.agroroute.core.model.Bid
import com.agroroute.core.model.BidStatus
import com.agroroute.core.model.HarvestListing
import com.agroroute.core.model.availableStock
import com.agroroute.core.model.recordSale
import com.agroroute.core.model.ListingStatus
import com.agroroute.core.model.Order
import com.agroroute.core.model.OrderStatus
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
    private val listingDao: HarvestListingDao
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

    override suspend fun updateOrderStatus(orderId: String, status: OrderStatus): Result<Unit> = safeCall {
        if (status == OrderStatus.CANCELLED) {
            val orderRef = firestoreService.firestore.collection(Constants.COLLECTION_ORDERS).document(orderId)
            val updatedRecords = firestoreService.runTransaction<Pair<Order, HarvestListing?>> { transaction ->
                val existing = transaction.get(orderRef).toObject(Order::class.java)
                    ?: error("Order $orderId not found")
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
            val existing = firestoreService.getOnce<Order>(Constants.COLLECTION_ORDERS, orderId)
                ?: error("Order $orderId not found")
            val updated = existing.copy(status = status, updatedAt = System.currentTimeMillis())
            firestoreService.set(Constants.COLLECTION_ORDERS, orderId, updated)
            dao.upsertOrder(updated)
        }
    }

    override suspend fun syncOrders(userId: String): Result<Unit> = safeCall {
        // Fetch orders where user is farmer
        val farmerOrders = firestoreService.queryFlow<Order>(Constants.COLLECTION_ORDERS, "farmerId", userId).first()
        // Fetch orders where user is supplier
        val supplierOrders = firestoreService.queryFlow<Order>(Constants.COLLECTION_ORDERS, "supplierId", userId).first()
        dao.upsertOrders((farmerOrders + supplierOrders).distinctBy { it.orderId })
    }
}
