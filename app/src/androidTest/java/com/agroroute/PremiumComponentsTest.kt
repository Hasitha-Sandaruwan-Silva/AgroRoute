package com.agroroute

import android.graphics.Bitmap
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import com.agroroute.core.components.*
import com.agroroute.core.navigation.Screen
import com.agroroute.core.theme.AgroRouteTheme
import com.github.thelacspace.icons.Lucide
import java.io.File
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

/** UI fixtures are isolated components, never written into app/business data. */
@RunWith(Parameterized::class)
class PremiumComponentsTest(private val dark: Boolean, private val width: Int) {
    @get:Rule val compose = createComposeRule()

    companion object {
        @JvmStatic @Parameterized.Parameters(name = "dark={0}, width={1}")
        fun configurations() = listOf(arrayOf(false, 320), arrayOf(true, 320), arrayOf(false, 384), arrayOf(true, 384))
    }

    @Test fun dockPreservesDestinationsAndAccessibleTargets() {
        var route by mutableStateOf(Screen.SupplierHome.route)
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, 1.3f)) {
                AgroRouteTheme(darkTheme = dark) {
                    Column(Modifier.width(width.dp).testTag("premium-components")) {
                        PremiumHero { Text("AgroRoute", style = MaterialTheme.typography.headlineSmall); Text("Harvest and trade, connected.") }
                        UserProfileAvatar("AgroRoute Member", null, size = 72.dp)
                        AnimatedBottomBar(listOf(
                            BottomNavItem(Screen.SupplierHome.route, "Home", Lucide.Home),
                            BottomNavItem(Screen.SupplierMarketplace.route, "Marketplace", Lucide.ShoppingBag),
                            BottomNavItem(Screen.SupplierOrders.route, "Orders", Lucide.Receipt),
                            BottomNavItem(Screen.SupplierProfile.route, "Profile", Lucide.User, profileName = "AgroRoute Member")
                        ), selectedRoute = route, onItemSelected = { route = it.route })
                    }
                }
            }
        }
        // The generic dock renders a profile icon; initials belong to the standalone avatar.
        compose.onAllNodesWithText("AM", useUnmergedTree = true).assertCountEquals(1)
        compose.onNodeWithContentDescription("Profile").assertExists()
        listOf("Home", "Marketplace", "Orders", "Profile").forEach { label ->
            compose.onNodeWithText(label).assertIsDisplayed().assertHeightIsAtLeast(48.dp).performClick()
        }
        compose.runOnIdle { assertEquals(Screen.SupplierProfile.route, route) }
        val bitmap = compose.onNodeWithTag("premium-components").captureToImage().asAndroidBitmap()
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        File(context.cacheDir, "premium-${width}-${if (dark) "dark" else "light"}.png").outputStream().use {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
        }
    }
}
