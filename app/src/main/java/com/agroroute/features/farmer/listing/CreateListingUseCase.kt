package com.agroroute.features.farmer.listing

import com.agroroute.core.model.HarvestListing
import com.agroroute.core.util.Result
import com.agroroute.core.repository.HarvestListingRepository
import javax.inject.Inject

/**
 * Creates or updates a [HarvestListing] through the listing repository.
 */
class CreateListingUseCase @Inject constructor(
    private val repository: HarvestListingRepository
) {
    suspend operator fun invoke(listing: HarvestListing): Result<Unit> {
        // Basic validation before delegating to repository
        if (listing.cropName.isBlank()) return Result.Error("Crop name cannot be empty")
        if (listing.category == null) return Result.Error("Harvest category is required")
        if (listing.quantity <= 0) return Result.Error("Quantity must be greater than zero")
        if (listing.reservePrice < 0) return Result.Error("Reserve price cannot be negative")
        return repository.saveListing(listing)
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
