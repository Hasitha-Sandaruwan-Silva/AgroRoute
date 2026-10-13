package com.agroroute.features.supplier.home

import androidx.lifecycle.ViewModelStore
import com.agroroute.core.model.*
import com.agroroute.core.remote.NetworkConnectivityMonitor
import com.agroroute.core.repository.*
import com.agroroute.core.usecase.*
import com.agroroute.core.util.Result
import com.agroroute.features.supplier.bids.SupplierBidsViewModel
import com.agroroute.features.supplier.orders.SupplierOrderViewModel
import io.mockk.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
import org.junit.*

@OptIn(ExperimentalCoroutinesApi::class)
class SupplierHomeObserverTest {
    @Before fun main() { Dispatchers.setMain(UnconfinedTestDispatcher()) }
    @After fun reset() { Dispatchers.resetMain() }
    @Test fun bidsObserveOnceUntilAccountChanges() = runTest {
        val read = mockk<GetBidsBySupplierUseCase>()
        every { read(any()) } returns flow<Result<List<Bid>>> { emit(Result.Success(emptyList())); awaitCancellation() }
        val vm = SupplierBidsViewModel(read, mockk(), mockk(relaxed = true))
        val store = ViewModelStore().apply { put("bids", vm) }
        try {
            vm.loadBids("one"); vm.loadBids("one")
            verify(exactly = 1) { read("one") }
            vm.loadBids("two"); vm.loadBids("two")
            verify(exactly = 1) { read("two") }
            vm.loadBids("")
            verify(exactly = 0) { read("") }
        } finally { store.clear() }
    }
    @Test fun ordersAndRequestsDoNotDuplicateObserversOrInitialSync() = runTest {
        val read = mockk<GetOrdersUseCase>()
        val orders = mockk<OrderRepository>()
        val requests = mockk<OrderRequestRepository>()
        val network = mockk<NetworkConnectivityMonitor>()
        every { network.isOnline } returns MutableStateFlow(false)
        every { read.forSupplier(any()) } returns flow<Result<List<Order>>> { emit(Result.Success(emptyList())); awaitCancellation() }
        every { requests.getRequestsForSupplier(any()) } returns flow<Result<List<OrderRequest>>> { emit(Result.Success(emptyList())); awaitCancellation() }
        coEvery { orders.syncOrders(any()) } returns Result.Success(Unit)
        coEvery { requests.syncRequestsForUser(any()) } returns Result.Success(Unit)
        val vm = SupplierOrderViewModel(read, orders, requests, mockk(relaxed = true), network)
        val store = ViewModelStore().apply { put("orders", vm) }
        try {
            vm.loadOrdersForSupplier("one"); vm.loadOrdersForSupplier("one")
            verify(exactly = 1) { read.forSupplier("one") }
            verify(exactly = 1) { requests.getRequestsForSupplier("one") }
            coVerify(exactly = 1) { orders.syncOrders("one") }
            coVerify(exactly = 1) { requests.syncRequestsForUser("one") }
            vm.loadOrdersForSupplier("two")
            verify(exactly = 1) { read.forSupplier("two") }
        } finally { store.clear() }
    }
}
