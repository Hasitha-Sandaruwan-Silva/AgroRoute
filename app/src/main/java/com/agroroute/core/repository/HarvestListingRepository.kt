package com.agroroute.core.repository

import com.agroroute.core.model.HarvestListing
import com.agroroute.core.model.ListingStatus
import com.agroroute.core.util.Result
import kotlinx.coroutines.flow.Flow

/**
 * Domain contract for all operations related to [HarvestListing] — implemented in the data layer.
 */
interface HarvestListingRepository {

    /** Emits all listings in real-time, sourced from Room as local truth. */
    fun getAllListings(): Flow<Result<List<HarvestListing>>>

    /** Emits listings belonging to the specified farmer. */
    fun getListingsByFarmer(farmerId: String): Flow<Result<List<HarvestListing>>>

    /** Emits local-only draft listings for [farmerId]. */
    fun getDraftsByFarmer(farmerId: String): Flow<Result<List<HarvestListing>>>

    /** Emits listings filtered by [status]. */
    fun getListingsByStatus(status: ListingStatus): Flow<Result<List<HarvestListing>>>

    /** Emits a single listing by ID, or [Result.Error] if not found. */
    fun getListingById(listingId: String): Flow<Result<HarvestListing?>>

    /** Creates or updates a listing in Firestore and mirrors to Room on success. */
    suspend fun saveListing(listing: HarvestListing): Result<Unit>

    /** Updates an existing owner listing in Firestore and mirrors it to Room on success. */
    suspend fun updateListing(listing: HarvestListing): Result<Unit> = saveListing(listing)

    /** Saves an incomplete listing locally without publishing it to Firestore. */
    suspend fun saveDraft(listing: HarvestListing): Result<Unit>

    /** Closes using current remote state; never replaces stock or historical fields. */
    suspend fun closeListing(listingId: String): Result<Unit> =
        Result.Error("Closing listings is unavailable here")

    /** Permanent removal is disabled until history can be securely verified server-side. */
    suspend fun deleteListing(listingId: String): Result<Unit>

    /** Fetches all listings from Firestore and refreshes the Room cache. */
    suspend fun syncListings(): Result<Unit>

    /** Toggles the [HarvestListing.biddingEnabled] flag for the given listing. */
    suspend fun toggleBiddingMode(listingId: String, enabled: Boolean): Result<Unit>

    /** Marks an expected harvest as ready, recording the actual available quantity. */
    suspend fun markHarvestReady(listingId: String, actualQuantity: Double): Result<Unit> =
        Result.Error("Marking a harvest ready is not supported here")

    /** Marks a harvest as failed/unavailable, stopping new purchase requests. */
    suspend fun markHarvestFailed(listingId: String, reason: String?): Result<Unit> =
        Result.Error("Marking a harvest failed is not supported here")
}
