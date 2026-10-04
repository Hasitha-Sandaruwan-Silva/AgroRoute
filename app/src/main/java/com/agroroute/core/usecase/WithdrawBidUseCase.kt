package com.agroroute.core.usecase

import com.agroroute.core.util.Result
import com.agroroute.core.repository.BidRepository
import javax.inject.Inject

/**
 * Withdraws a pending bid on behalf of a supplier.
 */
class WithdrawBidUseCase @Inject constructor(
    private val repository: BidRepository
) {
    suspend operator fun invoke(bidId: String): Result<Unit> =
        repository.withdrawBid(bidId)
}
