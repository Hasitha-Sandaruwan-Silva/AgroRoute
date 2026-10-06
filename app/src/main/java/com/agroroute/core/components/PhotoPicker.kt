package com.agroroute.core.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import com.github.thelacspace.icons.Lucide
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import coil.compose.AsyncImage

/**
 * A tappable image-picker composable that uses the system photo picker and renders the
 * selected image via Coil's [AsyncImage]; shows an icon placeholder when no image is selected.
 */
@Composable
fun PhotoPicker(
    selectedUri: Uri?,
    onImageSelected: (Uri) -> Unit,
    modifier: Modifier = Modifier,
    height: Dp = 120.dp,
    enabled: Boolean = true
) {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    it,
                    android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
            onImageSelected(it)
        }
    }

    var imageFailed by androidx.compose.runtime.remember(selectedUri) {
        androidx.compose.runtime.mutableStateOf(false)
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline,
                shape = RoundedCornerShape(20.dp)
            )
            .clickable(enabled = enabled) { launcher.launch(arrayOf("image/*")) },
        contentAlignment = Alignment.Center
    ) {
        if (selectedUri != null && !imageFailed) {
            AsyncImage(
                model            = selectedUri,
                contentDescription = "Selected photo",
                contentScale     = ContentScale.Crop,
                modifier         = Modifier.matchParentSize(),
                onError = { imageFailed = true }
            )
        }
        if (selectedUri == null || imageFailed) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector  = if (selectedUri == null) Lucide.ImagePlus else Lucide.Image,
                    contentDescription = if (selectedUri == null) "Pick a photo" else "Selected photo unavailable",
                    tint         = MaterialTheme.colorScheme.primary,
                    modifier     = Modifier.size(26.dp)
                )
            }
        }
    }
}
