package com.agroroute.features.farmer.listing

import com.agroroute.core.util.Result
import com.agroroute.core.repository.HarvestListingRepository
import com.agroroute.core.repository.AuthRepository
import com.agroroute.core.model.UserRole
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * Toggles the bidding-enabled flag on a listing, allowing farmers to pause/resume offers.
 */
class ToggleBiddingModeUseCase @Inject constructor(
    private val repository: HarvestListingRepository,
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(listingId: String, enabled: Boolean): Result<Unit> {
        val farmerId = authRepository.currentUserId
            ?: return Result.Error("Sign in to manage this listing")
        val profileResult = authRepository.getCurrentUserProfile().first()
        val profile = when (profileResult) {
            is Result.Success -> profileResult.data
            is Result.Error -> return profileResult
            Result.Loading -> return Result.Error("Unable to verify your account")
        }
        if (profile?.role != UserRole.FARMER || profile.userId != farmerId) {
            return Result.Error("Only the listing's Farmer can change bidding")
        }

        val listingResult = repository.getListingById(listingId).first()
        val listing = when (listingResult) {
            is Result.Success -> listingResult.data
            is Result.Error -> return listingResult
            Result.Loading -> return Result.Error("Unable to load this listing")
        } ?: return Result.Error("Listing not found")
        if (listing.farmerId != farmerId) {
            return Result.Error("Only the listing's Farmer can change bidding")
        }

        return repository.toggleBiddingMode(listingId, enabled)
    }
}
