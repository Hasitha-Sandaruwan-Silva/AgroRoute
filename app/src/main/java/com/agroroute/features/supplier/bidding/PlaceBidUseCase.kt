package com.agroroute.features.supplier.bidding

import com.agroroute.core.model.Bid
import com.agroroute.core.model.BidStatus
import com.agroroute.core.model.UserRole
import com.agroroute.core.model.acceptsNewBids
import com.agroroute.core.util.Result
import com.agroroute.core.repository.AuthRepository
import com.agroroute.core.repository.HarvestListingRepository
import com.agroroute.core.repository.BidRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * Places a [Bid] against an active listing, with pre-flight validation.
 */
class PlaceBidUseCase @Inject constructor(
    private val bidRepository: BidRepository,
    private val listingRepository: HarvestListingRepository,
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(bid: Bid): Result<Unit> {
        if (bid.offerPrice <= 0) return Result.Error("Offer price must be greater than zero")
        if (bid.supplierId.isBlank()) return Result.Error("Sign in as a Supplier to continue")
        if (bid.listingId.isBlank()) return Result.Error("Choose a listing to continue")
        val supplierId = authRepository.currentUserId
            ?: return Result.Error("Sign in as a Supplier to place bids")

        val profileResult = authRepository.getCurrentUserProfile().first()
        val currentRole = when (profileResult) {
            is Result.Success -> profileResult.data?.role
            is Result.Error -> return profileResult
            Result.Loading -> null
        }
        val profile = profileResult.getOrNull()
        if (currentRole != UserRole.SUPPLIER || profile?.userId != supplierId || bid.supplierId != supplierId) {
            return Result.Error("Only Suppliers can place bids")
        }

        val listingResult = listingRepository.getListingById(bid.listingId).first()
        val listing = when (listingResult) {
            is Result.Success -> listingResult.data
            is Result.Error -> return listingResult
            Result.Loading -> null
        } ?: return Result.Error("Listing is no longer available")
        if (!listing.acceptsNewBids()) {
            return Result.Error("This listing is not accepting bids")
        }
        if (com.agroroute.core.location.SelectedLocation.fromStoredText(listing.location) == null)
            return Result.Error("Farmer pickup location is missing. Ask the farmer to select the listing's pickup pin.")
        if (com.agroroute.core.location.SelectedLocation.fromStoredText(bid.deliveryLocation) == null)
            return Result.Error("Select a delivery location on the map before placing your bid")

        return bidRepository.placeBid(bid.copy(status = BidStatus.PENDING))
    }
}
