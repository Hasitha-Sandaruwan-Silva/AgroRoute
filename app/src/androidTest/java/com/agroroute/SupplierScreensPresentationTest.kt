package com.agroroute

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.agroroute.core.components.*
import com.agroroute.core.model.*
import com.agroroute.core.runtime.LocalContactLookup
import com.agroroute.core.runtime.LocalMutationAllowed
import com.agroroute.core.theme.AgroRouteTheme
import com.agroroute.features.supplier.components.*
import com.agroroute.features.supplier.marketplace.MarketplaceListingCard
import com.agroroute.features.supplier.orders.SupplierOrderRequestCard
import com.agroroute.features.supplier.shell.SupplierBottomBar
import com.github.thelacspace.icons.Lucide
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.*

/** Interaction checks for the redesigned surfaces, using existing eligibility and callbacks. */
class SupplierScreensPresentationTest {
    @get:Rule val compose = createComposeRule()
    private val listing = HarvestListing(listingId = "crop", cropName = "Tomato", farmerName = "Local Farmer",
        quantity = 100.0, soldQuantity = 40.0, reservePrice = 320.0)
    private fun content(dark: Boolean = false, large: Boolean = false, mutation: Boolean = true,
        body: @Composable () -> Unit) {
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, if (large) 1.6f else 1f),
                LocalSupplierPresentation provides true, LocalMutationAllowed provides mutation,
                LocalContactLookup provides { _: String -> "Local Farmer" to null }) {
                AgroRouteTheme(darkTheme = dark) {
                    Column(Modifier.width(320.dp).fillMaxHeight().verticalScroll(rememberScrollState())) { body() }
                }
            }
        }
    }
    @Test fun biddingCardUsesActualActionAndRemainingStock() {
        var clicked = false
        content { MarketplaceListingCard(listing, true, false) { clicked = true } }
        compose.onNodeWithText("60.0 KG").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("View / Bid").performScrollTo().performClick()
        compose.runOnIdle { assertTrue(clicked) }
    }
    @Test fun fixedPriceCardUsesRequestCallback() {
        var clicked = false
        content { MarketplaceListingCard(listing.copy(biddingEnabled = false), false, true) { clicked = true } }
        compose.onNodeWithText("View / Request").performScrollTo().performClick()
        compose.runOnIdle { assertTrue(clicked) }
    }
    @Test fun unavailableListingHasNoSubmissionAction() {
        var clicked = false
        content { MarketplaceListingCard(listing.copy(availableQuantity = 0.0), false, false) { clicked = true } }
        compose.onNodeWithText("View / Bid").assertDoesNotExist()
        compose.onNodeWithText("View / Request").assertDoesNotExist()
        compose.runOnIdle { assertFalse(clicked) }
    }
    @Test fun darkLargeTextRequestCancellationRemainsReachable() {
        var cancelled = false
        content(dark = true, large = true) {
            SupplierOrderRequestCard(OrderRequest(cropName = "Tomato", farmerName = "Local Farmer",
                requestedQuantity = 12.0, listedPrice = 320.0), listing, { cancelled = true })
        }
        compose.onNodeWithText("Cancel request").performScrollTo().performClick()
        compose.runOnIdle { assertTrue(cancelled) }
    }
    @Test fun demoRequestCancellationIsDisabled() {
        content(mutation = false) {
            SupplierOrderRequestCard(OrderRequest(cropName = "Tomato"), listing, {})
        }
        compose.onNodeWithText("Cancel request").performScrollTo().assertIsNotEnabled()
    }
    @Test fun rejectedRequestDoesNotGainCancellation() {
        content { SupplierOrderRequestCard(OrderRequest(cropName = "Tomato",
            status = OrderRequestStatus.REJECTED), listing, null) }
        compose.onNodeWithText("Cancel request").assertDoesNotExist()
        compose.onNodeWithText("Rejected").performScrollTo().assertIsDisplayed()
    }
    @Test fun failedListingPhotoUsesLocalFallback() {
        content { SupplierListingImage("file:///missing_supplier_photo.jpg") }
        compose.waitUntil(10_000) {
            compose.onAllNodesWithContentDescription("Listing photo unavailable; generic produce illustration")
                .fetchSemanticsNodes().isNotEmpty()
        }
    }
    @Test fun realPhotoHasPriorityOverLocalArt() {
        content { SupplierListingImage("android.resource://com.agroroute/${R.drawable.supplier_farm_hero}") }
        compose.onNodeWithContentDescription("Listing photo").assertIsDisplayed()
        compose.onAllNodesWithContentDescription("Listing photo unavailable; generic produce illustration").assertCountEquals(0)
    }
    @Test fun emptyStateHasNoInventedAction() {
        content(dark = true, large = true) { SupplierEmptyState(Lucide.Inbox, "No requests yet", "Requests appear here") }
        compose.onNodeWithText("No requests yet").assertIsDisplayed()
        compose.onAllNodes(hasClickAction()).assertCountEquals(0)
    }
    @Test fun allTabsUseExistingRoutes() {
        var route = ""
        content { SupplierBottomBar("supplier_orders", { route = it }) }
        compose.onNodeWithText("Marketplace").performClick()
        compose.runOnIdle { assertEquals("supplier_marketplace", route) }
        compose.onNodeWithText("Profile").performClick()
        compose.runOnIdle { assertEquals("supplier_profile", route) }
        compose.onNodeWithText("Map", substring = false).assertDoesNotExist()
    }
    @Test fun profileEditorRetainsValidationAndSaveFields() {
        var saved: List<String>? = null
        content { ProfileEditDialog("", "123", "Business address", false, {},
            { name, phone, location -> saved = listOf(name, phone, location) }) }
        compose.onNodeWithText("Save").assertIsNotEnabled()
        compose.onNode(hasSetTextAction() and hasText("Full name")).performTextInput("Supplier")
        compose.onNodeWithText("Save").performClick()
        compose.runOnIdle { assertEquals(listOf("Supplier", "123", "Business address"), saved) }
    }
    @Test fun pickerKeepsMapAliveAcrossCompactLayoutChanges() {
        var short by mutableStateOf(false)
        var creations = 0
        var disposals = 0
        compose.setContent {
            AgroRouteTheme {
                Box(Modifier.width(520.dp).height(if (short) 160.dp else 600.dp)) {
                    LocationPickerLayout(insets = WindowInsets(0, 0, 0, 0), search = {},
                        map = {
                            DisposableEffect(Unit) { creations++; onDispose { disposals++ } }
                            Box(Modifier.fillMaxSize().testTag("live-map"))
                        }, address = { androidx.compose.material3.Text("Address") },
                        confirm = { androidx.compose.material3.Button({}) { androidx.compose.material3.Text("Use this location") } })
                }
            }
        }
        compose.onNodeWithText("Use this location").assertIsDisplayed()
        compose.runOnIdle { short = true }
        compose.onNodeWithTag("live-map").assertIsDisplayed()
        compose.onNodeWithText("Use this location").assertIsDisplayed()
        compose.runOnIdle { assertEquals(1, creations); assertEquals(0, disposals) }
    }
    @Test fun reviewStarsAreAccessibleAndKeepExactRatingValues() {
        var rating by mutableIntStateOf(0)
        content(dark = true, large = true) {
            com.agroroute.features.reviews.RatingCard(rating) { rating = it }
        }
        compose.onNodeWithContentDescription("5 out of 5 stars").performClick()
        compose.runOnIdle { assertEquals(5, rating) }
        compose.onNodeWithContentDescription("5 out of 5 stars").assertIsSelected()
        compose.onNodeWithText("Excellent").assertIsDisplayed()
    }
    @Test fun reviewCommentKeepsMultilineInputAtLargeFontScale() {
        var comment by mutableStateOf("")
        content(large = true) { com.agroroute.features.reviews.CommentCard(comment) { comment = it } }
        compose.onNode(hasSetTextAction()).performTextInput("Fresh harvest\nClear communication")
        compose.runOnIdle { assertEquals("Fresh harvest\nClear communication", comment) }
    }
}
