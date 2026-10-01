package com.agroroute.features.farmer.bids

import com.agroroute.core.model.Bid
import com.agroroute.core.model.BidStatus
import com.agroroute.core.model.HarvestListing
import com.agroroute.core.model.UserRole
import com.agroroute.core.model.availableStock
import com.agroroute.core.model.Order
import com.agroroute.core.model.OrderStatus
import com.agroroute.core.repository.AuthRepository
import com.agroroute.core.repository.BidRepository
import com.agroroute.core.util.Result
import com.agroroute.core.repository.OrderRepository
import com.agroroute.core.repository.HarvestListingRepository
import java.util.UUID
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * Orchestrates the bid acceptance flow:
 * 1. Marks the accepted bid as ACCEPTED.
 * 2. Rejects all other bids for the same listing.
 * 3. Creates an [Order] from the accepted bid.
 * 4. Closes the listing.
 */
class AcceptBidUseCase @Inject constructor(
    private val orderRepository: OrderRepository,
) {
    suspend operator fun invoke(
        bid: Bid,
        listing: HarvestListing
    ): Result<Unit> {
        val order = Order(
            orderId      = UUID.randomUUID().toString(),
            listingId    = listing.listingId,
            bidId        = bid.bidId,
            farmerId     = listing.farmerId,
            supplierId   = bid.supplierId,
            cropName     = listing.cropName,
            quantity     = listing.availableStock(),
            quantityUnit = listing.quantityUnit,
            agreedPrice  = bid.offerPrice,
            status       = OrderStatus.AWAITING_PICKUP,
            pickupLocation  = listing.location,
            deliveryLocation = ""  // Supplier provides during fulfilment
        )
        return orderRepository.acceptBidAndCreateOrder(order)
    }
}

class RejectBidUseCase @Inject constructor(
    private val bidRepository: BidRepository,
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(bid: Bid): Result<Unit> {
        val profile = authRepository.getCurrentUserProfile().first().getOrNull()
        if (profile?.role != UserRole.FARMER) {
            return Result.Error("Only Farmers can respond to incoming bids")
        }
        if (bid.status != BidStatus.PENDING) {
            return Result.Error("Only pending bids can be rejected")
        }
        return bidRepository.updateBidStatus(bid.bidId, BidStatus.REJECTED)
    }
}
