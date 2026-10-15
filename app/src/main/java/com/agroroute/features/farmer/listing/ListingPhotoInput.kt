@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.agroroute.features.farmer.listing

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.github.thelacspace.icons.Lucide
import kotlinx.coroutines.CancellationException

internal val ListingPhotoUriSaver = Saver<Uri?, String>(
    save = { it?.toString().orEmpty() }, restore = { it.takeIf(String::isNotBlank)?.let(Uri::parse) })

@Composable
internal fun ListingPhotoInput(
    selectedUri: Uri?, isLoading: Boolean, isEditing: Boolean, enabled: Boolean,
    onImageSelected: (Uri) -> Unit, onRemove: () -> Unit,
    onBusyChanged: (Boolean) -> Unit = {}
) {
    val context = LocalContext.current
    var sheet by rememberSaveable { mutableStateOf(false) }
    var pendingCapture by rememberSaveable { mutableStateOf<String?>(null) }
    var captureReady by rememberSaveable { mutableStateOf(false) }
    var message by rememberSaveable { mutableStateOf<String?>(null) }
    val latestSelected by rememberUpdatedState(onImageSelected)
    val latestBusy by rememberUpdatedState(onBusyChanged)
    val busy = pendingCapture != null
    SideEffect { latestBusy(busy) }
    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success && pendingCapture != null) captureReady = true
        else {
            pendingCapture?.let { ListingCameraPhoto.discard(context, it) }
            pendingCapture = null
            captureReady = false
        }
    }
    val gallery = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let {
            runCatching { context.contentResolver.takePersistableUriPermission(it,
                android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            latestSelected(it)
            message = null
        }
    }
    LaunchedEffect(pendingCapture, captureReady) {
        val name = pendingCapture ?: return@LaunchedEffect
        if (!captureReady) return@LaunchedEffect
        try {
            latestSelected(ListingCameraPhoto.prepare(context, name))
            message = null
        } catch (cancelled: CancellationException) { throw cancelled }
        catch (_: Exception) {
            ListingCameraPhoto.discard(context, name)
            message = "Couldn’t read the captured photo. Please try again or choose from Gallery."
        }
        pendingCapture = null
        captureReady = false
    }
    fun takePhoto() {
        if (!enabled || pendingCapture != null) return
        sheet = false
        try {
            val name = ListingCameraPhoto.create(context)
            pendingCapture = name
            captureReady = false
            camera.launch(ListingCameraPhoto.uri(context, name))
        } catch (_: Exception) {
            pendingCapture?.let { ListingCameraPhoto.discard(context, it) }
            pendingCapture = null
            message = "Couldn’t open the camera. Please try again or choose from Gallery."
        }
    }
    fun chooseGallery() {
        if (!enabled || pendingCapture != null) return
        sheet = false
        try { gallery.launch(arrayOf("image/*")) }
        catch (_: Exception) { message = "Couldn’t open Gallery. Please try again." }
    }
    ListingPhotoCard(selectedUri, enabled && !busy, isLoading || busy,
        onChoose = { sheet = true }, onCamera = ::takePhoto, onGallery = ::chooseGallery, onRemove = onRemove)
    Text(if (busy) "Preparing your photo…" else if (!enabled && !isLoading)
        "Photo changes are unavailable for this listing." else if (isEditing)
        "Photo changes upload when you save. Keep the current photo or choose a replacement."
        else "Optional. A clear harvest photo helps buyers assess quality. Uploads when you publish.",
        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 8.dp))
    message?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error,
        modifier = Modifier.padding(top = 8.dp)) }
    if (sheet) ModalBottomSheet(onDismissRequest = { sheet = false }, containerColor = MaterialTheme.colorScheme.surface) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Add harvest photo", style = MaterialTheme.typography.headlineSmall)
            Text("Take a fresh photo or choose one you already have.", style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            FilledTonalButton(onClick = ::takePhoto, enabled = enabled && !busy,
                shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) {
                Icon(Lucide.Camera, null); Spacer(Modifier.width(10.dp)); Text("Take Photo")
            }
            OutlinedButton(onClick = ::chooseGallery, enabled = enabled && !busy,
                shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) {
                Icon(Lucide.ImagePlus, null); Spacer(Modifier.width(10.dp)); Text("Choose from Gallery")
            }
            TextButton(onClick = { sheet = false }, modifier = Modifier.fillMaxWidth()) { Text("Cancel") }
        }
    }
}

@Composable
internal fun ListingPhotoCard(
    selectedUri: Uri?, enabled: Boolean, loading: Boolean,
    onChoose: () -> Unit, onCamera: () -> Unit, onGallery: () -> Unit, onRemove: () -> Unit
) {
    var failed by remember(selectedUri) { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Surface(shape = RoundedCornerShape(22.dp), color = MaterialTheme.colorScheme.surfaceContainerLow,
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier.fillMaxWidth().height(210.dp).testTag("listing-photo-preview")
                .clickable(enabled = enabled, onClickLabel = "Choose harvest photo", onClick = onChoose)) {
            Box(contentAlignment = Alignment.Center) {
                if (selectedUri != null && !failed) AsyncImage(selectedUri, "Selected listing photo",
                    Modifier.fillMaxSize(), contentScale = ContentScale.Crop, onError = { failed = true })
                if (selectedUri == null || failed) Column(horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(if (failed) Lucide.AlertCircle else Lucide.Camera, null, Modifier.size(32.dp),
                        tint = MaterialTheme.colorScheme.primary)
                    Text(if (failed) "Photo unavailable" else "Add a clear harvest photo", style = MaterialTheme.typography.titleSmall)
                    Text("JPEG · PNG · WebP · up to 10 MB", style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (loading) Surface(shape = RoundedCornerShape(14.dp), tonalElevation = 4.dp) {
                    CircularProgressIndicator(Modifier.padding(12.dp).size(24.dp), strokeWidth = 2.dp)
                }
            }
        }
        if (selectedUri == null) {
            // Stack actions so labels remain readable on small phones and with larger text.
            FilledTonalButton(onClick = onCamera, enabled = enabled, shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                Icon(Lucide.Camera, null, Modifier.size(20.dp)); Spacer(Modifier.width(8.dp)); Text("Take Photo")
            }
            OutlinedButton(onClick = onGallery, enabled = enabled, shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                Icon(Lucide.ImagePlus, null, Modifier.size(20.dp)); Spacer(Modifier.width(8.dp)); Text("Choose from Gallery")
            }
        } else Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onChoose, enabled = enabled, shape = RoundedCornerShape(16.dp), modifier = Modifier.weight(1f)) {
                Text("Replace Photo")
            }
            TextButton(onClick = onRemove, enabled = enabled, modifier = Modifier.weight(1f)) { Text("Remove Photo") }
        }
    }
}
