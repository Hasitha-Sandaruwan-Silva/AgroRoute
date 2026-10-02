package com.agroroute.core.widgets

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import com.github.thelacspace.icons.Lucide
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage

internal fun shouldUseListingImageFallback(imageUrl: String?, imageFailed: Boolean): Boolean =
    imageUrl.isNullOrBlank() || imageFailed

internal fun shouldShowListingImageLoading(imageUrl: String?, isLoading: Boolean): Boolean =
    !imageUrl.isNullOrBlank() && isLoading

@Composable
fun ListingImage(
    imageUrl: String?,
    modifier: Modifier = Modifier,
    height: Dp = 160.dp
) {
    var imageFailed by remember(imageUrl) { mutableStateOf(false) }
    var imageLoading by remember(imageUrl) { mutableStateOf(!imageUrl.isNullOrBlank()) }
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        if (shouldUseListingImageFallback(imageUrl, imageFailed)) {
            Icon(
                imageVector = Lucide.Image,
                contentDescription = "Listing photo unavailable",
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
        }
        if (shouldShowListingImageLoading(imageUrl, imageLoading) && !imageFailed) {
            CircularProgressIndicator(
                modifier = Modifier.size(28.dp),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.primary
            )
        }
        if (!imageUrl.isNullOrBlank() && !imageFailed) {
            AsyncImage(
                model = imageUrl,
                contentDescription = "Listing photo",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
                onLoading = { imageLoading = true },
                onSuccess = { imageLoading = false },
                onError = {
                    imageLoading = false
                    imageFailed = true
                }
            )
        }
    }
}
