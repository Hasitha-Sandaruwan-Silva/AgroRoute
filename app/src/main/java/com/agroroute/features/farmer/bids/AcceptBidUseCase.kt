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
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * Orchestrates the bid acceptance flow:
 * Delegates the accepted bid, stock update, and order creation to the repository transaction.
 */
class AcceptBidUseCase @Inject constructor(
    private val orderRepository: OrderRepository,
    private val listingRepository: HarvestListingRepository,
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(
        bid: Bid,
        listing: HarvestListing
    ): Result<Unit> {
        val farmerId = authRepository.currentUserId
            ?: return Result.Error("Sign in as the listing's Farmer to accept bids")
        val profileResult = authRepository.getCurrentUserProfile().first()
        val profile = when (profileResult) {
            is Result.Success -> profileResult.data
            is Result.Error -> return profileResult
            Result.Loading -> return Result.Error("Unable to verify your account")
        }
        if (profile?.role != UserRole.FARMER || profile.userId != farmerId) {
            return Result.Error("Only the listing's Farmer can accept bids")
        }
        if (bid.status != BidStatus.PENDING) {
            return Result.Error("Only pending bids can be accepted")
        }

        val listingResult = listingRepository.getListingById(bid.listingId).first()
        val currentListing = when (listingResult) {
            is Result.Success -> listingResult.data
            is Result.Error -> return listingResult
            Result.Loading -> null
        } ?: return Result.Error("Listing not found")
        if (currentListing.farmerId != farmerId || listing.listingId != currentListing.listingId) {
            return Result.Error("Only the listing's Farmer can accept bids")
        }
        if (currentListing.isDraft || currentListing.status != com.agroroute.core.model.ListingStatus.OPEN ||
            !currentListing.biddingEnabled || currentListing.availableStock() <= 0.0
        ) {
            return Result.Error("This listing is not accepting bids")
        }

        val order = Order(
            orderId      = bid.bidId,
            listingId    = currentListing.listingId,
            bidId        = bid.bidId,
            farmerId     = currentListing.farmerId,
            supplierId   = bid.supplierId,
            cropName     = currentListing.cropName,
            quantity     = bid.requestedQuantity ?: currentListing.availableStock(),
            quantityUnit = currentListing.quantityUnit,
            agreedPrice  = bid.offerPrice,
            status       = OrderStatus.AWAITING_PICKUP,
            pickupLocation  = currentListing.location,
            deliveryLocation = bid.deliveryLocation
        )
        return orderRepository.acceptBidAndCreateOrder(order)
    }
}

class RejectBidUseCase @Inject constructor(
    private val bidRepository: BidRepository,
    private val listingRepository: HarvestListingRepository,
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(bid: Bid): Result<Unit> {
        val farmerId = authRepository.currentUserId
            ?: return Result.Error("Sign in as the listing's Farmer to reject bids")
        val profileResult = authRepository.getCurrentUserProfile().first()
        val profile = when (profileResult) {
            is Result.Success -> profileResult.data
            is Result.Error -> return profileResult
            Result.Loading -> return Result.Error("Unable to verify your account")
        }
        if (profile?.role != UserRole.FARMER || profile.userId != farmerId) {
            return Result.Error("Only Farmers can respond to incoming bids")
        }
        if (bid.status != BidStatus.PENDING) {
            return Result.Error("Only pending bids can be rejected")
        }
        val listingResult = listingRepository.getListingById(bid.listingId).first()
        val listing = when (listingResult) {
            is Result.Success -> listingResult.data
            is Result.Error -> return listingResult
            Result.Loading -> null
        } ?: return Result.Error("Listing not found")
        if (listing.farmerId != farmerId || listing.isDraft) {
            return Result.Error("Only the listing's Farmer can reject bids")
        }
        val storedBidResult = bidRepository.getBidById(bid.bidId).first()
        val storedBid = when (storedBidResult) {
            is Result.Success -> storedBidResult.data
            is Result.Error -> return storedBidResult
            Result.Loading -> null
        } ?: return Result.Error("Bid not found")
        if (storedBid.listingId != listing.listingId || storedBid.status != BidStatus.PENDING) {
            return Result.Error("Only pending bids on your listing can be rejected")
        }
        return bidRepository.updateBidStatus(storedBid.bidId, BidStatus.REJECTED)
    }
}
