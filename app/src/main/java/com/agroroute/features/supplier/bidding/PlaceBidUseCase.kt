package com.agroroute.features.supplier.bidding

import com.agroroute.core.model.Bid
import com.agroroute.core.model.UserRole
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
        if (bid.supplierId.isBlank()) return Result.Error("Supplier ID is required")
        if (bid.listingId.isBlank()) return Result.Error("Listing ID is required")

        val profileResult = authRepository.getCurrentUserProfile().first()
        val currentRole = when (profileResult) {
            is Result.Success -> profileResult.data?.role
            is Result.Error -> return profileResult
            Result.Loading -> null
        }
        if (currentRole != UserRole.SUPPLIER) {
            return Result.Error("Only Suppliers can place bids")
        }

        return bidRepository.placeBid(bid)
    }
}
