package com.agroroute.features.farmer.listing

import com.agroroute.core.util.Result
import com.agroroute.core.repository.HarvestListingRepository
import javax.inject.Inject

/**
 * Toggles the bidding-enabled flag on a listing, allowing farmers to pause/resume offers.
 */
class ToggleBiddingModeUseCase @Inject constructor(
    private val repository: HarvestListingRepository
) {
    suspend operator fun invoke(listingId: String, enabled: Boolean): Result<Unit> =
        repository.toggleBiddingMode(listingId, enabled)
}
