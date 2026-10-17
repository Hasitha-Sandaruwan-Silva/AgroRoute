package com.agroroute

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.*
import androidx.test.platform.app.InstrumentationRegistry
import com.agroroute.core.model.*
import com.agroroute.core.navigation.Screen
import com.agroroute.core.theme.AgroRouteTheme
import com.agroroute.features.supplier.home.*
import org.junit.*
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.io.File

@RunWith(Parameterized::class)
class SupplierAcceptedOrdersPresentationTest(private val dark: Boolean, private val compact: Boolean) {
    @get:Rule val compose = createComposeRule()
    companion object {
        @JvmStatic @Parameterized.Parameters(name = "dark={0},compact={1}")
        fun variants() = listOf(arrayOf(false, false), arrayOf(true, false), arrayOf(false, true), arrayOf(true, true))
    }
    private val now = 1_800_000_000_000L
    private val pickup = Order(orderId = "pickup", listingId = "listing", supplierId = "supplier", farmerId = "farmer",
        cropName = "Maha rice", quantity = 125.0, agreedPrice = 320.0, pickupLocation = "Kurunegala\n[7.4,80.3]",
        createdAt = now - 60_000, updatedAt = now - 30_000)
    private val transit = pickup.copy(orderId = "transit", cropName = "Banana", status = OrderStatus.IN_TRANSIT,
        updatedAt = now - 60_000)
    private val rows = mutableStateOf<List<Order>>(emptyList())
    private fun content() {
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, if (compact) 1.2f else 1f)) {
                AgroRouteTheme(darkTheme = dark) {
                    val nav = rememberNavController()
                    Box(Modifier.width(if (compact) 320.dp else 400.dp).fillMaxHeight()) {
                        NavHost(nav, startDestination = "home") {
                            composable("home") {
                                SupplierHomeContent(user = UserProfile(userId = "supplier", role = UserRole.SUPPLIER),
                                    listings = listOf(HarvestListing(listingId = "listing", farmerId = "farmer", farmerName = "Nimal")),
                                    bids = emptyList(), orders = rows.value, now = now,
                                    onMarketplace = {}, onOrders = { nav.navigate(Screen.SupplierOrders.route) }, onBids = {},
                                    onProfile = {}, onCommunity = {}, onListing = {}, notification = {},
                                    onOrder = { nav.navigate(Screen.SupplierOrderDetail.createRoute(it.orderId)) })
                            }
                            composable(Screen.SupplierOrderDetail.route) { entry ->
                                Column { Text("Order detail: ${entry.arguments?.getString("orderId")}"); TextButton({ nav.popBackStack() }) { Text("Back to Home") } }
                            }
                            composable(Screen.SupplierOrders.route) { Text("Existing Supplier Orders") }
                        }
                    }
                }
            }
        }
    }
    private fun scrollSection() {
        compose.onNodeWithTag("supplier-home").performScrollToNode(hasTestTag("accepted-orders-view-all"))
    }
    @Test fun emptyOneMultipleAndStatusUpdatesUseExistingHomeState() {
        content()
        compose.onNodeWithTag("accepted-orders").assertDoesNotExist()
        compose.runOnIdle { rows.value = listOf(pickup) }
        scrollSection()
        compose.onNodeWithTag("accepted-order-count").assertTextEquals("1")
        compose.onNodeWithText("Farmer: Nimal", useUnmergedTree = true).assertExists()
        compose.onNodeWithText("125 kg", useUnmergedTree = true).assertExists()
        compose.onNodeWithText("Rs. 320 / kg", useUnmergedTree = true).assertExists()
        compose.runOnIdle { rows.value = listOf(pickup, transit, pickup.copy(orderId = "third", updatedAt = now - 120_000),
            pickup.copy(orderId = "delivered", status = OrderStatus.DELIVERED), pickup.copy(orderId = "foreign", supplierId = "other")) }
        scrollSection()
        compose.onNodeWithTag("accepted-order-count").assertTextEquals("3")
        compose.onNodeWithTag("accepted-order-third").assertDoesNotExist()
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        File(context.cacheDir, "accepted-orders-$dark-$compact.png").outputStream().use {
            compose.onRoot().captureToImage().asAndroidBitmap().compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it)
        }
        compose.runOnIdle { rows.value = listOf(pickup.copy(status = OrderStatus.IN_TRANSIT)) }
        scrollSection()
        compose.onNodeWithText("View Delivery", useUnmergedTree = true).assertExists()
        compose.onNodeWithText("View Pickup Details", useUnmergedTree = true).assertDoesNotExist()
        compose.runOnIdle { rows.value = listOf(pickup.copy(status = OrderStatus.DELIVERED), transit.copy(status = OrderStatus.CANCELLED)) }
        compose.onNodeWithTag("accepted-orders").assertDoesNotExist()
    }
    @Test fun previewAndViewAllNavigateThroughExistingRoutes() {
        rows.value = listOf(pickup)
        content(); scrollSection()
        compose.onNodeWithTag("accepted-order-pickup").performClick()
        compose.onNodeWithText("Order detail: pickup").assertIsDisplayed()
        compose.onNodeWithText("Back to Home").performClick()
        scrollSection()
        compose.onNodeWithTag("accepted-orders-view-all").performClick()
        compose.onNodeWithText("Existing Supplier Orders").assertIsDisplayed()
    }
}
