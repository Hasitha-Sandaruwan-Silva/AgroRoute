package com.agroroute

import android.graphics.Bitmap
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
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

    @Test fun officialArtworkFitsCompactAndMediumWithLargeText() {
        var dark by mutableStateOf(false)
        var width by mutableIntStateOf(320)
        var font by mutableFloatStateOf(1f)
        compose.setContent {
            val density = androidx.compose.ui.platform.LocalDensity.current
            CompositionLocalProvider(androidx.compose.ui.platform.LocalDensity provides
                androidx.compose.ui.unit.Density(density.density, font)) {
                AgroRouteTheme(darkTheme = dark) {
                    Box(Modifier.width(width.dp)) {
                        StartupArtwork(2150f, 0f, dark, false)
                    }
                }
            }
        }
        for (night in listOf(false, true)) for (size in listOf(320, 384)) for (scale in listOf(1f, 1.6f)) {
            compose.runOnIdle { dark = night; width = size; font = scale }
            compose.onNodeWithTag("startup-official-logo").assertIsDisplayed()
            val name = compose.onNodeWithTag("startup-name").assertIsDisplayed().fetchSemanticsNode().boundsInRoot
            val subtitle = compose.onNodeWithTag("startup-tagline").assertIsDisplayed().fetchSemanticsNode().boundsInRoot
            org.junit.Assert.assertTrue("Name and subtitle must not overlap", name.bottom <= subtitle.top)
            org.junit.Assert.assertTrue("Subtitle must fit the viewport", subtitle.bottom <= compose.onRoot().fetchSemanticsNode().boundsInRoot.bottom)
        }
    }

    @Test fun captureDeviceLauncherMaskOnBothBackgrounds() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val launcher = context.getSystemService(android.content.pm.LauncherApps::class.java)
        val activity = launcher.getActivityList(context.packageName, android.os.Process.myUserHandle()).first()
        val icon = activity.getIcon(context.resources.displayMetrics.densityDpi)
        val bitmap = icon.toBitmap(256, 256)
        val sheet = Bitmap.createBitmap(640, 360, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(sheet)
        val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG)
        for ((index, color) in listOf(0xFFF5F1E9.toInt(), 0xFF0E211B.toInt()).withIndex()) {
            paint.color = color
            canvas.drawRect(index * 320f, 0f, (index + 1) * 320f, 360f, paint)
            canvas.drawBitmap(bitmap, index * 320f + 32f, 40f, null)
        }
        File(context.cacheDir, "startup-device-launcher.png").outputStream().use {
            sheet.compress(Bitmap.CompressFormat.PNG, 100, it)
        }
        val pixels = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        org.junit.Assert.assertTrue("Original gold arrow must survive the device mask", pixels.count {
            android.graphics.Color.red(it) > 150 && android.graphics.Color.green(it) in 90..230 &&
                android.graphics.Color.blue(it) < 110
        } > 30)
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
