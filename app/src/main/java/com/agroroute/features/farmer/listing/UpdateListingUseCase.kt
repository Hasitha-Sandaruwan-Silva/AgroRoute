package com.agroroute.features.farmer.listing

import com.agroroute.core.model.BidStatus
import com.agroroute.core.model.HarvestListing
import com.agroroute.core.model.HarvestStatus
import com.agroroute.core.model.UserRole
import com.agroroute.core.repository.AuthRepository
import com.agroroute.core.repository.BidRepository
import com.agroroute.core.repository.HarvestListingRepository
import com.agroroute.core.repository.ListingPhotoStorage
import com.agroroute.core.repository.OrderRepository
import com.agroroute.core.util.Result
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class UpdateListingUseCase @Inject constructor(
    private val listingRepository: HarvestListingRepository,
    private val listingPhotoStorage: ListingPhotoStorage,
    private val authRepository: AuthRepository,
    private val bidRepository: BidRepository,
    private val orderRepository: OrderRepository
) {
    suspend operator fun invoke(listing: HarvestListing): Result<Unit> {
        val farmerId = authRepository.currentUserId
            ?: return Result.Error("Sign in as a Farmer to edit listings")
        val profile = authRepository.getCurrentUserProfile().first().getOrNull()
        if (profile?.role != UserRole.FARMER || profile.userId != farmerId || listing.farmerId != farmerId) {
            return Result.Error("Only the signed-in owner can edit this listing")
        }
        val current = listingRepository.getListingById(listing.listingId).first().getOrNull()
            ?: return Result.Error("Listing not found")
        if (current.farmerId != farmerId || current.isDraft) {
            return Result.Error("You can only edit your own published listing")
        }

        if (com.agroroute.core.model.isPickupCoordinateRepair(current, listing)) {
            return listingRepository.updateListing(listing)
        }

        if (listing.cropName.isBlank()) return Result.Error("Crop name cannot be empty")
        if (listing.category == null) return Result.Error("Harvest category is required")
        if (listing.quantity <= 0.0) return Result.Error("Quantity must be greater than zero")
        if (listing.reservePrice < 0.0) return Result.Error("Reserve price cannot be negative")
        if (listing.harvestStatus == HarvestStatus.EXPECTED) {
            if (listing.expectedHarvestAt == null) {
                return Result.Error("An estimated harvest date is required")
            }
            if ((listing.estimatedQuantity ?: listing.quantity) <= 0.0) {
                return Result.Error("Estimated quantity must be greater than zero")
            }
        }

        when (val sync = bidRepository.syncBidsForListing(listing.listingId)) {
            is Result.Error -> return Result.Error("Could not verify listing bids. Try again.", sync.cause)
            Result.Loading -> return Result.Error("Could not verify listing bids. Try again.")
            is Result.Success -> Unit
        }
        val bids = when (val result = bidRepository.getBidsForListing(listing.listingId).first()) {
            is Result.Success -> result.data
            is Result.Error -> return Result.Error("Could not verify listing bids. Try again.", result.cause)
            Result.Loading -> return Result.Error("Could not verify listing bids. Try again.")
        }
        if (bids.any { it.status == BidStatus.PENDING }) {
            return Result.Error("This listing has pending bids. Resolve them before editing it.")
        }

        when (val sync = orderRepository.syncOrders(farmerId)) {
            is Result.Error -> return Result.Error("Could not verify listing orders. Try again.", sync.cause)
            Result.Loading -> return Result.Error("Could not verify listing orders. Try again.")
            is Result.Success -> Unit
        }
        val orders = when (val result = orderRepository.getOrdersForFarmer(farmerId).first()) {
            is Result.Success -> result.data
            is Result.Error -> return Result.Error("Could not verify listing orders. Try again.", result.cause)
            Result.Loading -> return Result.Error("Could not verify listing orders. Try again.")
        }
        if (orders.any { it.listingId == listing.listingId }) {
            return Result.Error("This listing has order history. It cannot be edited without changing agreed order terms.")
        }

        if (listing.imageUrls.size > MAX_LISTING_PHOTOS) return Result.Error("Maximum 5 photos")
        val updatedListing = try {
            listing.copy(imageUrls = uploadListingPhotos(listing, listingPhotoStorage))
        } catch (cancelled: kotlinx.coroutines.CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            return Result.Error("Photo upload failed. Check your connection and retry.", error)
        }
        return listingRepository.updateListing(updatedListing)
    }
}