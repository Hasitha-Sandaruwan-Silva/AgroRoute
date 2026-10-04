package com.agroroute.core.usecase

import com.agroroute.core.repository.BidRepository
import com.agroroute.core.util.Result
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test

class WithdrawBidUseCaseTest {

    @Test
    fun withdrawsPendingBid() = runBlocking {
        val repository = mockk<BidRepository>()
        coEvery { repository.withdrawBid("bid-1") } returns Result.Success(Unit)

        val useCase = WithdrawBidUseCase(repository)
        val result = useCase("bid-1")

        assertTrue(result is Result.Success)
        coVerify(exactly = 1) { repository.withdrawBid("bid-1") }
    }

    @Test
    fun propagatesRepositoryError() = runBlocking {
        val repository = mockk<BidRepository>()
        coEvery { repository.withdrawBid("bid-1") } returns Result.Error("Bid not found")

        val useCase = WithdrawBidUseCase(repository)
        val result = useCase("bid-1")

        assertTrue(result is Result.Error)
        assertTrue((result as Result.Error).message == "Bid not found")
    }
}
