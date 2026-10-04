package com.agroroute.features.orders

import com.agroroute.core.model.Order
import com.agroroute.core.model.OrderStatus
import com.agroroute.core.model.UserRole
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OrderStatusTransitionTest {

    @Test
    fun farmerCanTransitionFromAwaitingPickupToInTransit() = runBlocking {
        val order = Order(
            orderId = "order-1",
            listingId = "listing-1",
            bidId = "bid-1",
            farmerId = "farmer-1",
            supplierId = "supplier-1",
            cropName = "Rice",
            quantity = 5.0,
            quantityUnit = com.agroroute.core.model.QuantityUnit.KG,
            agreedPrice = 8.0,
            status = OrderStatus.AWAITING_PICKUP,
            pickupLocation = "North Farm"
        )

        val canTransition = canFarmerTransitionTo(order, OrderStatus.IN_TRANSIT)

        assertTrue(canTransition)
    }

    @Test
    fun supplierCannotTransitionFromAwaitingPickupToInTransit() = runBlocking {
        val order = Order(
            orderId = "order-1",
            listingId = "listing-1",
            bidId = "bid-1",
            farmerId = "farmer-1",
            supplierId = "supplier-1",
            cropName = "Rice",
            quantity = 5.0,
            quantityUnit = com.agroroute.core.model.QuantityUnit.KG,
            agreedPrice = 8.0,
            status = OrderStatus.AWAITING_PICKUP,
            pickupLocation = "North Farm"
        )

        val canTransition = canSupplierTransitionTo(order, OrderStatus.IN_TRANSIT)

        assertTrue(!canTransition)
    }

    @Test
    fun supplierCanTransitionFromInTransitToDelivered() = runBlocking {
        val order = Order(
            orderId = "order-1",
            listingId = "listing-1",
            bidId = "bid-1",
            farmerId = "farmer-1",
            supplierId = "supplier-1",
            cropName = "Rice",
            quantity = 5.0,
            quantityUnit = com.agroroute.core.model.QuantityUnit.KG,
            agreedPrice = 8.0,
            status = OrderStatus.IN_TRANSIT,
            pickupLocation = "North Farm"
        )

        val canTransition = canSupplierTransitionTo(order, OrderStatus.DELIVERED)

        assertTrue(canTransition)
    }

    @Test
    fun farmerCannotTransitionFromInTransitToDelivered() = runBlocking {
        val order = Order(
            orderId = "order-1",
            listingId = "listing-1",
            bidId = "bid-1",
            farmerId = "farmer-1",
            supplierId = "supplier-1",
            cropName = "Rice",
            quantity = 5.0,
            quantityUnit = com.agroroute.core.model.QuantityUnit.KG,
            agreedPrice = 8.0,
            status = OrderStatus.IN_TRANSIT,
            pickupLocation = "North Farm"
        )

        val canTransition = canFarmerTransitionTo(order, OrderStatus.DELIVERED)

        assertTrue(!canTransition)
    }

    @Test
    fun farmerCanCancelFromAwaitingPickup() = runBlocking {
        val order = Order(
            orderId = "order-1",
            listingId = "listing-1",
            bidId = "bid-1",
            farmerId = "farmer-1",
            supplierId = "supplier-1",
            cropName = "Rice",
            quantity = 5.0,
            quantityUnit = com.agroroute.core.model.QuantityUnit.KG,
            agreedPrice = 8.0,
            status = OrderStatus.AWAITING_PICKUP,
            pickupLocation = "North Farm"
        )

        val canTransition = canFarmerTransitionTo(order, OrderStatus.CANCELLED)

        assertTrue(canTransition)
    }

    @Test
    fun supplierCanCancelFromAwaitingPickup() = runBlocking {
        val order = Order(
            orderId = "order-1",
            listingId = "listing-1",
            bidId = "bid-1",
            farmerId = "farmer-1",
            supplierId = "supplier-1",
            cropName = "Rice",
            quantity = 5.0,
            quantityUnit = com.agroroute.core.model.QuantityUnit.KG,
            agreedPrice = 8.0,
            status = OrderStatus.AWAITING_PICKUP,
            pickupLocation = "North Farm"
        )

        val canTransition = canSupplierTransitionTo(order, OrderStatus.CANCELLED)

        assertTrue(canTransition)
    }

    @Test
    fun farmerCanCancelFromInTransit() = runBlocking {
        val order = Order(
            orderId = "order-1",
            listingId = "listing-1",
            bidId = "bid-1",
            farmerId = "farmer-1",
            supplierId = "supplier-1",
            cropName = "Rice",
            quantity = 5.0,
            quantityUnit = com.agroroute.core.model.QuantityUnit.KG,
            agreedPrice = 8.0,
            status = OrderStatus.IN_TRANSIT,
            pickupLocation = "North Farm"
        )

        val canTransition = canFarmerTransitionTo(order, OrderStatus.CANCELLED)

        assertTrue(canTransition)
    }

    @Test
    fun supplierCanCancelFromInTransit() = runBlocking {
        val order = Order(
            orderId = "order-1",
            listingId = "listing-1",
            bidId = "bid-1",
            farmerId = "farmer-1",
            supplierId = "supplier-1",
            cropName = "Rice",
            quantity = 5.0,
            quantityUnit = com.agroroute.core.model.QuantityUnit.KG,
            agreedPrice = 8.0,
            status = OrderStatus.IN_TRANSIT,
            pickupLocation = "North Farm"
        )

        val canTransition = canSupplierTransitionTo(order, OrderStatus.CANCELLED)

        assertTrue(canTransition)
    }

    @Test
    fun cannotCancelAfterDelivered() = runBlocking {
        val order = Order(
            orderId = "order-1",
            listingId = "listing-1",
            bidId = "bid-1",
            farmerId = "farmer-1",
            supplierId = "supplier-1",
            cropName = "Rice",
            quantity = 5.0,
            quantityUnit = com.agroroute.core.model.QuantityUnit.KG,
            agreedPrice = 8.0,
            status = OrderStatus.DELIVERED,
            pickupLocation = "North Farm"
        )

        val farmerCanCancel = canFarmerTransitionTo(order, OrderStatus.CANCELLED)
        val supplierCanCancel = canSupplierTransitionTo(order, OrderStatus.CANCELLED)

        assertTrue(!farmerCanCancel)
        assertTrue(!supplierCanCancel)
    }

    @Test
    fun cannotSkipFromAwaitingPickupToDelivered() = runBlocking {
        val order = Order(
            orderId = "order-1",
            listingId = "listing-1",
            bidId = "bid-1",
            farmerId = "farmer-1",
            supplierId = "supplier-1",
            cropName = "Rice",
            quantity = 5.0,
            quantityUnit = com.agroroute.core.model.QuantityUnit.KG,
            agreedPrice = 8.0,
            status = OrderStatus.AWAITING_PICKUP,
            pickupLocation = "North Farm"
        )

        val farmerCanSkip = canFarmerTransitionTo(order, OrderStatus.DELIVERED)
        val supplierCanSkip = canSupplierTransitionTo(order, OrderStatus.DELIVERED)

        assertTrue(!farmerCanSkip)
        assertTrue(!supplierCanSkip)
    }

    private fun canFarmerTransitionTo(order: Order, targetStatus: OrderStatus): Boolean {
        return when (order.status) {
            OrderStatus.AWAITING_PICKUP -> targetStatus == OrderStatus.IN_TRANSIT || targetStatus == OrderStatus.CANCELLED
            OrderStatus.IN_TRANSIT -> targetStatus == OrderStatus.CANCELLED
            else -> false
        }
    }

    private fun canSupplierTransitionTo(order: Order, targetStatus: OrderStatus): Boolean {
        return when (order.status) {
            OrderStatus.AWAITING_PICKUP -> targetStatus == OrderStatus.CANCELLED
            OrderStatus.IN_TRANSIT -> targetStatus == OrderStatus.DELIVERED || targetStatus == OrderStatus.CANCELLED
            else -> false
        }
    }
}
