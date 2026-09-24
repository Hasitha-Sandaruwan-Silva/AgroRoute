package com.agroroute.farmer

import com.agroroute.common.util.Result
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
