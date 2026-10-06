package com.agroroute.core.usecase

import com.agroroute.core.model.Bid
import com.agroroute.core.model.BidStatus
import com.agroroute.core.repository.BidRepository
import com.agroroute.core.util.Result
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GetBidsBySupplierUseCaseTest {

    @Test
    fun returnsBidsForSupplier() = runTest {
        val expectedBids = listOf(
            Bid(bidId = "bid-1", supplierId = "supplier-1", status = BidStatus.PENDING),
            Bid(bidId = "bid-2", supplierId = "supplier-1", status = BidStatus.ACCEPTED)
        )
        val repository = FakeBidRepository(Result.Success(expectedBids))

        val useCase = GetBidsBySupplierUseCase(repository)
        val result = useCase("supplier-1")

        val firstResult = result.first()
        assertTrue(firstResult is Result.Success)
        assertEquals(expectedBids, (firstResult as Result.Success).data)
        assertEquals("supplier-1", repository.requestedSupplierId)
    }

    @Test
    fun propagatesRepositoryError() = runTest {
        val repository = FakeBidRepository(Result.Error("Network error"))

        val useCase = GetBidsBySupplierUseCase(repository)
        val result = useCase("supplier-1")

        val firstResult = result.first()
        assertTrue(firstResult is Result.Error)
        assertEquals("Network error", (firstResult as Result.Error).message)
    }

    private class FakeBidRepository(
        private val supplierResult: Result<List<Bid>>
    ) : BidRepository {
        var requestedSupplierId: String? = null
            private set

        override fun getBidsForListing(listingId: String) = flowOf(Result.Success(emptyList<Bid>()))

        override fun getBidsBySupplier(supplierId: String) = flowOf(supplierResult).also {
            requestedSupplierId = supplierId
        }

        override fun getBidById(bidId: String) = flowOf(Result.Success<Bid?>(null))

        override suspend fun placeBid(bid: Bid) = Result.Error("Not used by this test")

        override suspend fun updateBidStatus(bidId: String, status: BidStatus) =
            Result.Error("Not used by this test")

        override suspend fun withdrawBid(bidId: String) = Result.Error("Not used by this test")

        override suspend fun syncBidsForListing(listingId: String) = Result.Error("Not used by this test")
    }
}
