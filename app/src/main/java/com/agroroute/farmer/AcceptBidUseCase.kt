package com.agroroute.farmer

import com.agroroute.common.model.Bid
import com.agroroute.common.model.BidStatus
import com.agroroute.common.model.HarvestListing
import com.agroroute.common.model.ListingStatus
import com.agroroute.common.model.Order
import com.agroroute.common.model.OrderStatus
import com.agroroute.common.util.Result
import com.agroroute.common.order.OrderRepository
import com.agroroute.supplier.BidRepository
import java.util.UUID
import javax.inject.Inject

/**
 * Orchestrates the bid acceptance flow:
 * 1. Marks the accepted bid as ACCEPTED.
 * 2. Rejects all other bids for the same listing.
 * 3. Creates an [Order] from the accepted bid.
 * 4. Closes the listing.
 */
class AcceptBidUseCase @Inject constructor(
    private val bidRepository: BidRepository,
    private val orderRepository: OrderRepository,
    private val listingRepository: HarvestListingRepository
) {
    suspend operator fun invoke(
        bid: Bid,
        listing: HarvestListing
    ): Result<Unit> {
        // Step 1: Accept the winning bid
        val acceptResult = bidRepository.updateBidStatus(bid.bidId, BidStatus.ACCEPTED)
        if (acceptResult is Result.Error) return acceptResult

        // Step 2: Create the order
        val order = Order(
            orderId      = UUID.randomUUID().toString(),
            listingId    = listing.listingId,
            bidId        = bid.bidId,
            farmerId     = listing.farmerId,
            supplierId   = bid.supplierId,
            cropName     = listing.cropName,
            quantity     = listing.quantity,
            quantityUnit = listing.quantityUnit,
            agreedPrice  = bid.offerPrice,
            status       = OrderStatus.AWAITING_PICKUP,
            pickupLocation  = listing.location,
            deliveryLocation = ""  // Supplier provides during fulfilment
        )
        val orderResult = orderRepository.createOrder(order)
        if (orderResult is Result.Error) return orderResult

        // Step 3: Close the listing so no more bids are accepted
        val updatedListing = listing.copy(
            status         = ListingStatus.FULFILLED,
            biddingEnabled = false
        )
        return listingRepository.saveListing(updatedListing)
    }
}
