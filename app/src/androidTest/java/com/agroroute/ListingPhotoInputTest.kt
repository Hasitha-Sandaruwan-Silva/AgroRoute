package com.agroroute

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.ExifInterface
import android.net.Uri
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.ActivityResultRegistryOwner
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityOptionsCompat
import androidx.test.platform.app.InstrumentationRegistry
import com.agroroute.core.theme.AgroRouteTheme
import com.agroroute.features.farmer.listing.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.io.File

/** System activity contracts are injected; no camera app, upload or listing writes. */
@RunWith(Parameterized::class)
class ListingPhotoInputTest(private val dark: Boolean) {
    @get:Rule val compose = createComposeRule()
    companion object {
        @JvmStatic @Parameterized.Parameters(name = "dark={0}")
        fun themes() = listOf(arrayOf(false), arrayOf(true))
    }
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private var lastUri: Uri? = null
    private var selections = 0
    private var busy = false
    private val registry = object : ActivityResultRegistry() {
        var galleryUri: Uri? = null
        var cancelled = false
        var invalid = false
        var unavailable = false
        var launches = 0
        override fun <I, O> onLaunch(requestCode: Int, contract: ActivityResultContract<I, O>, input: I,
            options: ActivityOptionsCompat?) {
            launches++
            if (unavailable) throw ActivityNotFoundException()
            if (contract is ActivityResultContracts.TakePicture) {
                if (!cancelled && !invalid) writePhoto(input as Uri)
                dispatchResult(requestCode, !cancelled)
            } else dispatchResult(requestCode, galleryUri)
        }
    }
    private fun writePhoto(uri: Uri) {
        val bitmap = Bitmap.createBitmap(120, 80, Bitmap.Config.ARGB_8888).apply { eraseColor(0xFF36754A.toInt()) }
        context.contentResolver.openOutputStream(uri)!!.use { bitmap.compress(Bitmap.CompressFormat.JPEG, 95, it) }
        bitmap.recycle()
    }
    private fun galleryPhoto(): Uri = ListingCameraPhoto.uri(context, ListingCameraPhoto.create(context)).also(::writePhoto)
    @Composable private fun form(enabled: Boolean = true) {
        CompositionLocalProvider(LocalActivityResultRegistryOwner provides object : ActivityResultRegistryOwner {
            override val activityResultRegistry = registry
        }) {
            AgroRouteTheme(darkTheme = dark) {
                var photo by rememberSaveable(stateSaver = ListingPhotoUriSaver) { mutableStateOf<Uri?>(null) }
                var description by rememberSaveable { mutableStateOf("Keep harvest details") }
                Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
                    OutlinedTextField(description, { description = it }, label = { Text("Description fixture") })
                    ListingPhotoInput(photo, false, false, enabled, onImageSelected = {
                        photo = it; lastUri = it; selections++
                    }, onRemove = { photo = null; lastUri = null }, onBusyChanged = { busy = it })
                }
                }
            }
        }
    }
    private fun content(enabled: Boolean = true) = compose.setContent { form(enabled) }
    private fun awaitSelection() = compose.waitUntil(10_000) { selections > 0 && !busy }
    private fun screenshot(name: String, anchor: String = "Description fixture") {
        compose.mainClock.advanceTimeBy(1_000)
        compose.waitForIdle()
        // ModalBottomSheet owns a separate window; capture that window through UiAutomation.
        val bitmap = if (name == "sheet") InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()
            else compose.onNode(isRoot() and hasAnyDescendant(hasText(anchor))).captureToImage().asAndroidBitmap()
        File(context.cacheDir, "photo-${if (dark) "dark" else "light"}-$name.png").outputStream().use {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
        }
    }

    @Test fun sourceSheetContainsCameraGalleryAndCancel() {
        content()
        screenshot("empty")
        compose.onNodeWithTag("listing-photo-preview").performClick()
        compose.onNodeWithText("Add harvest photo").assertIsDisplayed()
        compose.onAllNodesWithText("Take Photo").onLast().assertIsDisplayed()
        compose.onAllNodesWithText("Choose from Gallery").onLast().assertIsDisplayed()
        screenshot("sheet", "Add harvest photo")
        compose.onNodeWithText("Cancel").performClick()
        compose.onNodeWithText("Add harvest photo").assertDoesNotExist()
        compose.runOnIdle { assertEquals(0, registry.launches) }
    }

    @Test fun galleryUpdatesPreviewReplacementAndRemoval() {
        registry.galleryUri = galleryPhoto()
        content()
        compose.onNodeWithText("Choose from Gallery").performScrollTo().performClick()
        awaitSelection()
        compose.onNodeWithContentDescription("Selected listing photo").assertExists()
        val first = lastUri
        compose.onNodeWithText("Replace Photo").performScrollTo().performClick()
        registry.galleryUri = galleryPhoto()
        compose.onAllNodesWithText("Choose from Gallery").onLast().performClick()
        compose.waitUntil { selections == 2 }
        assertNotEquals(first, lastUri)
        screenshot("preview")
        compose.onNodeWithText("Remove Photo").performScrollTo().performClick()
        compose.runOnIdle { assertNull(lastUri) }
        compose.onNodeWithText("Add a clear harvest photo").assertExists()
    }

    @Test fun cameraResultIsPreparedAsJpegContentUriAndPreviewed() {
        content()
        compose.onNodeWithText("Take Photo").performScrollTo().performClick()
        awaitSelection()
        compose.runOnIdle {
            assertEquals("content", lastUri!!.scheme)
            assertEquals("${context.packageName}.listing.photos", lastUri!!.authority)
            assertEquals("image/jpeg", context.contentResolver.getType(lastUri!!))
            assertEquals(1, registry.launches)
        }
        compose.onNodeWithContentDescription("Selected listing photo").assertExists()
        screenshot("camera")
    }

    @Test fun cancelledCameraKeepsExistingPhotoAndForm() {
        registry.galleryUri = galleryPhoto(); content()
        compose.onNodeWithText("Choose from Gallery").performScrollTo().performClick(); awaitSelection()
        val original = lastUri
        registry.cancelled = true
        compose.onNodeWithText("Replace Photo").performScrollTo().performClick()
        compose.onAllNodesWithText("Take Photo").onLast().performClick()
        compose.waitUntil { !busy }
        compose.runOnIdle { assertEquals(original, lastUri); assertEquals(1, selections) }
        compose.onNodeWithText("Keep harvest details").assertExists()
    }

    @Test fun invalidCameraOutputKeepsFormAndShowsReadableError() {
        registry.invalid = true; content()
        compose.onNodeWithText("Take Photo").performScrollTo().performClick()
        compose.waitUntil(10_000) {
            compose.onAllNodesWithText("Couldn’t read the captured photo. Please try again or choose from Gallery.")
                .fetchSemanticsNodes().isNotEmpty()
        }
        compose.runOnIdle { assertNull(lastUri); assertEquals(0, selections); assertFalse(busy) }
        compose.onNodeWithText("Keep harvest details").assertExists()
    }

    @Test fun unavailableCameraIsHandledAndDisabledInputCannotLaunch() {
        registry.unavailable = true; content()
        compose.onNodeWithText("Take Photo").performScrollTo().performClick()
        compose.onNodeWithText("Couldn’t open the camera. Please try again or choose from Gallery.").assertExists()
        compose.runOnIdle { assertFalse(busy); assertNull(lastUri) }
    }

    @Test fun disabledPhotoActionsRemainDisabled() {
        content(enabled = false)
        compose.onNodeWithText("Take Photo").performScrollTo().assertIsNotEnabled()
        compose.onNodeWithText("Choose from Gallery").performScrollTo().assertIsNotEnabled()
        compose.runOnIdle { assertEquals(0, registry.launches) }
    }

    @Test fun selectedPhotoSurvivesSavedStateRestorationAndOtherFieldEdits() {
        val restore = StateRestorationTester(compose)
        restore.setContent { form() }
        compose.onNodeWithText("Take Photo").performScrollTo().performClick(); awaitSelection()
        val original = lastUri
        compose.onNode(hasSetTextAction()).performScrollTo().performTextReplacement("Changed details")
        restore.emulateSavedInstanceStateRestore()
        compose.onNodeWithContentDescription("Selected listing photo").assertExists()
        compose.onNodeWithText("Changed details").assertExists()
        compose.runOnIdle { assertEquals(original, lastUri); assertEquals(1, selections) }
    }

    @Test fun cameraPreparationCorrectsExifAndKeepsExistingSizeLimit() = runBlocking {
        val name = ListingCameraPhoto.create(context)
        val uri = ListingCameraPhoto.uri(context, name)
        writePhoto(uri)
        val file = File(context.filesDir, "listing_captures/$name")
        ExifInterface(file.path).apply {
            setAttribute(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_ROTATE_90.toString()); saveAttributes()
        }
        val prepared = ListingCameraPhoto.prepare(context, name)
        val bitmap = context.contentResolver.openInputStream(prepared)!!.use { BitmapFactory.decodeStream(it) }
        assertEquals(80, bitmap.width); assertEquals(120, bitmap.height)
        assertTrue(file.length() in 1..com.agroroute.core.util.CloudinaryConfig.MAX_PHOTO_BYTES)
        bitmap.recycle(); ListingCameraPhoto.discard(context, name)
    }
}
