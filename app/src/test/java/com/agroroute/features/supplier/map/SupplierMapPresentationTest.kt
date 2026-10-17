package com.agroroute.features.supplier.map

import com.agroroute.core.location.SelectedLocation
import com.agroroute.core.location.orderMapPresentation
import com.agroroute.core.model.Order
import com.agroroute.core.model.OrderStatus
import org.junit.Assert.*
import org.junit.Test

class SupplierMapPresentationTest {
    private val pickup = SelectedLocation("Farm", 7.2, 80.1)
    private val delivery = SelectedLocation("Warehouse", 6.9, 79.8)
    private fun order(id: String = "one", status: OrderStatus = OrderStatus.AWAITING_PICKUP, owner: String = "supplier") =
        Order(orderId = id, supplierId = owner, status = status,
            pickupLocation = pickup.toStoredText(), deliveryLocation = delivery.toStoredText())

    @Test fun emptyAndUnauthenticatedExposeNothing() {
        assertTrue(supplierMapOrders(emptyList(), "supplier").isEmpty())
        assertTrue(supplierMapOrders(listOf(order(owner = "")), "").isEmpty())
    }
    @Test fun activeOwnershipAndStatusAreChecked() {
        val orders = listOf(order(), order("two"), order("delivery", OrderStatus.IN_TRANSIT),
            order("cancelled", OrderStatus.CANCELLED), order("done", OrderStatus.DELIVERED), order("other", owner = "other"))
        assertEquals(listOf("one", "two", "delivery"), supplierMapOrders(orders, "supplier").map { it.orderId })
    }
    @Test fun filtersOnlyChangePresentation() {
        val orders = listOf(order(), order("delivery", OrderStatus.IN_TRANSIT))
        assertEquals(listOf(orders.first()), supplierMapOrders(orders, "supplier", SupplierMapFilter.Pickup))
        assertEquals(listOf(orders.last()), supplierMapOrders(orders, "supplier", SupplierMapFilter.Delivery))
        assertEquals(2, supplierMapOrders(orders, "supplier").size)
    }
    @Test fun stagesReuseSavedDestinationsAndNavigationLabels() {
        val pickupView = orderMapPresentation(order(), true)
        val deliveryView = orderMapPresentation(order(status = OrderStatus.IN_TRANSIT), true)
        assertEquals(pickup, pickupView.destination)
        assertEquals(delivery, deliveryView.destination)
        assertEquals("Navigate to Farmer", pickupView.navigationLabel)
        assertEquals("Navigate to Delivery", deliveryView.navigationLabel)
    }
    @Test fun legacyAddressStaysAccessibleWithoutInventingCoordinates() {
        val legacy = order().copy(pickupLocation = "Old farm address")
        assertEquals(listOf(legacy), supplierMapOrders(listOf(legacy), "supplier"))
        assertNull(orderMapPresentation(legacy, true).destination)
        assertEquals("Old farm address", orderMapPresentation(legacy, true).address)
    }
}
