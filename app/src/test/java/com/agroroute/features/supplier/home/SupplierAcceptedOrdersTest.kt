package com.agroroute.features.supplier.home

import com.agroroute.core.model.*
import org.junit.Assert.*
import org.junit.Test

class SupplierAcceptedOrdersTest {
    private val order = Order(orderId = "order", supplierId = "supplier", farmerId = "farmer", listingId = "listing")

    @Test fun onlyCurrentSuppliersConfirmedActiveOrdersAreIncluded() {
        val rows = listOf(order, order.copy(orderId = "transit", status = OrderStatus.IN_TRANSIT),
            order.copy(orderId = "delivered", status = OrderStatus.DELIVERED),
            order.copy(orderId = "cancelled", status = OrderStatus.CANCELLED),
            order.copy(orderId = "other", supplierId = "another-supplier"), order.copy(orderId = ""))
        assertEquals(setOf("order", "transit"), homeAcceptedOrders(rows, "supplier").map { it.orderId }.toSet())
        assertTrue(homeAcceptedOrders(rows, "").isEmpty())
    }

    @Test fun latestUpdatesLeadAndTerminalStatusRemovesThePreview() {
        val older = order.copy(orderId = "older", createdAt = 10, updatedAt = 20)
        val newer = order.copy(orderId = "newer", createdAt = 30, updatedAt = 40)
        assertEquals(listOf("newer", "older"), homeAcceptedOrders(listOf(older, newer, newer), "supplier").map { it.orderId })
        assertEquals(listOf(older), homeAcceptedOrders(listOf(older, newer.copy(status = OrderStatus.DELIVERED)), "supplier"))
    }

    @Test fun farmerNameUsesExistingMatchedSourcesWithoutLeakingIds() {
        val listing = HarvestListing(listingId = "listing", farmerId = "farmer", farmerName = "Nimal")
        val request = OrderRequest(requestId = "order", supplierId = "supplier", farmerId = "farmer",
            farmerName = "Kamal", status = OrderRequestStatus.ACCEPTED)
        assertEquals("Kamal", homeOrderFarmer(order, listOf(listing), listOf(request)))
        assertEquals("Nimal", homeOrderFarmer(order, listOf(listing), listOf(request.copy(supplierId = "other"))))
        assertEquals("Name unavailable", homeOrderFarmer(order, listOf(listing.copy(farmerId = "other")), emptyList()))
    }
}
