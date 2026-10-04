package com.agroroute.features.supplier.bids

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.agroroute.core.model.Bid
import com.agroroute.core.model.BidStatus
import com.agroroute.core.repository.AuthRepository
import com.agroroute.core.util.Result
import com.agroroute.core.usecase.GetBidsBySupplierUseCase
import com.agroroute.core.usecase.WithdrawBidUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.any
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class SupplierBidsViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var viewModel: SupplierBidsViewModel
    private val getBidsBySupplierUseCase = mockk<GetBidsBySupplierUseCase>()
    private val withdrawBidUseCase = mockk<WithdrawBidUseCase>()
    private val authRepository = mockk<AuthRepository>()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        every { authRepository.currentUserId } returns "supplier-1"
        viewModel = SupplierBidsViewModel(getBidsBySupplierUseCase, withdrawBidUseCase, authRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun loadBids_emitsSuccess() = runTest {
        val bids = listOf(
            Bid(bidId = "bid-1", supplierId = "supplier-1", status = BidStatus.PENDING),
            Bid(bidId = "bid-2", supplierId = "supplier-1", status = BidStatus.ACCEPTED)
        )
        coEvery { getBidsBySupplierUseCase("supplier-1") } returns flowOf(Result.Success(bids))

        viewModel.loadBids("supplier-1")
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(bids, viewModel.uiState.value.bids)
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun loadBids_emitsError() = runTest {
        coEvery { getBidsBySupplierUseCase(any()) } returns flowOf(Result.Error("Network error"))

        viewModel.loadBids("supplier-1")
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("Network error", viewModel.uiState.value.error)
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun withdrawBid_succeeds() = runTest {
        coEvery { withdrawBidUseCase("bid-1") } returns Result.Success(Unit)

        viewModel.withdrawBid("bid-1")
        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isWithdrawing)
        assertTrue(viewModel.uiState.value.error == null)
        coVerify(exactly = 1) { withdrawBidUseCase("bid-1") }
    }

    @Test
    fun withdrawBid_handlesError() = runTest {
        coEvery { withdrawBidUseCase(any()) } returns Result.Error("Bid not found")

        viewModel.withdrawBid("bid-1")
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("Bid not found", viewModel.uiState.value.error)
        assertFalse(viewModel.uiState.value.isWithdrawing)
    }

    @Test
    fun clearError_doesNotCrash() = runTest {
        // Verify clearError can be called without crashing
        viewModel.clearError()
        // No assertion needed - we just want to ensure it doesn't throw
    }

    @Test
    fun currentSupplierId_returnsAuthRepositoryValue() {
        every { authRepository.currentUserId } returns "supplier-123"

        viewModel = SupplierBidsViewModel(getBidsBySupplierUseCase, withdrawBidUseCase, authRepository)

        assertEquals("supplier-123", viewModel.currentSupplierId)
    }
}
