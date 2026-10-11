package com.agroroute.features.supplier.marketplace

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.net.Uri
import android.util.LruCache
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.agroroute.BuildConfig
import com.agroroute.core.components.locationPinBitmap
import com.agroroute.core.location.SelectedLocation
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withTimeoutOrNull
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.log.Logger
import org.maplibre.android.maps.Style
import org.maplibre.android.module.http.HttpRequestUtil
import org.maplibre.android.snapshotter.MapSnapshotter
import kotlin.coroutines.resume

/** Only bitmaps live in cards; a single cancellable renderer serves the visible list. */
@Composable
internal fun MarketplaceLocationPreview(location: SelectedLocation, modifier: Modifier = Modifier) {
    val context = LocalContext.current.applicationContext
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val pinColor = MaterialTheme.colorScheme.primary.toArgb()
    var size by remember { mutableStateOf(IntSize.Zero) }
    val key = remember(location, size, pinColor) { PreviewKey(location.latitude, location.longitude, size, pinColor) }
    var bitmap by remember(key) { mutableStateOf<Bitmap?>(null) }
    var failed by remember(key) { mutableStateOf(false) }

    LaunchedEffect(key, lifecycle) {
        if (size.width == 0 || size.height == 0) return@LaunchedEffect
        if (BuildConfig.MAPTILER_API_KEY.isBlank()) { failed = true; return@LaunchedEffect }
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            if (bitmap == null) {
                failed = false
                bitmap = try { MarketplaceSnapshots.render(context, key) }
                catch (cancelled: CancellationException) { throw cancelled }
                catch (_: Exception) { null }
                failed = bitmap == null
            }
        }
    }

    Column(modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(
            Modifier.fillMaxWidth().height(120.dp).clip(MaterialTheme.shapes.medium)
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .onSizeChanged { measured ->
                    // Bound bitmap memory while retaining the card's aspect ratio.
                    val scale = minOf(1f, 720f / measured.width.coerceAtLeast(1))
                    size = IntSize((measured.width * scale).toInt(), (measured.height * scale).toInt())
                },
            contentAlignment = Alignment.Center
        ) {
            bitmap?.let {
                Image(it.asImageBitmap(), contentDescription = "Farmer pickup location",
                    modifier = Modifier.fillMaxSize())
            } ?: Text(if (failed) "Map preview unavailable" else "Loading map preview…",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(location.address.ifBlank { location.coordinates() },
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

private data class PreviewKey(val latitude: Double, val longitude: Double, val size: IntSize, val pinColor: Int)

private object MarketplaceSnapshots {
    private val renderer = Semaphore(1)
    private val cache = object : LruCache<PreviewKey, Bitmap>(8 * 1024 * 1024) {
        override fun sizeOf(key: PreviewKey, value: Bitmap) = value.allocationByteCount
    }

    suspend fun render(context: Context, key: PreviewKey): Bitmap? = renderer.withPermit {
        cache.get(key)?.let { return@withPermit it }
        Logger.setVerbosity(Logger.NONE)
        MapLibre.getInstance(context)
        HttpRequestUtil.setLogEnabled(false)
        HttpRequestUtil.setPrintRequestUrlOnFailure(false)
        val coordinate = LatLng(key.latitude, key.longitude)
        val snapshotter = MapSnapshotter(context, MapSnapshotter.Options(key.size.width, key.size.height)
            .withStyleBuilder(Style.Builder().fromUri(
                "https://api.maptiler.com/maps/streets-v2/style.json?key=${Uri.encode(BuildConfig.MAPTILER_API_KEY)}"))
            .withCameraPosition(CameraPosition.Builder().target(coordinate).zoom(12.0).build())
            .withLogo(false)) // SDK 11.8 includes source attribution in snapshots automatically.
        try {
            val image = withTimeoutOrNull(15_000) {
                suspendCancellableCoroutine<Bitmap?> { continuation ->
                    snapshotter.start({ snapshot ->
                        if (continuation.isActive) {
                            val result = runCatching {
                                val image = snapshot.bitmap.copy(Bitmap.Config.ARGB_8888, true)
                                val point = snapshot.pixelForLatLng(coordinate)
                                val pin = locationPinBitmap(key.pinColor)
                                Canvas(image).drawBitmap(pin, point.x - pin.width / 2f, point.y - pin.height, null)
                                pin.recycle()
                                image
                            }.getOrNull()
                            continuation.resume(result)
                        }
                    }, { if (continuation.isActive) continuation.resume(null) })
                }
            }
            if (image != null) cache.put(key, image)
            image
        } finally {
            // Runs on the main thread on completion, timeout, lifecycle stop, or list disposal.
            snapshotter.cancel()
        }
    }
}
