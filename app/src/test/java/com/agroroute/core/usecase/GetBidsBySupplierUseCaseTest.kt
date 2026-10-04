package com.agroroute.core.usecase

import com.agroroute.core.model.Bid
import com.agroroute.core.model.BidStatus
import com.agroroute.core.repository.BidRepository
import com.agroroute.core.util.Result
import io.mockk.every
import io.mockk.mock
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GetBidsBySupplierUseCaseTest {

    @Test
    fun returnsBidsForSupplier() = runBlocking {
        val repository = mock<BidRepository>()
        val expectedBids = listOf(
            Bid(bidId = "bid-1", supplierId = "supplier-1", status = BidStatus.PENDING),
            Bid(bidId = "bid-2", supplierId = "supplier-1", status = BidStatus.ACCEPTED)
        )
        every { repository.getBidsBySupplier("supplier-1") } returns flowOf(Result.Success(expectedBids))

        val useCase = GetBidsBySupplierUseCase(repository)
        val result = useCase("supplier-1")

        // Collect the first emission
        val firstResult = runBlocking { result.first() }
        assertTrue(firstResult is Result.Success)
        assertEquals(expectedBids, (firstResult as Result.Success).data)
    }

    @Test
    fun propagatesRepositoryError() = runBlocking {
        val repository = mock<BidRepository>()
        every { repository.getBidsBySupplier(any()) } returns flowOf(Result.Error("Network error"))

        val useCase = GetBidsBySupplierUseCase(repository)
        val result = useCase("supplier-1")

        val firstResult = runBlocking { result.first() }
        assertTrue(firstResult is Result.Error)
        assertEquals("Network error", (firstResult as Result.Error).message)
    }
}
