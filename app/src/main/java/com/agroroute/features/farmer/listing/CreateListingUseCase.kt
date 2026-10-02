package com.agroroute.features.farmer.listing

import com.agroroute.core.model.HarvestListing
import com.agroroute.core.util.Result
import com.agroroute.core.repository.HarvestListingRepository
import com.agroroute.core.repository.ListingPhotoStorage
import com.agroroute.core.repository.AuthRepository
import com.agroroute.core.model.UserRole
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * Creates or updates a [HarvestListing] through the listing repository.
 */
class CreateListingUseCase @Inject constructor(
    private val repository: HarvestListingRepository,
    private val listingPhotoStorage: ListingPhotoStorage,
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(listing: HarvestListing): Result<Unit> {
        val farmerId = authRepository.currentUserId
            ?: return Result.Error("Sign in as a Farmer to publish listings")
        val profile = authRepository.getCurrentUserProfile().first().getOrNull()
        if (profile?.role != UserRole.FARMER || profile.userId != farmerId || listing.farmerId != farmerId) {
            return Result.Error("Only the signed-in Farmer can publish this listing")
        }
        // Basic validation before delegating to repository
        if (listing.cropName.isBlank()) return Result.Error("Crop name cannot be empty")
        if (listing.category == null) return Result.Error("Harvest category is required")
        if (listing.quantity <= 0) return Result.Error("Quantity must be greater than zero")
        if (listing.reservePrice < 0) return Result.Error("Reserve price cannot be negative")
        val localUri = listing.imageUrls.firstOrNull()?.takeIf {
            it.startsWith("content://") || it.startsWith("file://")
        }
        val publishedListing = if (localUri == null) {
            listing
        } else {
            val remoteUrl = try {
                listingPhotoStorage.uploadListingPhoto(localUri, listing.farmerId, listing.listingId)
            } catch (error: Exception) {
                return Result.Error(error.message ?: "Photo upload failed. Check your connection and retry.", error)
            }
            listing.copy(imageUrls = listOf(remoteUrl))
        }
        return repository.saveListing(publishedListing)
    }
}

class GetListingDraftsUseCase @Inject constructor(
    private val repository: HarvestListingRepository
) {
    operator fun invoke(farmerId: String) = repository.getDraftsByFarmer(farmerId)
}

class SaveListingDraftUseCase @Inject constructor(
    private val repository: HarvestListingRepository
) {
    suspend operator fun invoke(listing: HarvestListing): Result<Unit> =
        repository.saveDraft(listing.copy(isDraft = true))
}
