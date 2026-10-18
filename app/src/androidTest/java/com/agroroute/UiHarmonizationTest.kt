package com.agroroute

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.agroroute.core.components.*
import com.agroroute.core.model.*
import com.agroroute.core.notification.NotificationInboxItem
import com.agroroute.core.theme.AgroRouteTheme
import com.agroroute.features.farmer.orders.ModernOrderCard
import com.agroroute.features.settings.NotificationRow
import com.github.thelacspace.icons.Lucide
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class UiHarmonizationTest(private val dark: Boolean) {
    @get:Rule val compose = createComposeRule()
    companion object {
        @JvmStatic @Parameterized.Parameters fun themes() = listOf(false, true)
    }
    private fun capture(name: String) {
        val context = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().targetContext
        java.io.File(context.cacheDir, "harmonized-$name-$dark.png").outputStream().use {
            compose.onRoot().captureToImage().asAndroidBitmap().compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it)
        }
    }
    @Test fun orderCardsKeepStatusesAndNavigationWithLongNames() {
        var opened = 0
        compose.setContent { AgroRouteTheme(darkTheme = dark) {
            androidx.compose.material3.Surface { Box(Modifier.fillMaxSize().systemBarsPadding()) {
            Column(Modifier.width(320.dp).verticalScroll(rememberScrollState())) {
                OrderStatus.entries.forEach { status ->
                    ModernOrderCard(Order(orderId = status.name, cropName = "Fresh harvest tomatoes with a long produce name",
                        quantity = 40.0, agreedPrice = 120.0, status = status),
                        "Kasun Perera with a long supplier name") { opened++ }
                }
            }
        } } } }
        capture("orders")
        compose.onAllNodesWithText("View order").onFirst().performScrollTo().performClick()
        compose.runOnIdle { assertEquals(1, opened) }
        compose.onAllNodesWithText("View order").onLast().performScrollTo().assertIsDisplayed()
        compose.onAllNodesWithText("Supplier \u00B7 Kasun Perera with a long supplier name").onLast().performScrollTo().assertIsDisplayed()
    }
    @Test fun unreadMessageAndRequestPreserveTapAndMarkReadCallbacks() {
        var opened = false
        var read by mutableStateOf(false)
        val item = NotificationInboxItem("fixture", "CHAT_MESSAGE", "New message from Kasun",
            "A long message preview ".repeat(30), "existing-route", 1L, null)
        compose.setContent { AgroRouteTheme(darkTheme = dark) {
            androidx.compose.material3.Surface { Box(Modifier.fillMaxSize().systemBarsPadding()) {
            Column(Modifier.width(320.dp).verticalScroll(rememberScrollState())) {
                NotificationRow(item.copy(readAt = if (read) 2L else null), { opened = true }, markRead = { read = true })
                NotificationRow(item.copy(id = "request", eventType = "ORDER_REQUEST_CREATED", title = "New order request", readAt = 2L), {})
            }
        } } } }
        capture("notifications")
        compose.onNodeWithContentDescription("Unread").assertExists()
        compose.onNodeWithText("New message from Kasun").performClick()
        compose.runOnIdle { assertTrue(opened) }
        compose.onNodeWithText("Mark read").performScrollTo().performClick()
        compose.onNodeWithContentDescription("Unread").assertDoesNotExist()
        compose.onNodeWithText("New order request").performScrollTo().assertIsDisplayed()
    }
    @Test fun rationaleDefersSystemRequestUntilEnableIsTapped() {
        var enabled = 0
        var dismissed = 0
        compose.setContent { AgroRouteTheme(darkTheme = dark) {
            androidx.compose.material3.Surface { Box(Modifier.fillMaxSize().systemBarsPadding()) {
            com.agroroute.features.settings.NotificationRationale({ enabled++ }, { dismissed++ })
        } } } }
        compose.runOnIdle { assertEquals(0, enabled) }
        compose.onNodeWithText("Maybe later").performClick()
        compose.runOnIdle { assertEquals(1, dismissed); assertEquals(0, enabled) }
        compose.onNodeWithText("Enable notifications").performClick()
        compose.runOnIdle { assertEquals(1, enabled) }
    }
    @Test fun navigationRapidTapsKeepFinalSelectedState() {
        val items = listOf(BottomNavItem("home", "Home", Lucide.Leaf), BottomNavItem("orders", "Orders", Lucide.Receipt))
        var route by mutableStateOf("home")
        compose.setContent { AgroRouteTheme(darkTheme = dark) {
            androidx.compose.material3.Surface { Box(Modifier.fillMaxSize().systemBarsPadding()) {
            Box(Modifier.width(320.dp)) { AnimatedBottomBar(items, route, { route = it.route }) }
        } } } }
        repeat(3) {
            compose.onNodeWithText("Orders").performClick()
            compose.onNodeWithText("Home").performClick()
        }
        compose.onNodeWithText("Orders").performClick()
        compose.onNode(isSelected()).assertTextContains("Orders")
        capture("navigation")
    }

    @Test fun compactNavigationKeepsLargeFontLabelsInsideTheirTabs() {
        val items = listOf(BottomNavItem("home", "Home", Lucide.Leaf), BottomNavItem("orders", "Orders", Lucide.Receipt))
        compose.setContent {
            val density = androidx.compose.ui.platform.LocalDensity.current
            CompositionLocalProvider(androidx.compose.ui.platform.LocalDensity provides
                androidx.compose.ui.unit.Density(density.density, 2f)) {
                AgroRouteTheme(darkTheme = dark) {
                    Box(Modifier.width(320.dp)) { AnimatedBottomBar(items, "home", {}) }
                }
            }
        }
        val tab = compose.onNode(isSelected()).fetchSemanticsNode().boundsInRoot
        val label = compose.onNodeWithText("Home", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        assertTrue("Large label must fit vertically inside the tab", label.top >= tab.top && label.bottom <= tab.bottom)
        compose.onNodeWithText("Orders").assertIsDisplayed().performClick()
        capture("navigation-large-font")
    }
}
