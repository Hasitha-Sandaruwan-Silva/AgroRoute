package com.agroroute

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.agroroute.core.components.*
import com.agroroute.core.model.*
import com.agroroute.core.runtime.LocalMutationAllowed
import com.agroroute.core.theme.AgroRouteTheme
import com.agroroute.features.farmer.components.*
import com.agroroute.features.farmer.orderrequests.FarmerOrderRequestCard
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.*

class FarmerScreensPresentationTest {
    @get:Rule val compose = createComposeRule()
    private val listing = HarvestListing(listingId = "crop", cropName = "Tomato",
        quantity = 100.0, soldQuantity = 40.0, reservePrice = 320.0,
        location = "6.9271,79.8612", biddingEnabled = false)
    private fun content(dark: Boolean = false, mutation: Boolean = true, body: @Composable () -> Unit) {
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, 1.6f),
                LocalMutationAllowed provides mutation) {
                AgroRouteTheme(darkTheme = dark) { FarmerPresentation {
                    Column(Modifier.width(320.dp).fillMaxHeight().verticalScroll(rememberScrollState())) { body() }
                } }
            }
        }
    }
    @Test fun insufficientStockRequestKeepsAcceptDisabled() {
        var accepted = false
        content { FarmerOrderRequestCard(OrderRequest(cropName = "Tomato", requestedQuantity = 70.0,
            deliveryLocation = "6.9271,79.8612"), listing, false, { accepted = true }, {}) }
        compose.onNodeWithText("Accept", substring = false).performScrollTo().assertIsNotEnabled()
        compose.runOnIdle { assertFalse(accepted) }
    }
    @Test fun demoRequestCannotBeAcceptedOrRejectedInDarkMode() {
        content(dark = true, mutation = false) {
            FarmerOrderRequestCard(OrderRequest(cropName = "Tomato", requestedQuantity = 30.0), listing, false, {}, {})
        }
        compose.onNodeWithText("Accept", substring = false).performScrollTo().assertIsNotEnabled()
        compose.onNodeWithText("Reject", substring = false).performScrollTo().assertIsNotEnabled()
    }
    @Test fun acceptedRequestRetainsOrderCallback() {
        var opened = false
        content(dark = true) {
            FarmerOrderRequestCard(OrderRequest(cropName = "Tomato", status = OrderRequestStatus.ACCEPTED),
                listing, false, {}, {}, { opened = true })
        }
        compose.onNodeWithText("View order").performScrollTo().performClick()
        compose.runOnIdle { assertTrue(opened) }
    }
    @Test fun cancelledOrderDoesNotGainHandoverOrDeliveryAction() {
        content(dark = true) {
            OrderLifecycleActions(Order(cropName = "Tomato", status = OrderStatus.CANCELLED), false, false, {})
        }
        compose.onAllNodes(hasClickAction()).assertCountEquals(0)
    }
    @Test fun farmerNeverGetsSupplierDeliveryControl() {
        content {
            OrderLifecycleActions(Order(cropName = "Tomato", status = OrderStatus.IN_TRANSIT), false, false, {})
        }
        compose.onNodeWithText("Confirm Delivery").assertDoesNotExist()
    }
    @Test fun failedPhotoShowsExplicitLocalFallback() {
        content { ListingImage("file:///missing_farmer_photo.jpg") }
        compose.waitUntil(10_000) {
            compose.onAllNodesWithContentDescription("Listing photo unavailable; generic produce artwork")
                .fetchSemanticsNodes().isNotEmpty()
        }
    }
    @Test fun profileEditorRetainsSaveValidationAndFields() {
        var saved: List<String>? = null
        content { ProfileEditDialog("", "123", "Farm address", false, {},
            { name, phone, location -> saved = listOf(name, phone, location) }) }
        compose.onNodeWithText("Save").assertIsNotEnabled()
        compose.onNode(hasSetTextAction() and hasText("Full name")).performTextInput("Farmer")
        compose.onNodeWithText("Save").performClick()
        compose.runOnIdle { assertEquals(listOf("Farmer", "123", "Farm address"), saved) }
    }

    @Test fun largeNotificationCountsWrapInLightTheme() = notificationFilters(false)
    @Test fun largeNotificationCountsWrapInDarkTheme() = notificationFilters(true)

    private fun notificationFilters(dark: Boolean) {
        var unread by mutableStateOf(false)
        content(dark = dark) {
            Box(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                com.agroroute.features.settings.NotificationFilterLayout {
                    FilterChip(selected = !unread, onClick = { unread = false }, label = { Text("All (999999)") })
                    FilterChip(selected = unread, onClick = { unread = true }, label = { Text("Unread (999999)") })
                }
            }
        }
        val all = compose.onNodeWithText("All (999999)").assertIsDisplayed().fetchSemanticsNode().boundsInRoot
        val selected = compose.onNodeWithText("Unread (999999)").assertIsDisplayed()
        val bounds = selected.fetchSemanticsNode().boundsInRoot
        assertFalse("Filter chips must not overlap", all.overlaps(bounds))
        selected.performClick()
        compose.runOnIdle { assertTrue(unread) }
    }
}
