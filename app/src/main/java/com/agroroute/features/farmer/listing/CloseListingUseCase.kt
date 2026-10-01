package com.agroroute.features.farmer.listing

import com.agroroute.core.model.HarvestListing
import com.agroroute.core.model.ListingStatus
import com.agroroute.core.model.closeForNewBids
import com.agroroute.core.repository.HarvestListingRepository
import com.agroroute.core.util.Result
import javax.inject.Inject

class CloseListingUseCase @Inject constructor(
    private val repository: HarvestListingRepository
) {
    suspend operator fun invoke(listing: HarvestListing): Result<Unit> {
        if (listing.status != ListingStatus.OPEN) return Result.Error("Only open listings can be closed")
        return repository.saveListing(listing.closeForNewBids())
    }
}