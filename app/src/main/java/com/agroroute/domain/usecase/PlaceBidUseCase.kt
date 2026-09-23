package com.agroroute.domain.usecase

import com.agroroute.data.model.Bid
import com.agroroute.domain.repository.BidRepository
import com.agroroute.domain.repository.HarvestListingRepository
import com.agroroute.util.Result
import javax.inject.Inject

/**
 * Places a [Bid] against an active listing, with pre-flight validation.
 */
class PlaceBidUseCase @Inject constructor(
    private val bidRepository: BidRepository,
    private val listingRepository: HarvestListingRepository
) {
    suspend operator fun invoke(bid: Bid): Result<Unit> {
        if (bid.offerPrice <= 0) return Result.Error("Offer price must be greater than zero")
        if (bid.supplierId.isBlank()) return Result.Error("Supplier ID is required")
        if (bid.listingId.isBlank()) return Result.Error("Listing ID is required")
        return bidRepository.placeBid(bid)
    }
}
