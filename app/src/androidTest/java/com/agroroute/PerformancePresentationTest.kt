package com.agroroute

import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.agroroute.core.model.Order
import com.agroroute.core.model.OrderStatus
import com.agroroute.core.theme.AgroRouteTheme
import com.agroroute.features.supplier.map.SupplierMapContent
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class PerformancePresentationTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Before fun keepFixtureVisible() {
        compose.runOnUiThread {
            compose.activity.setShowWhenLocked(true)
            compose.activity.setTurnScreenOn(true)
            compose.activity.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    /** Count source-list traversal without adding instrumentation to shipping code. */
    @Test fun mapSelectionDoesNotRescanOrdersAndChangedOrdersStillInvalidateProjection() {
        val original = CountingOrders(listOf(Order(orderId = "one", supplierId = "supplier",
            cropName = "Tomatoes", pickupLocation = "Saved farm address")))
        var records: List<Order> by mutableStateOf(original)
        compose.setContent {
            AgroRouteTheme {
                SupplierMapContent(records, "supplier", onDetails = {})
            }
        }
        compose.onNodeWithText("1 Active Orders").assertIsDisplayed()
        compose.runOnIdle { assertTrue(original.reads > 0); original.reads = 0 }
        compose.onNodeWithText("Tomatoes", substring = true).performClick()
        compose.onNodeWithText("Saved farm address").assertIsDisplayed()
        compose.onNodeWithText("Close").performClick()
        compose.runOnIdle { assertEquals("Selection must reuse the order projection", 0, original.reads) }
        compose.runOnIdle {
            records = listOf(Order(orderId = "two", supplierId = "supplier", cropName = "Pumpkins",
                status = OrderStatus.IN_TRANSIT, deliveryLocation = "Saved delivery address"))
        }
        compose.onNodeWithText("Tomatoes", substring = true).assertDoesNotExist()
        compose.onNodeWithText("Pumpkins", substring = true).performClick()
        compose.onNodeWithText("Saved delivery address").assertIsDisplayed()
        compose.onNodeWithText("View Delivery").assertExists()
    }

    private class CountingOrders(private val values: List<Order>) : AbstractList<Order>() {
        var reads = 0
        override val size get() = values.size
        override fun get(index: Int): Order { reads++; return values[index] }
    }
}
