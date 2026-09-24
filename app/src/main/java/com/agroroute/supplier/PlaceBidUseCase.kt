package com.agroroute.supplier

import com.agroroute.common.model.Bid
import com.agroroute.common.util.Result
import com.agroroute.farmer.HarvestListingRepository
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
