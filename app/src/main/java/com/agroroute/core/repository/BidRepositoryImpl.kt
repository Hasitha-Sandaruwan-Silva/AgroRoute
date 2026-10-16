package com.agroroute.core.repository

import com.agroroute.core.database.BidDao
import com.agroroute.core.model.Bid
import com.agroroute.core.model.BidStatus
import com.agroroute.core.model.isExpired
import com.agroroute.core.model.HarvestListing
import com.agroroute.core.model.UserRole
import com.agroroute.core.notification.NotificationEvent
import com.agroroute.core.notification.NotificationEvent.Payload
import com.agroroute.core.notification.NotificationService
import com.agroroute.core.model.availableStock
import com.agroroute.core.model.acceptsNewBids
import com.agroroute.core.model.requirePickupLocation
import com.agroroute.core.model.requireDeliveryLocation
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
 * Implements [BidRepository] using the sync-on-fetch strategy —
 * Room is the reactive source of truth; Firestore is the remote authority.
 */
@Singleton
class BidRepositoryImpl @Inject constructor(
    @PublishedApi
    internal val dao: BidDao,
    @PublishedApi
    internal val firestoreService: FirestoreService,
    private val authRepository: AuthRepository,
    private val notificationService: NotificationService
) : BidRepository {

    override fun getBidsForListing(listingId: String): Flow<Result<List<Bid>>> =
        dao.getBidsForListing(listingId)
            .map<List<Bid>, Result<List<Bid>>> { Result.Success(it) }
            .catch { e -> emit(Result.Error("Couldn't load bids. Check your connection and try again.", e)) }

    override fun getBidsBySupplier(supplierId: String): Flow<Result<List<Bid>>> =
        dao.getBidsBySupplier(supplierId)
            .map<List<Bid>, Result<List<Bid>>> { Result.Success(it) }
            .catch { e -> emit(Result.Error("Couldn't load bids. Check your connection and try again.", e)) }

    override fun getBidById(bidId: String): Flow<Result<Bid?>> =
        dao.getBidById(bidId)
            .map<Bid?, Result<Bid?>> { Result.Success(it) }
            .catch { e -> emit(Result.Error("Couldn't load bid. Check your connection and try again.", e)) }

    override suspend fun placeBid(bid: Bid): Result<Unit> = safeCall {
        val supplierId = authRepository.currentUserId ?: error("Sign in as a Supplier to place bids")
        val profile = authRepository.getCurrentUserProfile().first().getOrNull()
        check(profile?.role == UserRole.SUPPLIER && profile.userId == supplierId && bid.supplierId == supplierId) {
            "Only the signed-in Supplier can place bids"
        }
        val pendingBid = bid.copy(status = BidStatus.PENDING)
        requireDeliveryLocation(pendingBid.deliveryLocation)

        val firestore = firestoreService.firestore
        val listingRef = firestore.collection(Constants.COLLECTION_LISTINGS).document(pendingBid.listingId)
        val bidRef = firestore.collection(Constants.COLLECTION_BIDS).document(pendingBid.bidId)
        val listing = firestoreService.runTransaction<HarvestListing> { transaction ->
            val listing = transaction.get(listingRef).toObject(HarvestListing::class.java)
                ?: error("Listing not found")
            check(!listing.isExpired()) { "Listing expired" }
            check(listing.acceptsNewBids()) { "This listing is not accepting bids" }
            requirePickupLocation(listing.location)
            pendingBid.requestedQuantity?.let {
                require(it.isFinite() && it > 0 && it <= listing.availableStock()) { "Requested quantity exceeds available stock" }
            }
            transaction.set(bidRef, pendingBid)
            listing
        }
        dao.upsertBid(pendingBid)

        // A supplier placed a new bid against a farmer's listing. Notify the farmer,
        // not the supplier (actorUserId), who is excluded.
        notificationService.sendPushToServer(
            recipientUserId = listing.farmerId,
            event = NotificationEvent.BID_CREATED,
            payload = Payload(
                cropName = listing.cropName,
                supplierName = pendingBid.supplierName,
                bidId = pendingBid.bidId,
                listingId = listing.listingId,
                reason = "A new bid is awaiting the listing owner's review."
            ),
            actorUserId = pendingBid.supplierId
        )
    }

    override suspend fun updateBidStatus(bidId: String, status: BidStatus): Result<Unit> = safeCall {
        val userId = authRepository.currentUserId ?: error("Sign in to respond to bids")
        val profile = authRepository.getCurrentUserProfile().first().getOrNull()
            ?: error("Account profile is unavailable")
        check(profile.userId == userId) { "Account profile does not match the signed-in user" }
        val firestore = firestoreService.firestore
        val bidRef = firestore.collection(Constants.COLLECTION_BIDS).document(bidId)
        val (updated, listing) = firestoreService.runTransaction<Pair<Bid, HarvestListing>> { transaction ->
            val existing = transaction.get(bidRef).toObject(Bid::class.java)
                ?: error("Bid not found")
            check(existing.status == BidStatus.PENDING) { "Only pending bids can change status" }
            val listingRef = firestore.collection(Constants.COLLECTION_LISTINGS)
                .document(existing.listingId)
            val listing = transaction.get(listingRef).toObject(HarvestListing::class.java)
                ?: error("Listing not found")
            when (status) {
                BidStatus.REJECTED -> {
                    check(profile.role == UserRole.FARMER) { "Only Farmers can reject incoming bids" }
                    check(listing.farmerId == userId) { "Only the listing's Farmer can reject this bid" }
                }
                BidStatus.WITHDRAWN -> {
                    check(profile.role == UserRole.SUPPLIER && existing.supplierId == userId) {
                        "Only the Supplier who placed this bid can withdraw it"
                    }
                }
                else -> error("This bid status transition is not supported")
            }
            val updated = existing.copy(status = status, updatedAt = System.currentTimeMillis()).also {
                transaction.update(bidRef, mapOf("status" to it.status.name, "updatedAt" to it.updatedAt))
            }
            updated to listing
        }
        dao.upsertBid(updated)

        val recipientUserId = if (status == BidStatus.REJECTED) updated.supplierId else listing.farmerId
        val event = if (status == BidStatus.REJECTED) NotificationEvent.BID_REJECTED else NotificationEvent.BID_WITHDRAWN
        notificationService.sendPushToServer(
            recipientUserId = recipientUserId,
            event = event,
            payload = Payload(
                cropName = listing.cropName,
                supplierName = updated.supplierName,
                bidId = updated.bidId,
                listingId = listing.listingId,
                status = status.name
            ),
            actorUserId = userId
        )
    }

    override suspend fun withdrawBid(bidId: String): Result<Unit> =
        updateBidStatus(bidId, BidStatus.WITHDRAWN)

    override suspend fun syncBidsForListing(listingId: String): Result<Unit> = safeCall {
        val remote = firestoreService.queryFlow<Bid>(Constants.COLLECTION_BIDS, "listingId", listingId)
        remote.first().let { bids -> dao.upsertBids(bids) }
    }
}
