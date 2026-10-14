package com.agroroute

import androidx.compose.material3.*
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
class NormalDemoPresentationTest {
    @get:Rule val compose=createComposeRule()
    private lateinit var db: AppDatabase
    private lateinit var session: DemoSessionManager
    private var dark by mutableStateOf(false)
    @Before fun open() {
        val context=InstrumentationRegistry.getInstrumentation().targetContext
        db=Room.inMemoryDatabaseBuilder(context,AppDatabase::class.java).build()
        session=DemoSessionManager()
        compose.setContent { AgroRouteTheme(darkTheme = dark) {
            DemoExperience(session,db) { enter ->
                CompositionLocalProvider(LocalEntry provides enter) { WelcomeScreen(rememberNavController()) }
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
        compose.onNodeWithText(label).performScrollTo().performClick()
    }
    private fun capture(name: String) {
        val context=InstrumentationRegistry.getInstrumentation().targetContext
        File(context.cacheDir,name).outputStream().use {
            compose.onRoot().captureToImage().asAndroidBitmap().compress(android.graphics.Bitmap.CompressFormat.PNG,100,it)
        }
    }
    @Test fun entryWithoutFirebaseLoginShowsRealFarmerHomeAndListings() {
        val uid=com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
        enter(UserRole.FARMER)
        assertEquals("demo_farmer_001",session.state.value.selectedDemoUserId)
        capture("normal-demo-farmer-home.png")
        compose.onAllNodesWithText("Listings",substring=false).onLast().performClick()
        awaitText("Tomato")
        assertEquals(uid,com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid)
        assertFalse(BackgroundNetworkAccess.allowed.value)
    }
    @Test fun realSupplierHomeMarketplaceAndBidsContainDemoRecords() {
        enter(UserRole.SUPPLIER)
        capture("normal-demo-supplier-home.png")
        compose.onAllNodesWithText("Marketplace",substring=false).onLast().performClick()
        awaitText("Tomato")
        capture("normal-demo-marketplace.png")
        menu("Browse bids")
        awaitText("Requested: 40.0")
        awaitText("Pending")
        val controlsWithdraw = compose.onAllNodesWithText("Withdraw",substring=false).fetchSemanticsNodes()
        assertTrue(controlsWithdraw.isNotEmpty())
        controlsWithdraw.forEach {
            assertTrue(it.config.contains(androidx.compose.ui.semantics.SemanticsProperties.Disabled))
        }
    }
    @Test fun roleSwitcherRecreatesBothNormalNavigationTrees() {
        enter(UserRole.FARMER)
        menu("Switch to Supplier")
        awaitText("Good day, FreshMart Traders")
        assertEquals("demo_supplier_001",session.state.value.selectedDemoUserId)
        menu("Switch to Farmer")
        awaitText("Good day, Nimal Perera")
        assertEquals("demo_farmer_001",session.state.value.selectedDemoUserId)
        compose.onAllNodesWithText("Marketplace",substring=false).assertCountEquals(0)
    }
    @Test fun farmerBidsAndRequestsReuseNormalReadOnlyScreens() {
        enter(UserRole.FARMER)
        menu("Browse bids")
        awaitText("Tomato")
        awaitText("FreshMart Traders")
        val controlsAccept = compose.onAllNodesWithText("Accept",substring=false).fetchSemanticsNodes()
        assertTrue(controlsAccept.isNotEmpty())
        controlsAccept.forEach {
            assertTrue(it.config.contains(androidx.compose.ui.semantics.SemanticsProperties.Disabled))
        }
        menu("Browse requests")
        awaitText("Carrot")
    }
    @Test fun supplierRequestsOrdersAndProfileAreLocalAndReadOnly() {
        enter(UserRole.SUPPLIER)
        menu("Orders and history")
        awaitText("Order requests")
        awaitText("Carrot")
        menu("Profile summary")
        awaitText("FreshMart Traders")
        val controlsLogout = compose.onAllNodesWithText("Log out",substring=false).fetchSemanticsNodes()
        assertTrue(controlsLogout.isNotEmpty())
        controlsLogout.forEach {
            assertTrue(it.config.contains(androidx.compose.ui.semantics.SemanticsProperties.Disabled))
        }
    }
    @Test fun exitReturnsToRealWelcomeAndPreservesDemoDatabase() = runBlocking {
        enter(UserRole.FARMER)
        compose.onNodeWithText("Demo menu").performClick()
        compose.onNodeWithText("Exit Demo Mode").performClick()
        awaitText("Try Demo Mode")
        assertEquals(DemoSession(),session.state.value)
        assertEquals(10,db.harvestListingDao().getAllListings().first().size)
        assertTrue(BackgroundNetworkAccess.allowed.value)
    }
    @Test fun backAtRoleHomeExitsOnlyLocalSession() = runBlocking {
        enter(UserRole.SUPPLIER)
        androidx.test.espresso.Espresso.pressBack()
        awaitText("Try Demo Mode")
        assertEquals(DemoSession(),session.state.value)
        assertEquals(10,db.harvestListingDao().getAllListings().first().size)
        assertTrue(BackgroundNetworkAccess.allowed.value)
    }
    @Test fun supplierOrderDetailUsesSavedPickupMapAndDisabledLifecycle() {
        enter(UserRole.SUPPLIER)
        menu("Orders and history")
        awaitText("Carrot")
        compose.onAllNodesWithText("Carrot",substring=false).onFirst().performClick()
        awaitText("Farmer: Nimal Perera")
        compose.onNodeWithText("Go to Farmer pickup").performScrollTo().assertExists()
        compose.onNodeWithTag("location-map").assertExists()
        compose.onNodeWithText("Cancel Order").assertIsNotEnabled()
        assertTrue(BackgroundNetworkAccess.allowed.value.not())
        capture("normal-demo-order-pickup.png")
    }
    @Test fun clearMenuPreservesNamespacePolicyAndRequiresExplicitReload() = runBlocking {
        enter(UserRole.SUPPLIER)
        menu("Clear Demo Data")
        awaitText("Demo data cleared")
        compose.onNodeWithText("Close",substring=false).performClick()
        assertTrue(db.harvestListingDao().getAllListings().first().isEmpty())
        compose.onNodeWithText("Continue as Farmer").assertIsNotEnabled()
        compose.onNodeWithText("Load Demo Data").performClick()
        compose.waitUntil(20_000) {
            compose.onNodeWithText("Continue as Farmer").fetchSemanticsNode().config
                .contains(androidx.compose.ui.semantics.SemanticsProperties.Disabled).not()
        }
        assertEquals(10,db.harvestListingDao().getAllListings().first().size)
    }

    @Test fun supplierRedesignJourneyKeepsRealRoutesAndReadOnlyActionsInBothThemes() {
        enter(UserRole.SUPPLIER)
        for (night in listOf(false, true)) {
            compose.runOnIdle { dark = night }
            val scheme = if (night) "dark" else "light"
            menu("Browse listings")
            awaitText("Tomato")
            capture("supplier-marketplace-$scheme.png")
            compose.onNode(hasSetTextAction()).performTextReplacement("Tomato")
            compose.onNodeWithText("View / Bid").performScrollTo().performClick()
            awaitText("Your offer")
            capture("supplier-listing-bid-$scheme.png")
            compose.onNode(hasText("Place Bid") and hasClickAction()).performScrollTo().assertIsNotEnabled()
            capture("supplier-bid-form-$scheme.png")
            menu("Browse listings")
            compose.onNode(hasSetTextAction()).performTextReplacement("Carrot")
            compose.onNodeWithText("View / Request").performScrollTo().performClick()
            awaitText("Your order request")
            capture("supplier-listing-request-$scheme.png")
            compose.onNodeWithText("Request Order").performScrollTo().assertIsNotEnabled()
            capture("supplier-request-form-$scheme.png")
            menu("Browse bids")
            awaitText("Requested: 40.0")
            capture("supplier-bids-$scheme.png")
            menu("Orders and history")
            awaitText("Carrot")
            capture("supplier-orders-$scheme.png")
            compose.onAllNodesWithText("Carrot", substring = false).onFirst().performClick()
            awaitText("Farmer: Nimal Perera")
            capture("supplier-order-detail-$scheme.png")
            compose.onNodeWithText("Go to Farmer pickup").performScrollTo().assertExists()
            capture("supplier-order-map-$scheme.png")
            menu("Profile summary")
            awaitText("FreshMart Traders")
            capture("supplier-profile-$scheme.png")
            compose.onNodeWithText("Notifications", substring = false).performScrollTo().performClick()
            awaitText("No notifications yet")
            capture("supplier-notifications-$scheme.png")
            assertFalse(BackgroundNetworkAccess.allowed.value)
        }
    }
}
