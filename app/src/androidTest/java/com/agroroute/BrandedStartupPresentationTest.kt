package com.agroroute

import android.graphics.Bitmap
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.test.platform.app.InstrumentationRegistry
import com.agroroute.core.components.BrandedStartup
import com.agroroute.core.components.StartupArtwork
import com.agroroute.core.theme.AgroRouteTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import java.io.File

class BrandedStartupPresentationTest {
    @get:Rule val compose = createComposeRule()

    @Test fun realContentStartsImmediatelyAndSurvivesIntroWithoutRecreation() {
        var composed = 0
        var disposed = 0
        var ready by mutableStateOf(false)
        var nativeFinished by mutableStateOf(false)
        compose.mainClock.autoAdvance = false
        compose.setContent {
            AgroRouteTheme {
                BrandedStartup(ready, nativeFinished, false) {
                    DisposableEffect(Unit) { composed++; onDispose { disposed++ } }
                    Text("Existing destination")
                }
            }
        }
        compose.runOnIdle { assertEquals(1, composed); assertEquals(0, disposed) }
        compose.mainClock.advanceTimeBy(3000)
        compose.onNodeWithTag("startup-intro").assertExists()
        compose.runOnIdle { nativeFinished = true }
        compose.mainClock.advanceTimeBy(2800)
        compose.onNodeWithTag("startup-session-loading").assertIsDisplayed()
        compose.onNodeWithText("Existing destination").assertDoesNotExist()
        compose.runOnIdle { ready = true }
        compose.mainClock.advanceTimeBy(400)
        compose.onNodeWithTag("startup-intro").assertDoesNotExist()
        compose.onNodeWithText("Existing destination").assertIsDisplayed()
        compose.runOnIdle { assertEquals(1, composed); assertEquals(0, disposed) }
    }

    @Test fun readySessionRevealsExistingDestinationAfterAnimationAndDoesNotReplay() {
        var dark by mutableStateOf(false)
        compose.mainClock.autoAdvance = false
        compose.setContent {
            AgroRouteTheme(darkTheme = dark) {
                BrandedStartup(true, true, dark) { Text("Existing role destination") }
            }
        }
        compose.mainClock.advanceTimeBy(1800)
        compose.onNodeWithTag("startup-intro").assertExists()
        compose.onNodeWithTag("startup-session-loading").assertDoesNotExist()
        compose.mainClock.advanceTimeBy(1100)
        compose.onNodeWithTag("startup-intro").assertDoesNotExist()
        compose.onNodeWithText("Existing role destination").assertIsDisplayed()
        compose.runOnIdle { dark = true }
        compose.mainClock.advanceTimeBy(3000)
        compose.onNodeWithTag("startup-intro").assertDoesNotExist()
    }

    @Test fun captureLightDarkAnimationFrames() {
        var dark by mutableStateOf(false)
        var time by mutableFloatStateOf(0f)
        compose.setContent {
            AgroRouteTheme(darkTheme = dark) {
                Box(Modifier.fillMaxSize()) { StartupArtwork(time, 0f, dark, false) }
            }
        }
        for (night in listOf(false, true)) {
            for (frame in listOf(0, 700, 1450, 2150, 2400)) {
                compose.runOnIdle { dark = night; time = frame.toFloat() }
                val name = "startup-${if (night) "dark" else "light"}-$frame.png"
                File(InstrumentationRegistry.getInstrumentation().targetContext.cacheDir, name).outputStream().use {
                    compose.onRoot().captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, it)
                }
            }
        }
    }

    @Test fun completedIntroStaysCompletedAfterSavedStateRestoration() {
        compose.mainClock.autoAdvance = false
        val restoration = StateRestorationTester(compose)
        restoration.setContent {
            AgroRouteTheme { BrandedStartup(true, true, false) { Text("Restored destination") } }
        }
        compose.mainClock.advanceTimeBy(3000)
        compose.onNodeWithTag("startup-intro").assertDoesNotExist()
        restoration.emulateSavedInstanceStateRestore()
        compose.onNodeWithTag("startup-intro").assertDoesNotExist()
        compose.onNodeWithText("Restored destination").assertIsDisplayed()
    }

    @Test fun introStatusBarOverridesUnderlyingHeaderAndReleasesIt() {
        var showing by mutableStateOf(true)
        compose.setContent {
            AgroRouteTheme(darkTheme = false) {
                com.agroroute.core.theme.DarkHeaderStatusBar()
                if (showing) com.agroroute.core.theme.StartupStatusBar(false)
                Text("Status bar")
            }
        }
        fun lightStatusIcons(): Boolean {
            val activity = androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry.getInstance()
                .getActivitiesInStage(androidx.test.runner.lifecycle.Stage.RESUMED).single()
            return androidx.core.view.WindowCompat.getInsetsController(activity.window, activity.window.decorView)
                .isAppearanceLightStatusBars
        }
        compose.runOnIdle { assertEquals(true, lightStatusIcons()); showing = false }
        compose.waitForIdle()
        compose.runOnIdle { assertEquals(false, lightStatusIcons()) }
    }
}
