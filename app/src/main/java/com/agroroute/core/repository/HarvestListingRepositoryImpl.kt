package com.agroroute.core.repository

import com.agroroute.core.database.HarvestListingDao
import com.agroroute.core.database.OrderRequestDao
import com.agroroute.core.model.HarvestListing
import com.agroroute.core.model.HarvestStatus
import com.agroroute.core.model.ListingStatus
import com.agroroute.core.model.OrderRequest
import com.agroroute.core.model.OrderRequestStatus
import com.agroroute.core.model.UserRole
import com.agroroute.core.model.availableStock
import com.agroroute.core.model.markHarvestFailed
import com.agroroute.core.model.markHarvestReady
import com.agroroute.core.repository.AuthRepository
import com.agroroute.core.model.setBiddingEnabled
import com.agroroute.core.notification.NotificationEvent
import com.agroroute.core.notification.NotificationEvent.Payload
import com.agroroute.core.notification.NotificationService
import com.agroroute.core.notification.HarvestReminderScheduler
import com.agroroute.core.remote.FirestoreService
import com.agroroute.core.util.Constants
import com.agroroute.core.util.Result
import com.agroroute.core.util.safeCall
import com.google.firebase.firestore.SetOptions
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
    internal val firestoreService: FirestoreService,
    private val authRepository: AuthRepository,
    private val orderRequestDao: OrderRequestDao,
    private val notificationService: NotificationService,
    private val harvestReminderScheduler: HarvestReminderScheduler
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
        val farmerId = authRepository.currentUserId ?: error("Sign in to publish listings")
        val profile = authRepository.getCurrentUserProfile().first().getOrNull()
        check(profile?.role == UserRole.FARMER && profile.userId == farmerId && listing.farmerId == farmerId) {
            "Only the signed-in Farmer can publish this listing"
        }
        check(!listing.isDraft) { "Draft listings must remain in local storage" }
        firestoreService.set(Constants.COLLECTION_LISTINGS, listing.listingId, listing)
        dao.upsertListing(listing) // Mirror to Room on Firestore success
    }

    override suspend fun updateListing(listing: HarvestListing): Result<Unit> = safeCall {
        val farmerId = authRepository.currentUserId ?: error("Sign in to edit listings")
        val profile = authRepository.getCurrentUserProfile().first().getOrNull()
        check(profile?.role == UserRole.FARMER && profile.userId == farmerId) {
            "Only the signed-in Farmer can edit listings"
        }
        check(listing.farmerId == farmerId) { "You can only edit your own listing" }
        check(!listing.isDraft) { "Only published listings can be edited here" }

        val existing = firestoreService.getOnce<HarvestListing>(Constants.COLLECTION_LISTINGS, listing.listingId)
            ?: error("Listing not found")
        check(existing.farmerId == farmerId) { "You can only edit your own listing" }
        check(existing.listingId == listing.listingId) { "Listing identity does not match" }
        check(listing.quantity > 0.0) { "Quantity must be greater than zero" }
        check(listing.quantity >= existing.soldQuantity) {
            "Quantity cannot be less than the amount already sold or reserved"
        }
        check(existing.soldQuantity <= 0.0 || listing.quantityUnit == existing.quantityUnit) {
            "Quantity unit cannot change after stock has been sold or reserved"
        }
        check(listing.harvestStatus == existing.harvestStatus) {
            "Use mark harvest ready or failed to change harvest status"
        }
        check(existing.harvestStatus != HarvestStatus.FAILED) {
            "A failed harvest cannot be edited"
        }

        val isExpected = existing.harvestStatus == HarvestStatus.EXPECTED
        if (isExpected && listing.estimatedQuantity != null && listing.estimatedQuantity != existing.estimatedQuantity) {
            val pendingRequests = firestoreService.queryFlow<OrderRequest>(
                Constants.COLLECTION_ORDER_REQUESTS,
                "listingId",
                listing.listingId
            ).first().filter {
                it.listingId == listing.listingId && it.isPreHarvest && it.status == OrderRequestStatus.PENDING
            }
            val maxPending = pendingRequests.maxOfOrNull { it.requestedQuantity } ?: 0.0
            val newEstimate = listing.estimatedQuantity ?: listing.quantity
            check(newEstimate >= maxPending) {
                "Estimated quantity cannot be lowered below the largest pending pre-harvest request ($maxPending)"
            }
        }
        val updatedAvailable = if (isExpected) {
            0.0
        } else {
            (existing.availableStock() + listing.quantity - existing.quantity).coerceAtLeast(0.0)
        }
        val estimateChanged = isExpected && (
            listing.expectedHarvestAt != existing.expectedHarvestAt ||
                listing.estimatedQuantity != existing.estimatedQuantity
            )
        val updated = listing.copy(
            farmerId = existing.farmerId,
            farmerName = existing.farmerName,
            status = existing.status,
            biddingEnabled = existing.biddingEnabled,
            createdAt = existing.createdAt,
            expiresAt = existing.expiresAt,
            availableQuantity = updatedAvailable,
            soldQuantity = existing.soldQuantity,
            isDraft = false,
            harvestStatus = existing.harvestStatus,
            harvestFailureReason = listing.harvestFailureReason ?: existing.harvestFailureReason,
            harvestUpdatedAt = if (estimateChanged) System.currentTimeMillis() else existing.harvestUpdatedAt
        )
        firestoreService.updateFields(
            Constants.COLLECTION_LISTINGS,
            listing.listingId,
            mapOf(
                "cropName" to updated.cropName,
                "description" to updated.description,
                "quantity" to updated.quantity,
                "quantityUnit" to updated.quantityUnit.name,
                "reservePrice" to updated.reservePrice,
                "imageUrls" to updated.imageUrls,
                "location" to updated.location,
                "category" to updated.category?.name,
                "expectedHarvestAt" to updated.expectedHarvestAt,
                "qualityGrade" to updated.qualityGrade,
                "availableQuantity" to updated.availableQuantity,
                "harvestStatus" to updated.harvestStatus.name,
                "estimatedQuantity" to updated.estimatedQuantity,
                "harvestFailureReason" to updated.harvestFailureReason,
                "harvestUpdatedAt" to updated.harvestUpdatedAt
            )
        )
        dao.upsertListing(updated)
        if (estimateChanged) notifyPreHarvestRequests(updated, farmerId, NotificationEvent.HARVEST_UPDATE)
    }

    override suspend fun saveDraft(listing: HarvestListing): Result<Unit> = safeCall {
        val farmerId = authRepository.currentUserId ?: error("Sign in to save a listing draft")
        val profile = authRepository.getCurrentUserProfile().first().getOrNull()
        check(profile?.role == UserRole.FARMER && profile.userId == farmerId && listing.farmerId == farmerId) {
            "Only the signed-in Farmer can save this draft"
        }
        dao.upsertListing(listing.copy(isDraft = true))
    }

    override suspend fun deleteListing(listingId: String): Result<Unit> = safeCall {
        val farmerId = authRepository.currentUserId ?: error("Sign in to delete listings")
        val profile = authRepository.getCurrentUserProfile().first().getOrNull()
        check(profile?.role == UserRole.FARMER && profile.userId == farmerId) {
            "Only Farmers can delete listings"
        }
        val listing = firestoreService.getOnce<HarvestListing>(Constants.COLLECTION_LISTINGS, listingId)
            ?: error("Listing not found")
        check(listing.farmerId == farmerId) { "You cannot delete another Farmer's listing" }
        firestoreService.delete(Constants.COLLECTION_LISTINGS, listingId)
        dao.deleteListing(listingId)
    }

    // ── Sync (Firestore → Room cache refresh) ──────────────────────────────────



    override suspend fun syncListings(): Result<Unit> = safeCall {
        val userId = authRepository.currentUserId ?: error("Sign in to sync listings")
        val profile = authRepository.getCurrentUserProfile().first().getOrNull()
            ?: error("Account profile is unavailable")
        check(profile.userId == userId) { "Account profile does not match the signed-in user" }

        val listings = when (profile.role) {
            UserRole.FARMER -> firestoreService.queryFlow<HarvestListing>(
                Constants.COLLECTION_LISTINGS,
                "farmerId",
                userId
            ).first()
            UserRole.SUPPLIER -> firestoreService.queryFlow<HarvestListing>(
                Constants.COLLECTION_LISTINGS,
                "status",
                ListingStatus.OPEN.name
            ).first().filterNot { it.isDraft }
        }
        dao.upsertListings(listings)
          if (profile.role == UserRole.FARMER) listings.forEach(harvestReminderScheduler::schedule)
          if (profile.role == UserRole.FARMER) listings.forEach(harvestReminderScheduler::schedule)
    }

    override suspend fun toggleBiddingMode(listingId: String, enabled: Boolean): Result<Unit> = safeCall {
        val farmerId = authRepository.currentUserId ?: error("Sign in to manage this listing")
        val profile = authRepository.getCurrentUserProfile().first().getOrNull()
        check(profile?.role == UserRole.FARMER && profile.userId == farmerId) {
            "Only Farmers can change bidding"
        }
        val snapshot = firestoreService.getOnce<HarvestListing>(Constants.COLLECTION_LISTINGS, listingId)
            ?: error("Listing $listingId not found")
        check(snapshot.farmerId == farmerId) { "Only the listing's Farmer can change bidding" }
        val updated = snapshot.setBiddingEnabled(enabled)
        firestoreService.set(Constants.COLLECTION_LISTINGS, listingId, updated)
        dao.upsertListing(updated)
    }

    override suspend fun markHarvestReady(listingId: String, actualQuantity: Double): Result<Unit> = safeCall {
        val farmerId = authRepository.currentUserId ?: error("Sign in to update this harvest")
        val profile = authRepository.getCurrentUserProfile().first().getOrNull()
        check(profile?.role == UserRole.FARMER && profile.userId == farmerId) {
            "Only Farmers can update harvest status"
        }
        val snapshot = firestoreService.getOnce<HarvestListing>(Constants.COLLECTION_LISTINGS, listingId)
            ?: error("Listing $listingId not found")
        check(snapshot.farmerId == farmerId) { "Only the listing's Farmer can update this harvest" }
        val updated = snapshot.markHarvestReady(actualQuantity)
        firestoreService.updateFields(
            Constants.COLLECTION_LISTINGS,
            listingId,
            mapOf(
                "quantity" to updated.quantity,
                "availableQuantity" to updated.availableQuantity,
                "soldQuantity" to updated.soldQuantity,
                "harvestStatus" to updated.harvestStatus.name,
                "harvestFailureReason" to updated.harvestFailureReason,
                "harvestUpdatedAt" to updated.harvestUpdatedAt
            )
        )
        dao.upsertListing(updated)
        harvestReminderScheduler.schedule(updated)
        notifyPreHarvestRequests(updated, farmerId, NotificationEvent.HARVEST_READY)
    }

    override suspend fun markHarvestFailed(listingId: String, reason: String?): Result<Unit> = safeCall {
        val farmerId = authRepository.currentUserId ?: error("Sign in to update this harvest")
        val profile = authRepository.getCurrentUserProfile().first().getOrNull()
        check(profile?.role == UserRole.FARMER && profile.userId == farmerId) {
            "Only Farmers can update harvest status"
        }
        val snapshot = firestoreService.getOnce<HarvestListing>(Constants.COLLECTION_LISTINGS, listingId)
            ?: error("Listing $listingId not found")
        check(snapshot.farmerId == farmerId) { "Only the listing's Farmer can update this harvest" }
        val pending = firestoreService.queryFlow<OrderRequest>(
            Constants.COLLECTION_ORDER_REQUESTS,
            "listingId",
            listingId
        ).first().filter {
            it.listingId == listingId && it.isPreHarvest && it.status == OrderRequestStatus.PENDING
        }
        val firestore = firestoreService.firestore
        val listingRef = firestore.collection(Constants.COLLECTION_LISTINGS).document(listingId)
        val requestRefs = pending.map {
            firestore.collection(Constants.COLLECTION_ORDER_REQUESTS).document(it.requestId)
        }
        val now = System.currentTimeMillis()
        val updatedRecords = firestoreService.runTransaction<Pair<HarvestListing, List<OrderRequest>>> { transaction ->
            val currentListing = transaction.get(listingRef).toObject(HarvestListing::class.java)
                ?: error("Listing $listingId not found")
            check(currentListing.farmerId == farmerId) { "Only the listing's Farmer can update this harvest" }
            val currentRequests = requestRefs.map { ref ->
                ref to transaction.get(ref).toObject(OrderRequest::class.java)
            }
            val failedListing = currentListing.markHarvestFailed(reason)
            transaction.set(listingRef, failedListing)
            val cancelled = currentRequests.mapNotNull { (ref, current) ->
                if (current != null &&
                    current.listingId == listingId &&
                    current.isPreHarvest &&
                    current.status == OrderRequestStatus.PENDING
                ) {
                    val updatedRequest = current.copy(status = OrderRequestStatus.CANCELLED, updatedAt = now)
                    transaction.set(ref, updatedRequest)
                    updatedRequest
                } else {
                    null
                }
            }
            failedListing to cancelled
        }
        dao.upsertListing(updatedRecords.first)
        harvestReminderScheduler.schedule(updatedRecords.first)
        updatedRecords.second.forEach { orderRequestDao.upsertRequest(it) }
        updatedRecords.second.forEach { request ->
            notificationService.sendPushToServer(
                recipientUserId = request.supplierId,
                event = NotificationEvent.HARVEST_FAILED,
                payload = Payload(
                    cropName = request.cropName,
                    supplierName = request.supplierName,
                    requestId = request.requestId,
                    listingId = request.listingId,
                    status = request.status.name
                ),
                actorUserId = farmerId
            )
        }
    }

    private suspend fun notifyPreHarvestRequests(
        listing: HarvestListing,
        farmerId: String,
        event: NotificationEvent
    ) {
        val pendingRequests = firestoreService.queryFlow<OrderRequest>(
            Constants.COLLECTION_ORDER_REQUESTS,
            "listingId",
            listing.listingId
        ).first().filter { it.isPreHarvest && it.status == OrderRequestStatus.PENDING }
        pendingRequests.forEach { request ->
            notificationService.sendPushToServer(
                recipientUserId = request.supplierId,
                event = event,
                payload = Payload(
                    cropName = listing.cropName,
                    supplierName = request.supplierName,
                    requestId = request.requestId,
                    listingId = listing.listingId,
                    status = listing.harvestStatus.name
                ),
                actorUserId = farmerId
            )
        }
    }
}
