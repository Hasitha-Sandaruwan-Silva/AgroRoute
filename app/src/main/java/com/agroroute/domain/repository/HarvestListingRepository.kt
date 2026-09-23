package com.agroroute.domain.repository

import com.agroroute.data.model.HarvestListing
import com.agroroute.data.model.ListingStatus
import com.agroroute.util.Result
import kotlinx.coroutines.flow.Flow

/**
 * Domain contract for all operations related to [HarvestListing] — implemented in the data layer.
 */
interface HarvestListingRepository {

    /** Emits all listings in real-time, sourced from Room as local truth. */
    fun getAllListings(): Flow<Result<List<HarvestListing>>>

    /** Emits listings belonging to the specified farmer. */
    fun getListingsByFarmer(farmerId: String): Flow<Result<List<HarvestListing>>>

    /** Emits listings filtered by [status]. */
    fun getListingsByStatus(status: ListingStatus): Flow<Result<List<HarvestListing>>>

    /** Emits a single listing by ID, or [Result.Error] if not found. */
    fun getListingById(listingId: String): Flow<Result<HarvestListing?>>

    /** Creates or updates a listing in Firestore and mirrors to Room on success. */
    suspend fun saveListing(listing: HarvestListing): Result<Unit>

    /** Deletes a listing from both Firestore and Room. */
    suspend fun deleteListing(listingId: String): Result<Unit>

    /** Fetches all listings from Firestore and refreshes the Room cache. */
    suspend fun syncListings(): Result<Unit>

    /** Toggles the [HarvestListing.biddingEnabled] flag for the given listing. */
    suspend fun toggleBiddingMode(listingId: String, enabled: Boolean): Result<Unit>
}
