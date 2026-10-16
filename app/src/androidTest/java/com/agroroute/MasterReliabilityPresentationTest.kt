package com.agroroute

import android.graphics.Bitmap
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.material3.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.agroroute.core.database.AppDatabase
import com.agroroute.core.model.*
import com.agroroute.core.runtime.*
import com.agroroute.core.theme.AgroRouteTheme
import com.agroroute.demo.DemoSessionManager
import com.agroroute.features.farmer.home.IncomingOrderRequests
import com.agroroute.features.farmer.orderrequests.AcceptRequestConfirmationDialog
import java.io.File
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class MasterReliabilityPresentationTest(private val dark: Boolean) {
    @get:Rule val compose = createComposeRule()
    companion object {
        @JvmStatic @Parameterized.Parameters(name = "dark={0}")
        fun themes() = listOf(arrayOf(false), arrayOf(true))
    }
    private fun capture(name: String) {
        val cache = InstrumentationRegistry.getInstrumentation().targetContext.cacheDir
        File(cache, "master-$name-${if (dark) "dark" else "light"}.png").outputStream().use {
            val bitmap = if (name == "create-keyboard") InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()
                else compose.onRoot().captureToImage().asAndroidBitmap()
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
        }
    }
    @Test fun homeRequestsShowRealCountPreviewAndReuseConfirmation() {
        val listing = HarvestListing(listingId = "l", farmerId = "f", quantity = 100.0,
            availableQuantity = 100.0, biddingEnabled = false, location = "Farm\n[7, 80]")
        val requests = (4 downTo 1).map { OrderRequest(requestId = "r$it", farmerId = "f", supplierId = "s",
            supplierName = "Supplier $it", cropName = "Tomatoes", requestedQuantity = 25.0, listedPrice = 180.0,
            listingId = "l", deliveryLocation = "Depot\n[6, 79]") }
        var viewed = false
        var confirmed = ""
        compose.setContent { AgroRouteTheme(darkTheme = dark) {
            var selected by remember { mutableStateOf<OrderRequest?>(null) }
            IncomingOrderRequests(requests, mapOf("l" to listing), false, { viewed = true }, { selected = it })
            selected?.let { request -> AcceptRequestConfirmationDialog(request, listing,
                { selected = null }, { confirmed = request.requestId; selected = null }) }
        } }
        compose.onNodeWithText("4 pending").assertIsDisplayed()
        compose.onNodeWithText("Supplier 1").assertDoesNotExist()
        compose.onNodeWithText("View All").performClick()
        compose.runOnIdle { assertTrue(viewed) }
        capture("home-requests")
        compose.onAllNodesWithText("Accept", substring = false).onFirst().performClick()
        compose.onNodeWithText("Accept this order request?").assertIsDisplayed()
        compose.onNodeWithText("Accept and create order").performClick()
        compose.runOnIdle { assertEquals("r4", confirmed) }
    }
    @Test fun emptyRequestsStayCompactAndInsufficientStockCannotAccept() {
        compose.setContent { AgroRouteTheme(darkTheme = dark) {
            Column {
                IncomingOrderRequests(emptyList(), emptyMap(), false, {}, {})
                Text("Farm essentials")
                IncomingOrderRequests(listOf(OrderRequest(requestId = "r", listingId = "l", requestedQuantity = 100.0)),
                    mapOf("l" to HarvestListing(quantity = 10.0, availableQuantity = 10.0, biddingEnabled = false)), false, {}, {})
            }
        } }
        compose.onNodeWithText("Farm essentials").assertIsDisplayed()
        compose.onNodeWithText("1 pending").assertIsDisplayed()
        compose.onNodeWithText("Accept", substring = false).assertIsNotEnabled()
    }
    @Test fun createListingRetainsFieldsPhotoSourcesMapAndKeyboardAccessiblePublish() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        val session = DemoSessionManager().apply { enter() }
        try {
            var imeVisible = false
            compose.setContent {
                val density = androidx.compose.ui.platform.LocalDensity.current
                val shown = WindowInsets.ime.getBottom(density) > 0
                SideEffect { imeVisible = shown }
                AgroRouteTheme(darkTheme = dark) { DemoExperience(session, db) {} }
            }
            compose.waitUntil(20_000) { compose.onAllNodesWithText("Continue as Farmer").fetchSemanticsNodes()
                .firstOrNull()?.config?.contains(androidx.compose.ui.semantics.SemanticsProperties.Disabled) == false }
            compose.onNodeWithText("Continue as Farmer").performClick()
            compose.waitUntil(20_000) { compose.onAllNodesWithText("Good day, Nimal Perera").fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithText("New listing", substring = false).performScrollTo().performClick()
            compose.onNodeWithText("Create a listing").assertExists()
            capture("create-top")
            for (label in listOf("Harvest photos", "Take Photo", "Choose from Gallery", "Harvest details", "Crop or harvest name",
                "Tell buyers about this harvest", "Quality grade (optional)", "Organic", "Inorganic", "Quantity & price", "Quantity", "Unit",
                "Reserve price per KG", "Availability", "Ready now", "Expected harvest", "Set expected availability date (optional)", "Available for (hours, optional)", "Pickup location", "Choose pickup location on map")) {
                compose.onAllNodesWithText(label, substring = false).onLast().performScrollTo().assertIsDisplayed()
            }
            capture("create-location")
            compose.onNodeWithText("Choose pickup location on map").performClick()
            compose.onNodeWithTag("location-map").assertIsDisplayed()
            compose.onNodeWithText("Use this location").assertIsDisplayed()
            compose.onAllNodesWithContentDescription("Back").onLast().performClick()
            compose.onNodeWithText("Tell buyers about this harvest").performScrollTo().performClick()
            compose.waitUntil(10_000) { imeVisible }
            compose.onNodeWithText("Tell buyers about this harvest").performTextInput("Fresh harvest. ".repeat(40))
            compose.waitForIdle()
            compose.onNodeWithText("Publish listing", substring = false).assertIsDisplayed()
            capture("create-keyboard")
        } finally {
            compose.runOnIdle { session.exit() }; compose.waitForIdle()
            BackgroundNetworkAccess.resume(); db.close()
        }
    }
}
