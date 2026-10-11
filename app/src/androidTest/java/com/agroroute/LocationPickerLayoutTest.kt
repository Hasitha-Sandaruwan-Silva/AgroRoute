package com.agroroute

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.agroroute.core.components.LocationPickerLayout
import com.agroroute.core.theme.AgroRouteTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

/** Exercise the production layout in bounded viewports without changing device settings. */
@RunWith(Parameterized::class)
class LocationPickerLayoutTest(
    private val dark: Boolean, private val width: Int, private val height: Int, private val fontScale: Float
) {
    @get:Rule val compose = createComposeRule()

    companion object {
        @JvmStatic @Parameterized.Parameters(name = "dark={0}, {1}x{2}, font={3}")
        fun configurations() = listOf(false, true).flatMap { dark ->
            listOf(arrayOf(dark, 320, 480, 1f), arrayOf(dark, 384, 640, 1f),
                arrayOf(dark, 384, 820, 1f), arrayOf(dark, 640, 320, 1f),
                arrayOf(dark, 320, 480, 1.3f))
        }
    }

    @Test fun gestureAndThreeButtonNavigationKeepActionInsideSafeViewport() {
        var navigationHeight by mutableIntStateOf(24)
        var confirmed = false
        content(bottom = { navigationHeight }, onConfirm = { confirmed = true })
        assertLayout(navigationHeight)
        compose.runOnIdle { navigationHeight = 48 }
        assertLayout(navigationHeight)
        compose.onNodeWithText("Use this location").performClick()
        compose.runOnIdle { assertTrue(confirmed) }
    }

    @Test fun imeResizesContentWhileActionSearchAndMapStayReachable() {
        val keyboardHeight = if (width > height) 100 else 260
        var bottom by mutableIntStateOf(24)
        var mapCreations = 0
        content(bottom = { bottom }, onMapCreated = { mapCreations++ })
        assertLayout(24)
        compose.runOnIdle { bottom = keyboardHeight }
        assertLayout(keyboardHeight)
        compose.runOnIdle { assertEquals("Resize preserves the existing map", 1, mapCreations) }
        compose.onNodeWithTag("search").performTextReplacement("Kandy")
        compose.onNodeWithTag("search").assertTextContains("Kandy")
        compose.onNodeWithText("Your selected location").assertIsDisplayed()
        compose.onNodeWithTag("address").performScrollTo().performTextReplacement("Saved place")
        compose.onNodeWithTag("address").assertTextContains("Saved place")
        compose.onNodeWithText("Use this location").assertIsDisplayed().performClick()
    }

    private fun content(bottom: () -> Int, onConfirm: () -> Unit = {}, onMapCreated: () -> Unit = {}) {
        compose.setContent {
            BoxWithConstraints(Modifier.fillMaxSize()) {
                val density = LocalDensity.current
                val scale = minOf(maxWidth.value / width, maxHeight.value / height, 1f)
                CompositionLocalProvider(LocalDensity provides Density(density.density * scale, fontScale)) {
                    AgroRouteTheme(darkTheme = dark) {
                        Box(Modifier.size(width.dp, height.dp).testTag("viewport")) {
                            var query by remember { mutableStateOf("") }
                            var address by remember { mutableStateOf("Saved place") }
                            LocationPickerLayout(
                                insets = WindowInsets(left = 0.dp, top = 24.dp, right = 0.dp, bottom = bottom().dp),
                                search = { OutlinedTextField(query, { query = it }, singleLine = true,
                                    placeholder = { Text("Search a place or address") }, modifier = Modifier.fillMaxWidth().testTag("search")) },
                                map = {
                                    DisposableEffect(Unit) { onMapCreated(); onDispose {} }
                                    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceVariant).testTag("map"))
                                },
                                address = {
                                    Text("Your selected location", style = MaterialTheme.typography.labelLarge)
                                    OutlinedTextField(address, { address = it }, maxLines = 2,
                                        modifier = Modifier.fillMaxWidth().testTag("address"))
                                    Text("7.290600, 80.633700", style = MaterialTheme.typography.labelSmall)
                                },
                                confirm = { Button(onClick = onConfirm,
                                    modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) { Text("Use this location") } }
                            )
                        }
                    }
                }
            }
        }
    }

    private fun assertLayout(bottom: Int) {
        compose.onNodeWithText("Use this location").assertIsDisplayed().assertIsEnabled()
        compose.onNodeWithTag("search").assertIsDisplayed()
        compose.onNodeWithTag("map").assertIsDisplayed()
        val viewport = compose.onNodeWithTag("viewport").fetchSemanticsNode().boundsInRoot
        val action = compose.onNode(hasClickAction() and hasText("Use this location")).fetchSemanticsNode().boundsInRoot
        val map = compose.onNodeWithTag("map").fetchSemanticsNode().boundsInRoot
        val search = compose.onNodeWithTag("search").fetchSemanticsNode().boundsInRoot
        val dpPixels = viewport.height / height
        assertTrue("Entire action above bottom inset", action.bottom <= viewport.bottom - bottom * dpPixels + 1f)
        assertTrue("Entire action inside viewport", action.left >= viewport.left && action.right <= viewport.right)
        assertTrue("Search below status/cutout inset", search.top >= viewport.top + 24 * dpPixels - 1f)
        assertTrue("Map has usable area", map.width > 0 && map.height > 0)
        assertTrue("Map does not overlap action", map.bottom <= action.top)
        assertTrue("Search does not overlap map", search.bottom <= map.top)
    }
}
