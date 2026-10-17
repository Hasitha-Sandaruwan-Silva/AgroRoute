package com.agroroute

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.agroroute.core.location.SelectedLocation
import com.agroroute.core.model.Order
import com.agroroute.core.model.OrderStatus
import com.agroroute.core.theme.AgroRouteTheme
import com.agroroute.features.supplier.map.SupplierMapContent
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class SupplierMapPresentationTest(private val dark: Boolean, private val width: Int) {
    @get:Rule val compose = createComposeRule()
    companion object {
        @JvmStatic @Parameterized.Parameters(name = "dark={0},width={1}")
        fun configurations() = listOf(arrayOf(false, 320), arrayOf(true, 320), arrayOf(false, 400), arrayOf(true, 400))
    }
    private val pickup = Order(orderId = "pickup", supplierId = "supplier", cropName = "Tomatoes",
        pickupLocation = SelectedLocation("Farm", 7.2, 80.1).toStoredText())
    private val delivery = pickup.copy(orderId = "delivery", cropName = "Pumpkins", status = OrderStatus.IN_TRANSIT,
        deliveryLocation = SelectedLocation("Warehouse", 6.9, 79.8).toStoredText())
    private var opened: String? = null
    private fun content(orders: List<Order>) {
        compose.setContent { AgroRouteTheme(darkTheme = dark) {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, if (width == 320) 1.3f else 1f)) {
            Box(Modifier.width(width.dp)) {
                SupplierMapContent(orders, "supplier", farmerName = { "Kamal" }, onDetails = { opened = it.orderId })
            }
            }
        } }
    }
    @Test fun emptyMapHasCompactMessage() {
        content(emptyList())
        compose.onNodeWithText("0 Active Orders").assertIsDisplayed()
        compose.onNodeWithText("No active orders on the map yet.").assertIsDisplayed()
        compose.onNodeWithContentDescription("Current location").assertIsDisplayed()
    }
    @Test fun pickupAndDeliveryPreviewsOpenExistingDetailsCallback() {
        content(listOf(pickup, delivery, pickup.copy(orderId = "foreign", supplierId = "other"),
            pickup.copy(orderId = "done", status = OrderStatus.DELIVERED)))
        compose.onNodeWithText("2 Active Orders").assertIsDisplayed()
        compose.onNodeWithText("Tomatoes").performClick()
        compose.onNodeWithText("Farmer: Kamal").assertIsDisplayed()
        compose.onNodeWithText("View Pickup").performScrollTo().performClick()
        compose.runOnIdle { assertEquals("pickup", opened) }
        compose.onNodeWithText("Close").performClick()
        compose.onNodeWithText("Delivery", substring = false).performClick()
        compose.onNodeWithText("Tomatoes").assertDoesNotExist()
        compose.onNodeWithText("Pumpkins").performClick()
        compose.onNodeWithText("View Delivery").performScrollTo().performClick()
        compose.runOnIdle { assertEquals("delivery", opened) }
    }
    @Test fun missingCoordinatesStillOfferDetails() {
        content(listOf(pickup.copy(pickupLocation = "Legacy address")))
        compose.onNodeWithText("Tomatoes · Location unavailable").performClick()
        compose.onNodeWithText("Legacy address").assertIsDisplayed()
        compose.onNodeWithText("Navigate").assertIsNotEnabled()
        compose.onNodeWithText("Order Details").performScrollTo().performClick()
        compose.runOnIdle { assertEquals("pickup", opened) }
    }
}
