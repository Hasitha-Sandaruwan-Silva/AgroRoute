package com.agroroute.core.repository

import com.agroroute.core.database.BidDao
import com.agroroute.core.model.Bid
import com.agroroute.core.model.BidStatus
import com.agroroute.core.model.HarvestListing
import com.agroroute.core.model.UserRole
import com.agroroute.core.model.acceptsNewBids
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
    private val authRepository: AuthRepository
) : BidRepository {

    override fun getBidsForListing(listingId: String): Flow<Result<List<Bid>>> =
        dao.getBidsForListing(listingId)
            .map<List<Bid>, Result<List<Bid>>> { Result.Success(it) }
            .catch { e -> emit(Result.Error(e.message ?: "Failed to load bids", e)) }

    override fun getBidsBySupplier(supplierId: String): Flow<Result<List<Bid>>> =
        dao.getBidsBySupplier(supplierId)
            .map<List<Bid>, Result<List<Bid>>> { Result.Success(it) }
            .catch { e -> emit(Result.Error(e.message ?: "Failed to load bids", e)) }

    override fun getBidById(bidId: String): Flow<Result<Bid?>> =
        dao.getBidById(bidId)
            .map<Bid?, Result<Bid?>> { Result.Success(it) }
            .catch { e -> emit(Result.Error(e.message ?: "Failed to load bid", e)) }

    override suspend fun placeBid(bid: Bid): Result<Unit> = safeCall {
        val supplierId = authRepository.currentUserId ?: error("Sign in as a Supplier to place bids")
        val profile = authRepository.getCurrentUserProfile().first().getOrNull()
        check(profile?.role == UserRole.SUPPLIER && profile.userId == supplierId && bid.supplierId == supplierId) {
            "Only the signed-in Supplier can place bids"
        }
        val pendingBid = bid.copy(status = BidStatus.PENDING)

        val firestore = firestoreService.firestore
        val listingRef = firestore.collection(Constants.COLLECTION_LISTINGS).document(pendingBid.listingId)
        val bidRef = firestore.collection(Constants.COLLECTION_BIDS).document(pendingBid.bidId)
        firestoreService.runTransaction<Unit> { transaction ->
            val listing = transaction.get(listingRef).toObject(HarvestListing::class.java)
                ?: error("Listing ${pendingBid.listingId} not found")
            check(listing.acceptsNewBids()) { "This listing is not accepting bids" }
            transaction.set(bidRef, pendingBid)
        }
        dao.upsertBid(pendingBid)
    }

    override suspend fun updateBidStatus(bidId: String, status: BidStatus): Result<Unit> = safeCall {
        val userId = authRepository.currentUserId ?: error("Sign in to respond to bids")
        val profile = authRepository.getCurrentUserProfile().first().getOrNull()
            ?: error("Account profile is unavailable")
        check(profile.userId == userId) { "Account profile does not match the signed-in user" }
        val firestore = firestoreService.firestore
        val bidRef = firestore.collection(Constants.COLLECTION_BIDS).document(bidId)
        val updated = firestoreService.runTransaction { transaction ->
            val existing = transaction.get(bidRef).toObject(Bid::class.java)
                ?: error("Bid $bidId not found")
            check(existing.status == BidStatus.PENDING) { "Only pending bids can change status" }
            when (status) {
                BidStatus.REJECTED -> {
                    check(profile.role == UserRole.FARMER) { "Only Farmers can reject incoming bids" }
                    val listingRef = firestore.collection(Constants.COLLECTION_LISTINGS)
                        .document(existing.listingId)
                    val listing = transaction.get(listingRef).toObject(HarvestListing::class.java)
                        ?: error("Listing not found")
                    check(listing.farmerId == userId) { "Only the listing's Farmer can reject this bid" }
                }
                BidStatus.WITHDRAWN -> {
                    check(profile.role == UserRole.SUPPLIER && existing.supplierId == userId) {
                        "Only the Supplier who placed this bid can withdraw it"
                    }
                }
                else -> error("This bid status transition is not supported")
            }
            existing.copy(status = status, updatedAt = System.currentTimeMillis()).also {
                transaction.set(bidRef, it)
            }
        }
        dao.upsertBid(updated)
    }

    override suspend fun withdrawBid(bidId: String): Result<Unit> =
        updateBidStatus(bidId, BidStatus.WITHDRAWN)

    override suspend fun syncBidsForListing(listingId: String): Result<Unit> = safeCall {
        val remote = firestoreService.queryFlow<Bid>(Constants.COLLECTION_BIDS, "listingId", listingId)
        remote.first().let { bids -> dao.upsertBids(bids) }
    }
}
