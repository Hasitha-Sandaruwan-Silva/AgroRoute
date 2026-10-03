package com.agroroute.features.farmer.orderrequests

import com.agroroute.core.model.OrderRequest
import com.agroroute.core.model.OrderRequestStatus
import com.agroroute.core.model.UserRole
import com.agroroute.core.repository.AuthRepository
import com.agroroute.core.repository.HarvestListingRepository
import com.agroroute.core.repository.OrderRepository
import com.agroroute.core.repository.OrderRequestRepository
import com.agroroute.core.util.Result
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * Orchestrates the order-request acceptance flow:
 * Delegates the stock update, order creation, and request acceptance to the repository transaction.
 */
class AcceptOrderRequestUseCase @Inject constructor(
    private val orderRepository: OrderRepository,
    private val orderRequestRepository: OrderRequestRepository,
    private val listingRepository: HarvestListingRepository,
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(request: OrderRequest): Result<Unit> {
        val farmerId = authRepository.currentUserId
            ?: return Result.Error("Sign in as the listing's Farmer to accept order requests")
        val profileResult = authRepository.getCurrentUserProfile().first()
        val profile = when (profileResult) {
            is Result.Success -> profileResult.data
            is Result.Error -> return profileResult
            Result.Loading -> return Result.Error("Unable to verify your account")
        }
        if (profile?.role != UserRole.FARMER || profile.userId != farmerId) {
            return Result.Error("Only the listing's Farmer can accept order requests")
        }
        if (request.farmerId != farmerId) {
            return Result.Error("Only the listing's Farmer can accept this order request")
        }
        if (request.status != OrderRequestStatus.PENDING) {
            return Result.Error("Only pending order requests can be accepted")
        }

        val listingResult = listingRepository.getListingById(request.listingId).first()
        val listing = when (listingResult) {
            is Result.Success -> listingResult.data
            is Result.Error -> return listingResult
            Result.Loading -> null
        } ?: return Result.Error("Listing not found")
        if (listing.farmerId != farmerId) {
            return Result.Error("Only the listing's Farmer can accept this order request")
        }

        return orderRepository.acceptOrderRequestAndCreateOrder(request.requestId)
    }
}

class RejectOrderRequestUseCase @Inject constructor(
    private val orderRequestRepository: OrderRequestRepository,
    private val listingRepository: HarvestListingRepository,
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(request: OrderRequest): Result<Unit> {
        val farmerId = authRepository.currentUserId
            ?: return Result.Error("Sign in as the listing's Farmer to reject order requests")
        val profileResult = authRepository.getCurrentUserProfile().first()
        val profile = when (profileResult) {
            is Result.Success -> profileResult.data
            is Result.Error -> return profileResult
            Result.Loading -> return Result.Error("Unable to verify your account")
        }
        if (profile?.role != UserRole.FARMER || profile.userId != farmerId) {
            return Result.Error("Only Farmers can respond to incoming order requests")
        }
        if (request.status != OrderRequestStatus.PENDING) {
            return Result.Error("Only pending order requests can be rejected")
        }
        if (request.farmerId != farmerId) {
            return Result.Error("Only the listing's Farmer can reject this order request")
        }
        val listingResult = listingRepository.getListingById(request.listingId).first()
        val listing = when (listingResult) {
            is Result.Success -> listingResult.data
            is Result.Error -> return listingResult
            Result.Loading -> null
        } ?: return Result.Error("Listing not found")
        if (listing.farmerId != farmerId || listing.isDraft) {
            return Result.Error("Only the listing's Farmer can reject this order request")
        }

        return orderRequestRepository.updateRequestStatus(request.requestId, OrderRequestStatus.REJECTED)
    }
}
