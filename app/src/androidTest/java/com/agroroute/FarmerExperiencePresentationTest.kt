package com.agroroute

import androidx.compose.material3.*
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.navigation.compose.rememberNavController
import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.agroroute.core.database.AppDatabase
import com.agroroute.core.model.UserRole
import com.agroroute.core.runtime.*
import com.agroroute.core.theme.AgroRouteTheme
import com.agroroute.demo.*
import com.agroroute.features.welcome.WelcomeScreen
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*
import java.io.File

/** Real normal role screens fed through the debug scope; no production login or data writes. */
class FarmerExperiencePresentationTest {
    @get:Rule val compose=createComposeRule()
    private lateinit var db: AppDatabase
    private lateinit var session: DemoSessionManager
    private var dark by mutableStateOf(false)
    private var compact by mutableStateOf(false)
    @Before fun open() {
        val context=InstrumentationRegistry.getInstrumentation().targetContext
        db=Room.inMemoryDatabaseBuilder(context,AppDatabase::class.java).build()
        session=DemoSessionManager()
        compose.setContent { AgroRouteTheme(darkTheme = dark) {
            val density = androidx.compose.ui.platform.LocalDensity.current
            val width = androidx.compose.ui.platform.LocalConfiguration.current.screenWidthDp
            CompositionLocalProvider(androidx.compose.ui.platform.LocalDensity provides
                androidx.compose.ui.unit.Density(density.density, if (compact) 1.6f else 1f)) {
                androidx.compose.foundation.layout.Box(androidx.compose.ui.Modifier
                    .then(androidx.compose.ui.Modifier.width((if (compact) 320 else width).dp))
                    .fillMaxHeight()) {
                    DemoExperience(session,db) { enter ->
                        CompositionLocalProvider(LocalEntry provides enter) { WelcomeScreen(rememberNavController()) }
                    }
                }
            }
        } }
    }
    @After fun close() {
        compose.runOnIdle { session.exit() }
        compose.waitForIdle()
        BackgroundNetworkAccess.resume()
        db.close()
    }
    private fun awaitText(text: String) {
        compose.waitUntil(20_000) { compose.onAllNodesWithText(text,substring=true).fetchSemanticsNodes(atLeastOneRootRequired = false).isNotEmpty() }
    }
    private fun enter(role: UserRole) {
        compose.onNodeWithText("Try Demo Mode").performClick()
        val label="Continue as ${role.name.lowercase().replaceFirstChar { it.uppercase() }}"
        compose.waitUntil(20_000) {
            compose.onAllNodesWithText(label).fetchSemanticsNodes().firstOrNull()?.config
                ?.contains(androidx.compose.ui.semantics.SemanticsProperties.Disabled)==false
        }
        compose.onNodeWithText(label).performClick()
        awaitText(if (role==UserRole.FARMER) "Good day, Nimal Perera" else "Good day, FreshMart Traders")
    }
    private fun menu(label: String) {
        compose.onNodeWithText("Demo menu").performClick()
        awaitText(label)
        compose.onNodeWithText(label).performScrollTo().performClick()
    }
    private fun capture(name: String) {
        val context=InstrumentationRegistry.getInstrumentation().targetContext
        File(context.cacheDir,name).outputStream().use {
            compose.onRoot().captureToImage().asAndroidBitmap().compress(android.graphics.Bitmap.CompressFormat.PNG,100,it)
        }
    }
    @Test fun farmerRootsStayReachableInBothThemes() {
        enter(UserRole.FARMER)
        for (night in listOf(false, true, false)) {
            compose.runOnIdle { dark = night }
            val mode = if (night) "dark" else "light"
            compose.onAllNodesWithText("Home", substring=false).onLast().performClick()
            awaitText("Good day, Nimal Perera")
            compose.runOnIdle {
                val activity = androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry.getInstance()
                    .getActivitiesInStage(androidx.test.runner.lifecycle.Stage.RESUMED).single()
                assertEquals(!night, androidx.core.view.WindowCompat.getInsetsController(activity.window, activity.window.decorView).isAppearanceLightStatusBars)
            }
            capture("farmer-$mode-home.png")
            compose.onAllNodesWithText("Listings", substring=false).onLast().performClick()
            awaitText("My Harvests")
            awaitText("Tomato")
            capture("farmer-$mode-listings.png")
            compose.onAllNodesWithText("Orders", substring=false).onLast().performClick()
            awaitText("My orders")
            compose.onAllNodesWithText("Awaiting pickup", substring=false).onFirst().performScrollTo().assertIsDisplayed()
            capture("farmer-$mode-orders.png")
            compose.onAllNodesWithText("Profile", substring=false).onLast().performClick()
            awaitText("Nimal Perera")
            capture("farmer-$mode-profile.png")
            compose.onNodeWithText("Settings", substring=false).performScrollTo().assertIsDisplayed()
        }
        assertFalse(BackgroundNetworkAccess.allowed.value)
        assertEquals("demo_farmer_001", session.state.value.selectedDemoUserId)
    }
    @Test fun farmerSubscreensRetainNavigationInBothThemes() {
        enter(UserRole.FARMER)
        for (night in listOf(false, true)) {
            compose.runOnIdle { dark = night }
            val mode = if (night) "dark" else "light"
            menu("Browse listings")
            awaitText("Tomato")
            compose.onAllNodesWithText("Tomato", substring=false).onFirst().performClick()
            awaitText("Bids for")
            capture("farmer-$mode-listing-details.png")
            compose.onNodeWithContentDescription("Back", substring=false).performClick()
            awaitText("My Harvests")
            compose.onNodeWithText("New Listing", substring=false, useUnmergedTree=true).performClick()
            awaitText("Create a listing")
            capture("farmer-$mode-create-listing.png")
            menu("Browse bids")
            awaitText("Incoming bids")
            awaitText("Requested quantity")
            capture("farmer-$mode-bids.png")
            menu("Browse requests")
            awaitText("Order requests")
            awaitText("Listed price")
            capture("farmer-$mode-requests.png")
            menu("Orders and history")
            awaitText("View details")
            compose.onAllNodesWithText("View details", substring=false).onFirst().performScrollTo().performClick()
            awaitText("Order #")
            capture("farmer-$mode-order-details.png")
            menu("Profile summary")
            awaitText("Nimal Perera")
            compose.onNodeWithText("Notifications", substring=false).performScrollTo().performClick()
            awaitText("Notifications")
            capture("farmer-$mode-notifications.png")
        }
        assertFalse(BackgroundNetworkAccess.allowed.value)
    }
    @Test fun compactFarmerControlsRemainReachableWithKeyboardAndLargeText() {
        enter(UserRole.FARMER)
        compose.runOnIdle { compact = true; dark = true }
        awaitText("Demo menu")
        val menuBounds = compose.onNodeWithText("Demo menu").getUnclippedBoundsInRoot()
        assertTrue(menuBounds.bottom - menuBounds.top <= 64.dp)
        compose.onAllNodesWithText("Listings", substring=false).onLast().performClick()
        awaitText("My Harvests")
        compose.onNode(hasSetTextAction()).performTextInput("Tomato")
        compose.onNode(hasSetTextAction()).assertIsDisplayed()
        compose.waitUntil(10_000) {
            compose.onAllNodesWithText("New Listing", useUnmergedTree=true).fetchSemanticsNodes().isEmpty()
        }
        capture("farmer-compact-keyboard.png")
        androidx.test.espresso.Espresso.pressBack()
        compose.waitUntil(10_000) {
            compose.onAllNodesWithText("New Listing", useUnmergedTree=true)
                .fetchSemanticsNodes(atLeastOneRootRequired = false).isNotEmpty()
        }
    }

}
