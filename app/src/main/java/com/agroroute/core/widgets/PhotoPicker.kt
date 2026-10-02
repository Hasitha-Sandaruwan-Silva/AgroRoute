package com.agroroute.core.widgets

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import com.github.thelacspace.icons.Lucide
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage

/**
 * A tappable image-picker composable that uses the system photo picker and renders the
 * selected image via Coil's [AsyncImage]; shows an icon placeholder when no image is selected.
 */
@Composable
fun PhotoPicker(
    selectedUri: Uri?,
    onImageSelected: (Uri) -> Unit,
    modifier: Modifier = Modifier
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

    Box(
        modifier = modifier
            .size(120.dp)
            .clip(RoundedCornerShape(12.dp))
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable { launcher.launch(arrayOf("image/*")) },
        contentAlignment = Alignment.Center
    ) {
        if (selectedUri != null) {
            AsyncImage(
                model            = selectedUri,
                contentDescription = "Selected photo",
                contentScale     = ContentScale.Crop,
                modifier         = Modifier.matchParentSize()
            )
        } else {
            Icon(
                imageVector  = Lucide.ImagePlus,
                contentDescription = "Pick a photo",
                tint         = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
