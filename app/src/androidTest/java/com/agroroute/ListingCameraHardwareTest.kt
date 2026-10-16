package com.agroroute

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiSelector
import com.agroroute.core.theme.AgroRouteTheme
import com.agroroute.features.farmer.listing.ListingPhotosInput
import org.junit.Assert.*
import org.junit.FixMethodOrder
import org.junit.Rule
import org.junit.Test
import org.junit.runners.MethodSorters

/** Real permission UI, CameraX and Pixel hardware. No remote listing writes or uploads. */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class ListingCameraHardwareTest {
    @get:Rule val compose = createComposeRule()
    private val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
    private var selected = emptyList<String>()
    private fun form() {
        compose.setContent {
            AgroRouteTheme {
                var photos by remember { mutableStateOf(emptyList<String>()) }
                Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                    Text("Selected photos: ${photos.size}")
                    ListingPhotosInput(photos, false, false, true, { photos = it; selected = it }, {})
                }
            }
        }
    }
    private fun permissionButton(allow: Boolean) = device.findObject(UiSelector().resourceIdMatches(
        if (allow) ".*:id/permission_allow_foreground_only_button|.*:id/permission_allow_button"
        else ".*:id/permission_deny_button"))

    @Test fun a_permissionIsRequestedOnlyOnTapAndDenialKeepsGallery() {
        form()
        assertFalse(permissionButton(true).exists())
        compose.onNodeWithText("Take Photo").performScrollTo().performClick()
        val deny = permissionButton(false)
        assertTrue("Android CAMERA permission dialog must appear", deny.waitForExists(10_000))
        deny.click()
        compose.onNodeWithText("Camera permission is required to take a harvest photo.").assertExists()
        compose.onNodeWithText("Choose from Gallery").performScrollTo().assertIsEnabled()
        compose.runOnIdle { assertTrue(selected.isEmpty()) }
    }

    @Test fun b_grantRearCameraCaptureRetakeUseAndSecondCapture() {
        form()
        compose.onNodeWithText("Take Photo").performScrollTo().performClick()
        val allow = permissionButton(true)
        assertTrue("Android CAMERA permission dialog must appear", allow.waitForExists(10_000))
        allow.click()
        captureAndReview()
        compose.onNodeWithText("Retake").performClick()
        captureAndReview()
        compose.onNodeWithText("Use Photo").performClick()
        compose.waitUntil(20_000) { selected.size == 1 }
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val first = Uri.parse(selected.first())
        assertEquals("content", first.scheme)
        context.contentResolver.openInputStream(first)!!.use {
            val bitmap = requireNotNull(BitmapFactory.decodeStream(it))
            assertNotNull(bitmap)
            assertTrue(maxOf(bitmap.width, bitmap.height) <= 2048)
            bitmap.recycle()
        }
        compose.onNodeWithText("Take Photo").performScrollTo().performClick()
        captureAndReview()
        compose.onNodeWithText("Use Photo").performClick()
        compose.waitUntil(20_000) { selected.size == 2 }
        assertNotEquals(selected[0], selected[1])
        compose.onNodeWithText("Primary").assertExists()
    }

    @Test fun c_cancelDoesNotAppendPhoto() {
        form()
        compose.onNodeWithText("Take Photo").performScrollTo().performClick()
        compose.waitUntil(20_000) {
            compose.onAllNodesWithContentDescription("Close camera").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithContentDescription("Close camera").performClick()
        compose.onNodeWithText("Selected photos: 0").assertExists()
        compose.runOnIdle { assertTrue(selected.isEmpty()) }
    }

    @Test fun d_permanentlyDeniedShowsSettingsAndKeepsGallery() {
        org.junit.Assume.assumeTrue("Requires externally configured permanent denial",
            InstrumentationRegistry.getArguments().getString("camera_permission_qa_state") == "permanent")
        form()
        compose.onNodeWithText("Take Photo").performScrollTo().performClick()
        compose.waitUntil(10_000) {
            compose.onAllNodesWithText("Camera access is disabled. Enable it from App Settings.")
                .fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText("Open Settings").performScrollTo().assertIsEnabled()
        compose.onNodeWithText("Choose from Gallery").performScrollTo().assertIsEnabled()
        compose.runOnIdle { assertTrue(selected.isEmpty()) }
    }

    private fun captureAndReview() {
        compose.waitUntil(20_000) {
            compose.onAllNodesWithContentDescription("Capture photo").fetchSemanticsNodes()
                .any { it.config.contains(androidx.compose.ui.semantics.SemanticsProperties.Disabled).not() }
        }
        // Wait for preview frames before driving the physical sensor shutter.
        device.waitForIdle()
        compose.onNodeWithContentDescription("Capture photo").performClick()
        compose.waitUntil(20_000) {
            compose.onAllNodesWithText("Use Photo").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithContentDescription("Captured harvest photo").assertExists()
    }
}
