package com.agroroute.farmer

import com.agroroute.common.model.HarvestListing
import com.agroroute.common.util.Result
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
        if (listing.quantity <= 0) return Result.Error("Quantity must be greater than zero")
        if (listing.reservePrice < 0) return Result.Error("Reserve price cannot be negative")
        return repository.saveListing(listing)
    }
}
