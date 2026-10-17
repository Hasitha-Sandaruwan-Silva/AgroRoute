package com.agroroute

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import com.agroroute.core.runtime.LocalContactLookup
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.navigation.compose.*
import com.agroroute.core.model.*
import com.agroroute.core.theme.AgroRouteTheme
import com.agroroute.features.leaderboard.*
import com.agroroute.features.participants.*
import kotlinx.coroutines.flow.flowOf
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class ProfileDiscoveryPresentationTest(private val dark: Boolean, private val viewer: UserRole) {
    @get:Rule val compose = createComposeRule()
    companion object {
        @JvmStatic @Parameterized.Parameters(name = "dark={0},viewer={1}")
        fun themes() = listOf(arrayOf(false, UserRole.FARMER), arrayOf(true, UserRole.FARMER),
            arrayOf(false, UserRole.SUPPLIER), arrayOf(true, UserRole.SUPPLIER))
    }
    private val farmer = UserProfile(userId = "farmer-private-id", displayName = "Nimal Perera", role = UserRole.FARMER, points = 40)
    private val supplier = UserProfile(userId = "supplier-private-id", displayName = "Kasun Silva", role = UserRole.SUPPLIER, points = 50)
    private val counterpart get() = if (viewer == UserRole.FARMER) supplier else farmer
    @Composable private fun Theme(content: @Composable () -> Unit) {
        AgroRouteTheme(darkTheme = dark) {
            val surface: @Composable () -> Unit = {
                Surface(color = MaterialTheme.colorScheme.background, contentColor = MaterialTheme.colorScheme.onBackground) { content() }
            }
            if (viewer == UserRole.FARMER) com.agroroute.features.farmer.components.FarmerPresentation(surface)
            else CompositionLocalProvider(com.agroroute.features.supplier.components.LocalSupplierPresentation provides true, content = surface)
        }
    }
    private fun profileVm() = ParticipantProfileViewModel(object : ParticipantRepository {
        override suspend fun identity(id: String): ParticipantIdentity {
            val user = if (id == farmer.userId) farmer else supplier
            return ParticipantIdentity(user.userId, user.displayName, user.role, points = user.points.toLong())
        }
        override fun profile(id: String) = flowOf(ParticipantProfile(
            (if (id == farmer.userId) farmer else supplier).let { ParticipantIdentity(it.userId, it.displayName, it.role) }, emptyList(), null))
    })
    private fun awaitText(text: String) {
        compose.waitUntil(10_000) { compose.onAllNodesWithText(text).fetchSemanticsNodes(atLeastOneRootRequired = false).isNotEmpty() }
    }
    private fun capture(name: String) {
        val cache = InstrumentationRegistry.getInstrumentation().targetContext.cacheDir
        File(cache, "discovery-$name-${viewer.name}-${if (dark) "dark" else "light"}.png").outputStream().use {
            compose.onRoot().captureToImage().asAndroidBitmap().compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it)
        }
    }
    @Test fun orderCardReusesCounterpartIdentityAndAuthorizedPhone() {
        val user = counterpart
        var identityId = ""
        var phoneId = ""
        var dialed: android.content.Intent? = null
        val vm = OrderParticipantViewModel(object : ParticipantRepository {
            override suspend fun identity(id: String): ParticipantIdentity {
                identityId = id
                return ParticipantIdentity(id, user.displayName, user.role, points = user.points.toLong())
            }
            override fun profile(id: String) = error("Full profile should not load for a preview")
        })
        compose.setContent { Theme {
            val base = androidx.compose.ui.platform.LocalContext.current
            val capturedContext = remember(base) {
                object : android.content.ContextWrapper(base) {
                    override fun startActivity(intent: android.content.Intent) { dialed = intent }
                }
            }
            CompositionLocalProvider(androidx.compose.ui.platform.LocalContext provides capturedContext,
                LocalContactLookup provides { id -> phoneId = id; user.displayName to "071 234 5678" }) {
                OrderParticipantCard(user.userId, if (user.role == UserRole.FARMER) "Farmer" else "Supplier", rememberNavController(), viewModel = vm)
            }
        } }
        compose.onNodeWithText(user.displayName).assertIsDisplayed()
        compose.onNodeWithText("071 234 5678", substring = true).assertIsDisplayed()
        compose.onNodeWithText("Call").assertIsDisplayed()
        compose.runOnIdle { assertEquals(user.userId, identityId); assertEquals(user.userId, phoneId) }
        capture("order-card")
        compose.onNodeWithText("Call").performClick()
        compose.runOnIdle {
            assertEquals(android.content.Intent.ACTION_DIAL, dialed?.action)
            assertEquals("tel", dialed?.data?.scheme)
            assertEquals("071 234 5678", dialed?.data?.schemeSpecificPart)
        }
        // Capture startActivity only inside this test; no external app or call is launched.
    }
    @Test fun marketplaceFarmerIdentityOpensProfileWithoutChangingListingAction() {
        var selected = ""
        var listingOpened = false
        val listing = HarvestListing(listingId = "listing", farmerId = farmer.userId, farmerName = farmer.displayName,
            cropName = "Tomatoes", quantity = 10.0, availableQuantity = 10.0, reservePrice = 100.0)
        compose.setContent { Theme {
            CompositionLocalProvider(LocalContactLookup provides { farmer.displayName to null }) {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    com.agroroute.features.supplier.marketplace.MarketplaceListingCard(listing, true, false,
                        onFarmerProfile = { selected = listing.farmerId }, onCardClick = { listingOpened = true })
                }
            }
        } }
        compose.onNodeWithText("by ${farmer.displayName}").performScrollTo().performClick()
        compose.runOnIdle { assertEquals(farmer.userId, selected); assertFalse(listingOpened) }
        compose.onNodeWithText("View / Bid").performScrollTo().performClick()
        compose.runOnIdle { assertTrue(listingOpened) }
    }
    @Test fun bothHomePreviewsNavigateToEitherRoleAndBack() {
        val vm = profileVm()
        var selected = ""
        compose.setContent { Theme {
            val nav = rememberNavController()
            NavHost(nav, startDestination = "home") {
                composable("home") {
                    Column(Modifier.verticalScroll(rememberScrollState())) {
                        Text("Home", Modifier.testTag("home-origin"))
                        CommunityLeadersContent(CommunityLeadersState(listOf(farmer), listOf(supplier), false), {}, { id ->
                            selected = id; nav.navigate(ParticipantRoutes.profile(id))
                        })
                    }
                }
                composable(ParticipantRoutes.PROFILE) { entry -> ParticipantProfileScreen(entry.arguments!!.getString("participantId")!!, nav, vm) }
            }
        } }
        capture("home-leaders")
        compose.onNodeWithText("Top Farmers").assertExists()
        compose.onNodeWithText("Top Suppliers").performScrollTo().assertIsDisplayed()
        for (user in listOf(farmer, supplier)) {
            compose.onNodeWithText(user.displayName).performScrollTo().performClick()
            awaitText("Participant profile")
            awaitText(user.displayName)
            compose.onNodeWithText("Participant profile").assertIsDisplayed()
            compose.onNodeWithText(user.displayName).assertIsDisplayed()
            compose.runOnIdle { assertEquals(user.userId, selected) }
            compose.onNodeWithText("Edit profile").assertDoesNotExist()
            compose.onNodeWithContentDescription("Back").performClick()
            compose.onNodeWithTag("home-origin").assertExists()
        }
    }
    @Test fun fullLeaderboardSwitchesRolesAndReturnsToSelectedTab() {
        val vm = profileVm()
        var selected by mutableStateOf("FARMER")
        compose.setContent { Theme {
            val nav = rememberNavController()
            NavHost(nav, startDestination = "leaders") {
                composable("leaders") {
                    val rows = if (selected == "FARMER") listOf(farmer) else listOf(supplier)
                    LeaderboardContent(selected, LeaderboardUiState(items = rows.map { LeaderboardUserItem(it) }),
                        { selected = it }, { nav.navigate(ParticipantRoutes.profile(it)) }, {})
                }
                composable(ParticipantRoutes.PROFILE) { entry -> ParticipantProfileScreen(entry.arguments!!.getString("participantId")!!, nav, vm) }
            }
        } }
        compose.onNodeWithText(farmer.displayName).assertIsDisplayed()
        compose.onNodeWithText(supplier.displayName).assertDoesNotExist()
        compose.onNodeWithText("Suppliers").performClick()
        compose.onNodeWithText(supplier.displayName).performClick()
        compose.onNodeWithText("Participant profile").assertIsDisplayed()
        compose.onNodeWithContentDescription("Back").performClick()
        compose.onNodeWithText(supplier.displayName).assertIsDisplayed()
        compose.onNodeWithText(farmer.displayName).assertDoesNotExist()
        compose.onNodeWithText(supplier.userId).assertDoesNotExist()
    }
    @Test fun counterpartIdentityAndMessageUseExistingRoutesAndPreserveOrder() {
        val vm = profileVm()
        var messageTarget = ""
        val user = counterpart
        val identity = ParticipantIdentity(user.userId, user.displayName, user.role, points = user.points.toLong())
        compose.setContent { Theme {
            val nav = rememberNavController()
            NavHost(nav, startDestination = "order/unchanged") {
                composable("order/unchanged") {
                    Column {
                        Text("Historical order unchanged")
                        ParticipantIdentityCard(ParticipantCardState(identity, false), user.role.name.lowercase().replaceFirstChar { it.titlecase() },
                            { nav.navigate(ParticipantRoutes.profile(user.userId)) }, { nav.navigate(ParticipantRoutes.chat(user.userId)) })
                    }
                }
                composable(ParticipantRoutes.PROFILE) { entry -> ParticipantProfileScreen(entry.arguments!!.getString("participantId")!!, nav, vm) }
                composable(ParticipantRoutes.CHAT) { entry ->
                    messageTarget = entry.arguments!!.getString("participantId")!!
                    Column { Text("Existing chat route"); TextButton(onClick = { nav.popBackStack() }) { Text("Return to order") } }
                }
            }
        } }
        compose.onNodeWithText(user.displayName).performClick()
        compose.onNodeWithText("Participant profile").assertIsDisplayed()
        compose.onNodeWithContentDescription("Back").performClick()
        compose.onNodeWithText("Historical order unchanged").assertIsDisplayed()
        compose.onNodeWithText(if (user.role == UserRole.FARMER) "NP" else "KS").performClick()
        compose.onNodeWithText("Participant profile").assertIsDisplayed()
        compose.onNodeWithContentDescription("Back").performClick()
        compose.onNodeWithText("View Profile").performClick()
        compose.onNodeWithContentDescription("Back").performClick()
        compose.onNodeWithText("Message").performClick()
        compose.onNodeWithText("Existing chat route").assertIsDisplayed()
        compose.runOnIdle { assertEquals(user.userId, messageTarget) }
        compose.onNodeWithText("Return to order").performClick()
        compose.onNodeWithText("Historical order unchanged").assertIsDisplayed()
    }
    @Test fun missingParticipantDoesNotHideHistoricalOrderOrEnableChat() {
        compose.setContent { Theme {
            Column {
                Text("Historical order unchanged")
                ParticipantIdentityCard(ParticipantCardState(loading = false), "Participant", {}, {})
            }
        } }
        compose.onNodeWithText("Participant unavailable").assertIsDisplayed()
        compose.onNodeWithText("Historical order unchanged").assertIsDisplayed()
        compose.onNodeWithText("Message").assertIsNotEnabled()
        compose.onNodeWithText("Retry participant").assertIsDisplayed()
    }
    @Test fun viewAllUsesCorrectRoleAndHomeOnlyShowsTopThree() {
        var target = ""
        val farmers = (1..4).map { farmer.copy(userId = "f$it", displayName = "Grower $it", points = 100 - it) }
        compose.setContent { Theme {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                CommunityLeadersContent(CommunityLeadersState(farmers, listOf(supplier), false), { target = it }, {})
            }
        } }
        compose.onNodeWithText("Grower 4").assertDoesNotExist()
        compose.onNodeWithText("View All Farmers").performScrollTo().performClick()
        compose.runOnIdle { assertEquals("FARMER", target) }
        compose.onNodeWithText("View All Suppliers").performScrollTo().performClick()
        compose.runOnIdle { assertEquals("SUPPLIER", target) }
    }
    @Test fun longParticipantNameKeepsAvatarRoleAndProfileActionReadable() {
        var opened = false
        val longName = "Nimal Perera Jayawardena Wickramasinghe With An Exceptionally Long Name"
        compose.setContent { Theme {
            Box(Modifier.width(300.dp)) {
                ParticipantIdentityRow(longName, "Farmer", { opened = true })
            }
        } }
        compose.onNodeWithText(longName).assertIsDisplayed()
        compose.onNodeWithText("Farmer", substring = false).assertIsDisplayed()
        compose.onNodeWithText("View Profile").assertIsDisplayed().performClick()
        compose.runOnIdle { assertTrue(opened) }
        capture("long-name")
    }

}
