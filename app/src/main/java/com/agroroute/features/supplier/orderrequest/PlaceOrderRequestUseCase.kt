package com.agroroute.features.supplier.orderrequest

import com.agroroute.core.model.HarvestListing
import com.agroroute.core.model.OrderRequest
import com.agroroute.core.model.OrderRequestStatus
import com.agroroute.core.model.UserRole
import com.agroroute.core.model.acceptsDirectOrders
import com.agroroute.core.model.availableStock
import com.agroroute.core.util.Result
import com.agroroute.core.repository.AuthRepository
import com.agroroute.core.repository.HarvestListingRepository
import com.agroroute.core.repository.OrderRequestRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * Places an [OrderRequest] against a fixed-price listing, with pre-flight validation.
 */
class PlaceOrderRequestUseCase @Inject constructor(
    private val orderRequestRepository: OrderRequestRepository,
    private val listingRepository: HarvestListingRepository,
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(request: OrderRequest): Result<Unit> {
        if (request.requestedQuantity <= 0) return Result.Error("Requested quantity must be greater than zero")
        if (request.supplierId.isBlank()) return Result.Error("Supplier ID is required")
        if (request.listingId.isBlank()) return Result.Error("Listing ID is required")
        val supplierId = authRepository.currentUserId
            ?: return Result.Error("Sign in as a Supplier to place order requests")

        val profileResult = authRepository.getCurrentUserProfile().first()
        val currentRole = when (profileResult) {
            is Result.Success -> profileResult.data?.role
            is Result.Error -> return profileResult
            Result.Loading -> null
        }
        val profile = profileResult.getOrNull()
        if (currentRole != UserRole.SUPPLIER || profile?.userId != supplierId || request.supplierId != supplierId) {
            return Result.Error("Only Suppliers can place order requests")
        }

        val listingResult = listingRepository.getListingById(request.listingId).first()
        val listing = when (listingResult) {
            is Result.Success -> listingResult.data
            is Result.Error -> return listingResult
            Result.Loading -> null
        } ?: return Result.Error("Listing is no longer available")
        if (!listing.acceptsDirectOrders()) {
            return Result.Error("This listing is not available for direct orders")
        }
        if (request.requestedQuantity > listing.availableStock()) {
            return Result.Error("Requested quantity exceeds available stock")
        }

        return orderRequestRepository.placeOrderRequest(
            request.copy(
                farmerId = listing.farmerId,
                farmerName = listing.farmerName,
                cropName = listing.cropName,
                quantityUnit = listing.quantityUnit,
                listedPrice = listing.reservePrice,
                status = OrderRequestStatus.PENDING
            )
        )
    }
}
