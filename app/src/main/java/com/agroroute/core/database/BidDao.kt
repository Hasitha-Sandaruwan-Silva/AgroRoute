package com.agroroute.core.database

import androidx.room.*
import com.agroroute.core.model.Bid
import com.agroroute.core.model.BidStatus
import kotlinx.coroutines.flow.Flow

/**
 * DAO for all CRUD operations on the [Bid] table.
 */
@Dao
interface BidDao {

    @Query("SELECT * FROM bids WHERE listingId = :listingId ORDER BY createdAt DESC")
    fun getBidsForListing(listingId: String): Flow<List<Bid>>

    @Query("SELECT * FROM bids WHERE supplierId = :supplierId ORDER BY createdAt DESC")
    fun getBidsBySupplier(supplierId: String): Flow<List<Bid>>

    @Query("SELECT * FROM bids WHERE bidId = :bidId")
    fun getBidById(bidId: String): Flow<Bid?>

    @Query("SELECT * FROM bids WHERE listingId = :listingId AND status = :status")
    fun getBidsByListingAndStatus(listingId: String, status: BidStatus): Flow<List<Bid>>

    @Upsert
    suspend fun upsertBid(bid: Bid)

    @Upsert
    suspend fun upsertBids(bids: List<Bid>)

    @Query("DELETE FROM bids WHERE bidId = :bidId")
    suspend fun deleteBid(bidId: String)

    @Query("DELETE FROM bids WHERE listingId = :listingId")
    suspend fun deleteBidsForListing(listingId: String)
}
