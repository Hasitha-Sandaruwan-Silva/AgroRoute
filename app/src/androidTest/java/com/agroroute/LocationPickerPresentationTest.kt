package com.agroroute

import android.graphics.Bitmap
import java.io.File
import androidx.test.platform.app.InstrumentationRegistry
import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.agroroute.core.components.LocationPickerDialog
import com.agroroute.core.theme.AgroRouteTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class LocationPickerPresentationTest(private val dark: Boolean) {
    @get:Rule val compose = createComposeRule()
    companion object {
        @JvmStatic @Parameterized.Parameters(name = "dark={0}")
        fun configurations() = listOf(arrayOf(false), arrayOf(true))
    }

    @Test fun keyboardLeavesConfirmationReachableAndBackDismisses() {
        var visible by mutableStateOf(true)
        var dismissed = false
        compose.setContent {
            AgroRouteTheme(darkTheme = dark) {
                if (visible) LocationPickerDialog("Kandy\n[7.290600, 80.633700]",
                    onDismiss = { visible = false; dismissed = true }, onConfirm = {})
            }
        }
        capture("before-keyboard")
        compose.onNodeWithText("Use this location").assertIsDisplayed().assertIsEnabled()
        compose.onNodeWithText("Search a place or address").performClick().performTextInput("Kandy")
        compose.waitForIdle()
        capture("keyboard")
        compose.onNodeWithText("Use this location").assertIsDisplayed().assertIsEnabled()
        compose.onNodeWithContentDescription("Go to my current location").assertIsDisplayed()
        compose.onNodeWithContentDescription("Back").performClick()
        compose.runOnIdle { assertTrue(dismissed) }
        compose.onNodeWithText("Use this location").assertDoesNotExist()
    }
    private fun capture(stage: String) {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        File(instrumentation.targetContext.cacheDir, "location-${if (dark) "dark" else "light"}-$stage.png").outputStream().use {
            instrumentation.uiAutomation.takeScreenshot().compress(Bitmap.CompressFormat.PNG, 100, it)
        }
    }
}
