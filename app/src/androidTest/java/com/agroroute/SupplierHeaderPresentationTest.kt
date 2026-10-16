package com.agroroute

import android.graphics.Bitmap
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.agroroute.core.database.AppDatabase
import com.agroroute.core.runtime.*
import com.agroroute.core.theme.AgroRouteTheme
import com.agroroute.demo.DemoSessionManager
import org.junit.*
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.io.File

/** Real supplier graph backed by an isolated in-memory demo database. */
@RunWith(Parameterized::class)
class SupplierHeaderPresentationTest(private val dark: Boolean) {
    @get:Rule val compose = createComposeRule()
    companion object {
        @JvmStatic @Parameterized.Parameters(name = "dark={0}")
        fun themes() = listOf(arrayOf(false), arrayOf(true))
    }
    private var activeDark by mutableStateOf(dark)
    private lateinit var db: AppDatabase
    private lateinit var session: DemoSessionManager
    @Before fun open() {
        db = Room.inMemoryDatabaseBuilder(InstrumentationRegistry.getInstrumentation().targetContext, AppDatabase::class.java).build()
        session = DemoSessionManager().apply { enter() }
        compose.setContent { AgroRouteTheme(darkTheme = activeDark) { DemoExperience(session, db) { } } }
        compose.waitUntil(20_000) {
            compose.onAllNodesWithText("Continue as Supplier").fetchSemanticsNodes().firstOrNull()?.config
                ?.contains(androidx.compose.ui.semantics.SemanticsProperties.Disabled) == false
        }
        compose.onNodeWithText("Continue as Supplier").performClick()
        compose.waitUntil(20_000) { compose.onAllNodesWithTag("supplier-home-hero").fetchSemanticsNodes().isNotEmpty() }
    }
    @After fun close() {
        compose.runOnIdle { session.exit() }; compose.waitForIdle()
        BackgroundNetworkAccess.resume(); db.close()
    }
    private fun capture(name: String) {
        File(InstrumentationRegistry.getInstrumentation().targetContext.cacheDir,
            "supplier-header-${if (dark) "dark" else "light"}-$name.png").outputStream().use {
            compose.onRoot().captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, it)
        }
    }
    private fun tab(name: String) { compose.onAllNodesWithText(name, substring = false).onLast().performClick(); compose.waitForIdle() }

    @Test fun homeHasOneHeaderAndOtherRootTabsRetainSharedBar() {
        compose.onNodeWithTag("supplier-shared-top-bar").assertDoesNotExist()
        compose.onNodeWithTag("supplier-home-hero").assertIsDisplayed()
        capture("home")
        for (name in listOf("Marketplace", "Orders", "Profile")) {
            tab(name)
            compose.onNodeWithTag("supplier-shared-top-bar").assertIsDisplayed()
            compose.onNodeWithText("AgroRoute · Supplier").assertExists()
            capture(name.lowercase())
            tab("Home")
            compose.onNodeWithTag("supplier-shared-top-bar").assertDoesNotExist()
            compose.onNodeWithTag("supplier-home-hero").assertIsDisplayed()
        }
    }

    @Test fun homeNotificationAndProfileCallbacksRemainAvailable() {
        compose.onAllNodes(hasContentDescription("Notifications", substring = true)).onFirst().performClick()
        compose.onNodeWithText("Notifications", substring = false).assertIsDisplayed()
        compose.onNodeWithText("View all notifications").performClick()
        compose.onNodeWithContentDescription("Back").performClick()
        compose.onNodeWithText("Supplier", substring = false).performClick()
        compose.waitUntil(10_000) { compose.onAllNodesWithTag("supplier-shared-top-bar").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("AgroRoute · Supplier").assertExists()
    }
    @Test fun sharedHeaderAndHeroStatusIconsFollowLiveThemeSwitch() {
        tab("Marketplace")
        for (night in listOf(false, true, false)) {
            compose.runOnIdle { activeDark = night }
            compose.onNodeWithTag("supplier-shared-top-bar").assertIsDisplayed()
            compose.runOnIdle {
                val activity = androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry.getInstance()
                    .getActivitiesInStage(androidx.test.runner.lifecycle.Stage.RESUMED).single()
                Assert.assertEquals(!night, androidx.core.view.WindowCompat.getInsetsController(activity.window, activity.window.decorView).isAppearanceLightStatusBars)
            }
            capture("live-${if (night) "dark" else "light"}")
            tab("Home")
            compose.onNodeWithTag("supplier-shared-top-bar").assertDoesNotExist()
            compose.runOnIdle {
                val activity = androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry.getInstance()
                    .getActivitiesInStage(androidx.test.runner.lifecycle.Stage.RESUMED).single()
                Assert.assertFalse(androidx.core.view.WindowCompat.getInsetsController(activity.window, activity.window.decorView).isAppearanceLightStatusBars)
            }
            tab("Marketplace")
        }
    }

}
