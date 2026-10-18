package com.agroroute

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.agroroute.core.components.AnimatedBottomBar
import com.agroroute.core.components.BottomNavItem
import com.agroroute.core.navigation.Screen
import com.agroroute.core.theme.AgroRouteTheme
import com.agroroute.features.farmer.components.FarmerPresentation
import com.github.thelacspace.icons.Lucide
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

/** Synthetic system insets exercise both navigation modes without changing the user's phone. */
@RunWith(Parameterized::class)
class FarmerNavigationPresentationTest(private val dark: Boolean, private val width: Int, private val font: Float) {
    @get:Rule val compose = createComposeRule()
    companion object {
        @JvmStatic @Parameterized.Parameters(name = "dark={0},width={1},font={2}")
        fun configurations() = listOf(false, true).flatMap { dark ->
            listOf(320, 384).flatMap { width -> listOf(1f, 1.6f).map { arrayOf(dark, width, it) } }
        }
    }
    @Test fun originalTabsRemainReadableReachableAndAboveBothNavigationInsets() {
        var inset by mutableIntStateOf(24)
        var route by mutableStateOf(Screen.FarmerHome.route)
        val tabs = listOf(
            BottomNavItem(Screen.FarmerHome.route, "Home", Lucide.Home),
            BottomNavItem(Screen.MyListings.route, "Listings", Lucide.FileText),
            BottomNavItem(Screen.FarmerOrders.route, "Orders", Lucide.Receipt),
            BottomNavItem(Screen.FarmerProfile.route, "Profile", Lucide.User)
        )
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, font)) {
                AgroRouteTheme(darkTheme = dark) { FarmerPresentation {
                    Box(Modifier.size(width.dp, 640.dp).background(MaterialTheme.colorScheme.background)
                        .testTag("farmer-nav-viewport")) {
                        Box(Modifier.fillMaxSize().consumeWindowInsets(WindowInsets.navigationBars)
                            .padding(bottom = inset.dp)) {
                            AnimatedBottomBar(tabs, route, { route = it.route }, Modifier.align(Alignment.BottomCenter))
                        }
                    }
                } }
            }
        }
        for (bottom in listOf(24, 48)) {
            compose.runOnIdle { inset = bottom }
            val viewport = compose.onNodeWithTag("farmer-nav-viewport").fetchSemanticsNode().boundsInRoot
            val pixelsPerDp = viewport.width / width
            var previousRight = viewport.left
            for (tab in tabs) {
                val node = compose.onNodeWithText(tab.label, substring = false)
                node.assertIsDisplayed().assertHasClickAction().performClick().assertIsSelected()
                val bounds = node.fetchSemanticsNode().boundsInRoot
                assertTrue("48 dp touch target", bounds.height >= 48f * pixelsPerDp - 1f)
                assertTrue("Preserved tab order and no overlap", bounds.left >= previousRight)
                assertTrue("Above system navigation", bounds.bottom <= viewport.bottom - bottom * pixelsPerDp + 1f)
                previousRight = bounds.right
                node.performSemanticsAction(SemanticsActions.GetTextLayoutResult) { action ->
                    val results = mutableListOf<TextLayoutResult>()
                    action(results)
                    assertTrue("Label layout is available", results.isNotEmpty())
                    // A paragraph may retain maxWidth while Text sizes to its shorter ink width.
                    // Check actual line bounds and ellipsis instead of paragraph-box overflow.
                    val diagnostic = results.joinToString { "size=${it.size}, line=${it.getLineLeft(0)}..${it.getLineRight(0)}, heightOverflow=${it.didOverflowHeight}" }
                    assertFalse("Complete ${tab.label} label ($diagnostic)", results.any {
                        it.lineCount != 1 || it.isLineEllipsized(0) || it.didOverflowHeight ||
                            it.getLineRight(0) - it.getLineLeft(0) > it.size.width + 1f
                    })
                }
                compose.runOnIdle { assertEquals(tab.route, route) }
            }
        }
    }
}
