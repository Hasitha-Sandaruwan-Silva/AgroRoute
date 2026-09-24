package com.agroroute.supplier

import com.agroroute.common.model.Bid
import com.agroroute.common.model.BidStatus
import com.agroroute.common.util.Result
import kotlinx.coroutines.flow.Flow

/**
 * Domain contract for all [Bid]-related operations — implemented in the data layer.
 */
interface BidRepository {

    /** Emits all bids for a given listing in real-time. */
    fun getBidsForListing(listingId: String): Flow<Result<List<Bid>>>

    /** Emits all bids placed by a given supplier. */
    fun getBidsBySupplier(supplierId: String): Flow<Result<List<Bid>>>

    /** Emits a single bid by ID. */
    fun getBidById(bidId: String): Flow<Result<Bid?>>

    /** Submits a new bid to Firestore and caches locally. */
    suspend fun placeBid(bid: Bid): Result<Unit>

    /**
     * Updates the status of an existing bid (e.g. ACCEPTED / REJECTED).
     * When accepting, creates an [Order] via [OrderRepository] — callers handle this in the use-case layer.
     */
    suspend fun updateBidStatus(bidId: String, status: BidStatus): Result<Unit>

    /** Withdraws a pending bid (supplier action). */
    suspend fun withdrawBid(bidId: String): Result<Unit>

    /** Fetches bids for a listing from Firestore and refreshes Room cache. */
    suspend fun syncBidsForListing(listingId: String): Result<Unit>
}
