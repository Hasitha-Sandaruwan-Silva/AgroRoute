package com.agroroute.core.repository

import com.agroroute.core.database.HarvestListingDao
import com.agroroute.core.model.HarvestListing
import com.agroroute.core.model.ListingStatus
import com.agroroute.core.model.setBiddingEnabled
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
 * Implements [HarvestListingRepository] using a sync-on-fetch strategy:
 * - Reads always come from Room (source of truth, reactive Flow).
 * - Writes go to Firestore first; on success the local Room cache is updated.
 * - Explicit [syncListings] pulls Firestore data and overwrites the local cache.
 */
@Singleton
class HarvestListingRepositoryImpl @Inject constructor(
    @PublishedApi
    internal val dao: HarvestListingDao,
    @PublishedApi
    internal val firestoreService: FirestoreService
) : HarvestListingRepository {

    // ── Read operations (Room as source of truth) ──────────────────────────────

    override fun getAllListings(): Flow<Result<List<HarvestListing>>> =
        dao.getAllListings()
            .map<List<HarvestListing>, Result<List<HarvestListing>>> { Result.Success(it) }
            .catch { e -> emit(Result.Error(e.message ?: "Failed to load listings", e)) }

    override fun getListingsByFarmer(farmerId: String): Flow<Result<List<HarvestListing>>> =
        dao.getListingsByFarmer(farmerId)
            .map<List<HarvestListing>, Result<List<HarvestListing>>> { Result.Success(it) }
            .catch { e -> emit(Result.Error(e.message ?: "Failed to load farmer listings", e)) }

    override fun getDraftsByFarmer(farmerId: String): Flow<Result<List<HarvestListing>>> =
        dao.getDraftsByFarmer(farmerId)
            .map<List<HarvestListing>, Result<List<HarvestListing>>> { Result.Success(it) }
            .catch { e -> emit(Result.Error(e.message ?: "Failed to load listing drafts", e)) }

    override fun getListingsByStatus(status: ListingStatus): Flow<Result<List<HarvestListing>>> =
        dao.getListingsByStatus(status)
            .map<List<HarvestListing>, Result<List<HarvestListing>>> { Result.Success(it) }
            .catch { e -> emit(Result.Error(e.message ?: "Failed to load listings", e)) }

    override fun getListingById(listingId: String): Flow<Result<HarvestListing?>> =
        dao.getListingById(listingId)
            .map<HarvestListing?, Result<HarvestListing?>> { Result.Success(it) }
            .catch { e -> emit(Result.Error(e.message ?: "Failed to load listing", e)) }

    // ── Write operations (Firestore first, then Room mirror) ───────────────────

    override suspend fun saveListing(listing: HarvestListing): Result<Unit> = safeCall {
        firestoreService.set(Constants.COLLECTION_LISTINGS, listing.listingId, listing)
        dao.upsertListing(listing) // Mirror to Room on Firestore success
    }

    override suspend fun saveDraft(listing: HarvestListing): Result<Unit> = safeCall {
        dao.upsertListing(listing.copy(isDraft = true))
    }

    override suspend fun deleteListing(listingId: String): Result<Unit> = safeCall {
        firestoreService.delete(Constants.COLLECTION_LISTINGS, listingId)
        dao.deleteListing(listingId)
    }

    // ── Sync (Firestore → Room cache refresh) ──────────────────────────────────

    override suspend fun syncListings(): Result<Unit> = safeCall {
        val remote = firestoreService.collectionFlow<HarvestListing>(Constants.COLLECTION_LISTINGS)
        // Collect one snapshot, write to Room, then done.
        // For a production app, consider a permanent listener instead.
        remote.first().let { listings ->
            dao.upsertListings(listings)
        }
    }

    override suspend fun toggleBiddingMode(listingId: String, enabled: Boolean): Result<Unit> = safeCall {
        val snapshot = firestoreService.getOnce<HarvestListing>(Constants.COLLECTION_LISTINGS, listingId)
            ?: error("Listing $listingId not found")
        val updated = snapshot.setBiddingEnabled(enabled)
        firestoreService.set(Constants.COLLECTION_LISTINGS, listingId, updated)
        dao.upsertListing(updated)
    }
}
