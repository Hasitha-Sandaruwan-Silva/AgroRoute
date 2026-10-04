package com.agroroute.core.usecase

import com.agroroute.core.model.Bid
import com.agroroute.core.util.Result
import com.agroroute.core.repository.BidRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Retrieves all bids placed by a specific supplier.
 */
class GetBidsBySupplierUseCase @Inject constructor(
    private val repository: BidRepository
) {
    operator fun invoke(supplierId: String): Flow<Result<List<Bid>>> =
        repository.getBidsBySupplier(supplierId)
}
