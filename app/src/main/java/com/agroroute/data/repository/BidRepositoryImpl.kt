package com.agroroute.data.repository

import com.agroroute.data.local.dao.BidDao
import com.agroroute.data.model.Bid
import com.agroroute.data.model.BidStatus
import com.agroroute.data.remote.FirestoreService
import com.agroroute.domain.repository.BidRepository
import com.agroroute.util.Constants
import com.agroroute.util.Result
import com.agroroute.util.safeCall
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
    internal val firestoreService: FirestoreService
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
        firestoreService.set(Constants.COLLECTION_BIDS, bid.bidId, bid)
        dao.upsertBid(bid)
    }

    override suspend fun updateBidStatus(bidId: String, status: BidStatus): Result<Unit> = safeCall {
        val existing = firestoreService.getOnce<Bid>(Constants.COLLECTION_BIDS, bidId)
            ?: error("Bid $bidId not found")
        val updated = existing.copy(status = status, updatedAt = System.currentTimeMillis())
        firestoreService.set(Constants.COLLECTION_BIDS, bidId, updated)
        dao.upsertBid(updated)
    }

    override suspend fun withdrawBid(bidId: String): Result<Unit> =
        updateBidStatus(bidId, BidStatus.WITHDRAWN)

    override suspend fun syncBidsForListing(listingId: String): Result<Unit> = safeCall {
        val remote = firestoreService.queryFlow<Bid>(Constants.COLLECTION_BIDS, "listingId", listingId)
        remote.first().let { bids -> dao.upsertBids(bids) }
    }
}
