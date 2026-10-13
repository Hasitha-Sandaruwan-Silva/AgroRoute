package com.agroroute

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.agroroute.core.database.AppDatabase
import com.agroroute.core.model.*
import com.agroroute.core.runtime.*
import com.agroroute.core.theme.AgroRouteTheme
import com.agroroute.demo.*
import com.agroroute.features.supplier.home.*
import com.github.thelacspace.icons.Lucide
import org.junit.*
import org.junit.Assert.*
import java.io.File

class SupplierHomePresentationTest {
    @get:Rule val compose = createComposeRule()
    private val now = 1_800_000_000_000L
    private val tomato = HarvestListing(listingId = "tomato", cropName = "Tomato", quantity = 100.0,
        soldQuantity = 40.0, reservePrice = 320.0, location = "Kurunegala\n[7.4,80.3]", createdAt = now - 3_600_000)
    private val banana = tomato.copy(listingId = "banana", cropName = "Banana", biddingEnabled = false)
    private var action = ""
    private fun content(rows: List<HarvestListing> = listOf(tomato, banana), dark: Boolean = false,
        compact: Boolean = false, fontScale: Float = 1f, loading: Boolean = false) {
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale)) {
                AgroRouteTheme(darkTheme = dark) {
                    Scaffold(bottomBar = { SupplierHomeBottomNavigation(onNavigate = { action = it }) }) { padding ->
                        Box(Modifier.padding(padding).width(if (compact) 320.dp else 400.dp).fillMaxHeight()) {
                            SupplierHomeContent(UserProfile(displayName = "Local Supplier", role = UserRole.SUPPLIER), rows,
                                listOf(Bid(status = BidStatus.PENDING), Bid(status = BidStatus.ACCEPTED)),
                                listOf(Order(status = OrderStatus.IN_TRANSIT), Order(status = OrderStatus.DELIVERED)),
                                loading, now, { action = "market" }, { action = "orders" }, { action = "bids" },
                                { action = "profile" }, { action = "community" }, { action = it.listingId },
                                notification = { IconButton({ action = "notifications" }) { Icon(Lucide.Bell, "Notifications") } })
                        }
                    }
                }
            }
        }
    }
    private fun scroll(tag: String) {
        if (tag.startsWith("featured-")) {
            compose.onNodeWithTag("supplier-home").performScrollToNode(hasTestTag("featured-row"))
            compose.onNodeWithTag("featured-row").performScrollToNode(hasTestTag(tag))
        } else compose.onNodeWithTag("supplier-home").performScrollToNode(hasTestTag(tag))
    }
    private fun textScroll(text: String) { compose.onNodeWithTag("supplier-home").performScrollToNode(hasText(text, substring = true)) }
    private fun capture(name: String) {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        File(context.cacheDir, name).outputStream().use {
            compose.onRoot().captureToImage().asAndroidBitmap().compress(android.graphics.Bitmap.CompressFormat.PNG,100,it)
        }
    }
    @Test fun headerSearchCategoriesAndBannerUseReferenceHierarchy() {
        content()
        compose.onNodeWithText("AgroRoute").assertIsDisplayed()
        compose.onNodeWithText("Fresh Farms. Direct to You.").assertIsDisplayed()
        compose.onNodeWithTag("supplier-search").assertIsDisplayed()
        compose.onNodeWithText("All").assertIsDisplayed()
        compose.onNodeWithText("Vegetables").assertIsDisplayed()
        compose.onNodeWithText("Fresh Produce, Direct from Farmers").assertIsDisplayed()
        capture("supplier-home-light-top.png")
    }
    @Test fun statisticsAreDerivedFromStateAndEachHasARealAction() {
        content()
        for ((label, value) in listOf("New Listings" to "2", "Active Bids" to "1", "Recent Orders" to "2", "On the Way" to "1")) {
            scroll("stat-$label")
            compose.onNodeWithTag("stat-$label", useUnmergedTree = true).assert(hasAnyDescendant(hasText(value)))
        }
        compose.onNodeWithTag("stat-Active Bids").performClick()
        compose.runOnIdle { assertEquals("bids", action) }
    }
    @Test fun searchOpensExistingMarketplace() {
        content(); compose.onNodeWithTag("supplier-search").performClick()
        compose.runOnIdle { assertEquals("market", action) }
    }
    @Test fun viewMapUsesMarketplaceFallback() {
        content(); compose.onNodeWithText("View Map").performClick()
        compose.runOnIdle { assertEquals("market", action) }
    }
    @Test fun categoriesFilterRealProduceWithoutChangingSchema() {
        content(); compose.onNodeWithText("Fruits").performClick(); scroll("featured-banana")
        compose.onNodeWithTag("featured-banana").assertExists()
        compose.onNodeWithTag("featured-tomato").assertDoesNotExist()
    }
    @Test fun produceRendersRealTermsAndNavigatesToListing() {
        content(); scroll("featured-tomato")
        compose.onNodeWithTag("featured-tomato", useUnmergedTree = true).assert(hasAnyDescendant(hasText("Rs. 320 / kg")))
        compose.onNodeWithTag("featured-tomato", useUnmergedTree = true).assert(hasAnyDescendant(hasText("60 kg available")))
        capture("supplier-home-featured.png")
        compose.onNodeWithTag("featured-tomato").performClick()
        compose.runOnIdle { assertEquals("tomato", action) }
    }
    @Test fun latestRowsAndFarmerPromoAreReachableByScrolling() {
        content(); scroll("latest-tomato"); compose.onNodeWithTag("latest-tomato").performClick()
        compose.runOnIdle { assertEquals("tomato", action) }
        textScroll("Explore More"); compose.onNodeWithText("Explore More").assertIsDisplayed()
        capture("supplier-home-promo.png")
        compose.onNodeWithText("Explore More").performClick()
        compose.runOnIdle { assertEquals("market", action) }
    }
    @Test fun bottomTabsKeepOnlyValidRoutes() {
        content()
        compose.onNodeWithText("Marketplace", substring = false).performClick()
        compose.runOnIdle { assertEquals("supplier_marketplace", action) }
        compose.onNodeWithText("Orders", substring = false).performClick()
        compose.runOnIdle { assertEquals("supplier_orders", action) }
        compose.onNodeWithText("Profile", substring = false).performClick()
        compose.runOnIdle { assertEquals("supplier_profile", action) }
        compose.onNodeWithText("Map", substring = false).assertDoesNotExist()
    }
    @Test fun notificationAndProfileActionsRemainInteractive() {
        content(); compose.onNodeWithContentDescription("Notifications").performClick()
        compose.runOnIdle { assertEquals("notifications", action) }
        compose.onNodeWithText("Supplier", substring = false).performClick()
        compose.runOnIdle { assertEquals("profile", action) }
    }
    @Test fun emptyStateRetainsHeroAndNavigation() {
        content(rows = emptyList()); textScroll("No listings available")
        compose.onNodeWithText("No listings available").assertIsDisplayed()
        compose.onNodeWithText("Orders", substring = false).assertIsDisplayed()
    }
    @Test fun loadingStateIsReadable() {
        content(rows = emptyList(), loading = true); textScroll("Loading produce")
        compose.onNodeWithText("Loading produce").assertIsDisplayed()
    }
    @Test fun darkCompactAndLargeTextStayScrollable() {
        content(dark = true, compact = true, fontScale = 1.5f)
        compose.onNodeWithTag("supplier-search").assertIsDisplayed()
        textScroll("Explore More"); compose.onNodeWithText("Explore More").assertIsDisplayed()
        compose.onNodeWithText("Profile", substring = false).assertIsDisplayed()
        capture("supplier-home-dark-compact.png")
    }
    @Test fun expiredAndSoldOutListingsNeverBecomeFeatured() {
        content(rows = listOf(tomato.copy(expiresAt = now), banana.copy(availableQuantity = 0.0)))
        textScroll("No listings available"); compose.onNodeWithTag("featured-tomato").assertDoesNotExist()
        compose.onNodeWithTag("featured-banana").assertDoesNotExist()
    }
    @Test fun failedPhotoUsesReadableLocalFallback() {
        content(rows = listOf(tomato.copy(imageUrls = listOf("file:///nonexistent_supplier_photo.jpg"))))
        scroll("featured-tomato")
        compose.waitUntil(10_000) { compose.onAllNodesWithContentDescription("Tomato photo unavailable")
            .fetchSemanticsNodes(atLeastOneRootRequired = false).isNotEmpty() }
        compose.onNodeWithTag("featured-tomato").assertIsDisplayed()
    }
    @Test fun suppliedPhotoDoesNotAnnounceUnavailableFallback() {
        content(rows = listOf(tomato.copy(imageUrls = listOf("android.resource://com.agroroute/${R.drawable.supplier_farm_hero}"))))
        scroll("featured-tomato")
        compose.onAllNodesWithContentDescription("Tomato photo unavailable").assertCountEquals(0)
        compose.onAllNodesWithContentDescription("Tomato").onFirst().assertIsDisplayed()
    }
    @Test fun actualDemoGraphDisplaysLocalProduceAndNewStatistics() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        val manager = DemoSessionManager().apply { enter() }
        try {
            compose.setContent { AgroRouteTheme { DemoExperience(manager, db) { } } }
            compose.waitUntil(20_000) { compose.onAllNodesWithText("Continue as Supplier").fetchSemanticsNodes().firstOrNull()?.config
                ?.contains(androidx.compose.ui.semantics.SemanticsProperties.Disabled) == false }
            compose.onNodeWithText("Continue as Supplier").performClick()
            compose.waitUntil(20_000) { compose.onAllNodesWithTag("stat-New Listings").fetchSemanticsNodes(atLeastOneRootRequired = false).isNotEmpty() }
            // Await the seeded DAO emission, rather than scrolling during the initial empty/loading frame.
            compose.waitUntil(20_000) { compose.onAllNodesWithText("7", substring = false).fetchSemanticsNodes(atLeastOneRootRequired = false).isNotEmpty() }
            scroll("featured-demo_listing_001")
            compose.onNodeWithTag("featured-demo_listing_001", useUnmergedTree = true).assert(hasAnyDescendant(hasText("Tomato")))
            compose.onNodeWithTag("featured-demo_listing_001", useUnmergedTree = true).assert(hasAnyDescendant(hasText("100 kg available")))
        } finally {
            compose.runOnIdle { manager.exit() }; compose.waitForIdle(); BackgroundNetworkAccess.resume(); db.close()
        }
    }
}
