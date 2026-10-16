package com.agroroute.features.farmer.home

import com.agroroute.core.model.*
import com.agroroute.features.farmer.orderrequests.FarmerOrderRequestsUiState
import org.junit.Assert.*
import org.junit.Test

class IncomingOrderRequestsTest {
    private val listing = HarvestListing(listingId = "l", farmerId = "f", status = ListingStatus.OPEN,
        quantity = 100.0, availableQuantity = 100.0, biddingEnabled = false)
    private fun request(id: String = "r", time: Long = 1) = OrderRequest(requestId = id, listingId = "l", farmerId = "f", createdAt = time)
    @Test fun emptyAndSinglePendingUseActualRequests() {
        assertTrue(pendingHomeRequests(FarmerOrderRequestsUiState(), "f", 100).isEmpty())
        val state = FarmerOrderRequestsUiState(requests = listOf(request()), listingsById = mapOf("l" to listing))
        assertEquals(listOf("r"), pendingHomeRequests(state, "f", 100).map { it.requestId })
    }
    @Test fun countExcludesAllNonpendingAndExpiredListingsAndOtherFarmers() {
        val requests = OrderRequestStatus.entries.map { request(it.name).copy(status = it) } +
            request("other").copy(farmerId = "other") + request("missing").copy(listingId = "missing")
        val state = FarmerOrderRequestsUiState(requests = requests, listingsById = mapOf("l" to listing))
        assertEquals(listOf("PENDING"), pendingHomeRequests(state, "f", 100).map { it.requestId })
        assertTrue(pendingHomeRequests(state.copy(listingsById = mapOf("l" to listing.copy(expiresAt = 100))), "f", 100).isEmpty())
    }
    @Test fun latestAreFirstButCountIncludesAllPendingRequests() {
        val state = FarmerOrderRequestsUiState(requests = (1..4).map { request("r$it", it.toLong()) }, listingsById = mapOf("l" to listing))
        val pending = pendingHomeRequests(state, "f", 100)
        assertEquals(4, pending.size)
        assertEquals(listOf("r4", "r3", "r2"), pending.take(3).map { it.requestId })
    }
    @Test fun closedDraftAndFailedHarvestsCannotAppearAsActivePending() {
        for (changed in listOf(listing.copy(status = ListingStatus.FULFILLED), listing.copy(isDraft = true),
            listing.copy(harvestStatus = HarvestStatus.FAILED), listing.copy(farmerId = "other"))) {
            val state = FarmerOrderRequestsUiState(requests = listOf(request()), listingsById = mapOf("l" to changed))
            assertTrue(pendingHomeRequests(state, "f", 100).isEmpty())
        }
    }
    @Test fun insufficientStockRemainsReviewableWithoutInventingAnAcceptancePath() {
        val state = FarmerOrderRequestsUiState(requests = listOf(request().copy(requestedQuantity = 200.0)), listingsById = mapOf("l" to listing))
        assertEquals(1, pendingHomeRequests(state, "f", 100).size)
        assertNotNull(com.agroroute.core.components.listingAcceptanceBlockedReason(listing, false, 200.0, ""))
    }
}
