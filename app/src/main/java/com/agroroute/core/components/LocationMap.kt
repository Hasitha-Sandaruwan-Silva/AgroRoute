package com.agroroute.core.components

import android.content.ComponentCallbacks2
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.location.Location
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.agroroute.BuildConfig
import com.agroroute.core.location.SelectedLocation
import com.agroroute.core.location.OrderDestinationKind
import com.agroroute.core.location.OrderMapViewport
import com.agroroute.core.location.OrderMapCameraController
import com.agroroute.core.location.OrderCameraTarget
import com.agroroute.core.location.geoJsonCoordinate
import com.agroroute.core.location.usableDeviceCoordinates
import com.agroroute.core.theme.GreenEmerald
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.log.Logger
import org.maplibre.android.module.http.HttpRequestUtil
import org.maplibre.android.maps.*
import org.maplibre.android.style.layers.CircleLayer
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.Property
import org.maplibre.android.style.layers.PropertyFactory.*
import org.maplibre.android.style.layers.SymbolLayer
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.FeatureCollection
import org.maplibre.geojson.Point
import org.maplibre.geojson.LineString

/** Shared map renderer for the editable picker and order-specific destination previews. */
@Composable
fun LocationMap(
    location: SelectedLocation?,
    currentFix: Location? = null,
    focusRequest: Int = 0,
    onSelect: ((SelectedLocation) -> Unit)? = null,
    modifier: Modifier = Modifier,
    orderViewport: OrderMapViewport? = null
) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val configured = BuildConfig.MAPTILER_API_KEY.isNotBlank()
    val latitude = location?.latitude ?: 7.8731
    val longitude = location?.longitude ?: 80.7718
    val hasSelection = location != null
    val pinColor = when (orderViewport?.kind) {
        OrderDestinationKind.PICKUP -> GreenEmerald.toArgb()
        OrderDestinationKind.DELIVERY -> 0xFFB96F27.toInt()
        null -> MaterialTheme.colorScheme.primary.toArgb()
    }
    val density = LocalDensity.current
    var measuredSize by remember { mutableStateOf(IntSize.Zero) }
    val orderCamera = remember(orderViewport?.sceneKey, location?.latitude, location?.longitude) { OrderMapCameraController() }
    val latestOrderCamera by rememberUpdatedState(orderCamera)
    val visibleFix = if (orderViewport == null) currentFix else currentFix?.takeIf {
        usableDeviceCoordinates(it.latitude, it.longitude)
    }
    var map by remember { mutableStateOf<MapLibreMap?>(null) }
    var styleReady by remember { mutableStateOf(false) }
    var mapFailed by remember { mutableStateOf(false) }
    val mapView = remember(context, configured) {
        if (!configured) null else {
            // Map request URLs contain the client key; never log native SDK messages/URLs.
            Logger.setVerbosity(Logger.NONE)
            MapLibre.getInstance(context.applicationContext)
            HttpRequestUtil.setLogEnabled(false)
            HttpRequestUtil.setPrintRequestUrlOnFailure(false)
            MapView(context, MapLibreMapOptions.createFromAttributes(context).textureMode(true)
                .camera(CameraPosition.Builder().target(LatLng(latitude, longitude))
                    .zoom(if (hasSelection) 16.0 else 7.0).build())).apply { onCreate(null) }
        }
    }
    val latestSelect by rememberUpdatedState(onSelect)
    DisposableEffect(mapView, lifecycle) {
        var alive = true
        var started = false
        var resumed = false
        var destroyed = false
        fun destroy() {
            if (destroyed) return
            destroyed = true
            alive = false
            if (resumed) mapView?.onPause()
            if (started) mapView?.onStop()
            resumed = false
            started = false
            mapView?.onDestroy()
            map = null
        }
        val observer = LifecycleEventObserver { _, event ->
            if (alive && mapView != null) when (event) {
                Lifecycle.Event.ON_START -> { mapView.onStart(); started = true }
                Lifecycle.Event.ON_RESUME -> { mapView.onResume(); resumed = true }
                Lifecycle.Event.ON_PAUSE -> { mapView.onPause(); resumed = false }
                Lifecycle.Event.ON_STOP -> { mapView.onStop(); started = false }
                Lifecycle.Event.ON_DESTROY -> destroy()
                else -> Unit
            }
        }
        val memoryCallbacks = object : ComponentCallbacks2 {
            override fun onConfigurationChanged(newConfig: Configuration) = Unit
            override fun onLowMemory() { if (alive) mapView?.onLowMemory() }
            @Suppress("DEPRECATION")
            override fun onTrimMemory(level: Int) { if (level >= ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW) onLowMemory() }
        }
        val sizeListener = android.view.View.OnLayoutChangeListener { view, _, _, _, _, _, _, _, _ ->
            if (alive) measuredSize = IntSize(view.width, view.height)
        }
        lifecycle.addObserver(observer)
        context.applicationContext.registerComponentCallbacks(memoryCallbacks)
        mapView?.addOnLayoutChangeListener(sizeListener)
        mapView?.addOnDidFailLoadingMapListener {
            if (alive) { mapFailed = true; styleReady = false }
        }
        mapView?.getMapAsync { readyMap ->
            if (alive) {
                map = readyMap
                readyMap.uiSettings.isCompassEnabled = false
                readyMap.addOnCameraMoveStartedListener { reason ->
                    if (alive && reason == MapLibreMap.OnCameraMoveStartedListener.REASON_API_GESTURE) {
                        latestOrderCamera.onUserGesture()
                    }
                }
                readyMap.addOnMapClickListener { point ->
                    if (alive) latestSelect?.invoke(SelectedLocation("", point.latitude, point.wrap().longitude))
                    latestSelect != null
                }
                readyMap.setStyle(Style.Builder().fromUri(
                    "https://api.maptiler.com/maps/streets-v2/style.json?key=${android.net.Uri.encode(BuildConfig.MAPTILER_API_KEY)}"
                )) { style ->
                    if (alive) {
                        style.addSource(GeoJsonSource("agroroute-selected", FeatureCollection.fromFeatures(emptyArray())))
                        style.addSource(GeoJsonSource("agroroute-current", FeatureCollection.fromFeatures(emptyArray())))
                        // Sources/layers are created once. A provider route only updates its source.
                        if (orderViewport != null) {
                            style.addSource(GeoJsonSource("agroroute-road-route", FeatureCollection.fromFeatures(emptyArray())))
                            style.addLayer(LineLayer("agroroute-route-outline", "agroroute-road-route").withProperties(
                                lineColor("#FFFFFF"), lineWidth(9f), lineCap(Property.LINE_CAP_ROUND), lineJoin(Property.LINE_JOIN_ROUND)))
                            style.addLayer(LineLayer("agroroute-route-line", "agroroute-road-route").withProperties(
                                lineColor("#246B55"), lineWidth(5f), lineCap(Property.LINE_CAP_ROUND), lineJoin(Property.LINE_JOIN_ROUND)))
                        }
                        style.addLayer(CircleLayer("agroroute-current-halo", "agroroute-current").withProperties(
                            circleRadius(17f), circleColor("#4285F4"), circleOpacity(0.18f)))
                        style.addLayer(CircleLayer("agroroute-current-dot", "agroroute-current").withProperties(
                            circleRadius(7f), circleColor("#4285F4"), circleStrokeColor("#FFFFFF"), circleStrokeWidth(2.5f)))
                        style.addImage("agroroute-pin", locationPinBitmap(pinColor))
                        style.addLayer(SymbolLayer("agroroute-selected-pin", "agroroute-selected").withProperties(
                            iconImage("agroroute-pin"), iconAnchor(Property.ICON_ANCHOR_BOTTOM),
                            iconAllowOverlap(true), iconIgnorePlacement(true)))
                        mapFailed = false
                        styleReady = true
                    }
                }
            }
        }
        onDispose {
            alive = false
            lifecycle.removeObserver(observer)
            context.applicationContext.unregisterComponentCallbacks(memoryCallbacks)
            mapView?.removeOnLayoutChangeListener(sizeListener)
            destroy()
        }
    }
    LaunchedEffect(map, styleReady, latitude, longitude, hasSelection) {
        if (styleReady) map?.style?.getSourceAs<GeoJsonSource>("agroroute-selected")?.let {
            if (hasSelection) it.setGeoJson(Point.fromLngLat(longitude, latitude))
            else it.setGeoJson(FeatureCollection.fromFeatures(emptyArray()))
        }
    }
    LaunchedEffect(map, styleReady, visibleFix) {
        if (styleReady && visibleFix == null) map?.style?.getSourceAs<GeoJsonSource>("agroroute-current")
            ?.setGeoJson(FeatureCollection.fromFeatures(emptyArray()))
        if (styleReady) visibleFix?.let { fix ->
            map?.style?.getSourceAs<GeoJsonSource>("agroroute-current")
                ?.setGeoJson(Point.fromLngLat(fix.longitude, fix.latitude))
        }
    }
    LaunchedEffect(map, styleReady, pinColor) {
        if (styleReady) map?.style?.addImage("agroroute-pin", locationPinBitmap(pinColor))
    }
    LaunchedEffect(map, styleReady, orderViewport?.route) {
        if (styleReady) map?.style?.getSourceAs<GeoJsonSource>("agroroute-road-route")?.let { source ->
            val geometry = orderViewport?.route?.geometry
            if (geometry == null) source.setGeoJson(FeatureCollection.fromFeatures(emptyArray()))
            else source.setGeoJson(LineString.fromLngLats(geometry.map { point ->
                val (lng, lat) = point.geoJsonCoordinate()
                Point.fromLngLat(lng, lat)
            }))
        }
    }
    LaunchedEffect(map, styleReady, focusRequest, location?.takeIf { onSelect == null }?.coordinates()) {
        if (orderViewport == null && styleReady && (focusRequest > 0 || onSelect == null) && hasSelection) map?.animateCamera(CameraUpdateFactory.newLatLngZoom(LatLng(latitude, longitude), 16.0))
    }
    LaunchedEffect(map, styleReady, location?.coordinates(), currentFix) {
        // On order previews, keep both the saved destination and supplier's fix visible.
        if (orderViewport == null && onSelect == null && styleReady && location != null && currentFix != null) {
            withFrameNanos { }
            if (mapView != null && mapView.width > 0 && mapView.height > 0 &&
                (kotlin.math.abs(currentFix.latitude - latitude) > 0.00001 ||
                    kotlin.math.abs(currentFix.longitude - longitude) > 0.00001)) {
                val bounds = LatLngBounds.Builder().include(LatLng(latitude, longitude))
                    .include(LatLng(currentFix.latitude, currentFix.longitude)).build()
                map?.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 48))
            }
        }
    }
    LaunchedEffect(map, styleReady, measuredSize, orderViewport, visibleFix, orderCamera) {
        // AndroidView's measurement is authoritative; a single frame is not a sizing guarantee.
        val viewport = orderViewport ?: return@LaunchedEffect
        val readyMap = map ?: return@LaunchedEffect
        if (!styleReady || measuredSize.width <= 0 || measuredSize.height <= 0) return@LaunchedEffect
        val current = visibleFix?.let { SelectedLocation("", it.latitude, it.longitude) }
        val target = orderCamera.target(location, current, viewport.route,
            viewport.initialCameraPending, viewport.recenterRequest) ?: return@LaunchedEffect
        val update = when (target) {
            is OrderCameraTarget.Destination -> CameraUpdateFactory.newLatLngZoom(
                LatLng(target.location.latitude, target.location.longitude), target.zoom)
            is OrderCameraTarget.Bounds -> {
                val bounds = LatLngBounds.Builder().also { builder ->
                    target.points.forEach { builder.include(LatLng(it.latitude, it.longitude)) }
                }.build()
                // Reserve space for the top-right floating control and the pin's tip/height.
                val left = with(density) { 32.dp.roundToPx() }.coerceAtMost(measuredSize.width / 5)
                val right = with(density) { 64.dp.roundToPx() }.coerceAtMost(measuredSize.width / 4)
                val top = with(density) { 68.dp.roundToPx() }.coerceAtMost(measuredSize.height / 3)
                val bottom = with(density) { 40.dp.roundToPx() }.coerceAtMost(measuredSize.height / 4)
                CameraUpdateFactory.newLatLngBounds(bounds, 0.0, 0.0, left, top, right, bottom)
            }
        }
        if (viewport.recenterRequest > 0) readyMap.animateCamera(update) else readyMap.moveCamera(update)
    }

    val mapShape = RoundedCornerShape(16.dp)
    Box(modifier.clip(mapShape).border(1.dp, MaterialTheme.colorScheme.outlineVariant, mapShape)
        .testTag("location-map")) {
        if (mapView != null) AndroidView(factory = { mapView }, modifier = Modifier.fillMaxSize())
        if (!configured || mapFailed) {
            Surface(modifier = Modifier.align(Alignment.Center).padding(16.dp), tonalElevation = 4.dp) {
                Text(if (orderViewport != null) "Map unavailable. The saved destination is shown below."
                    else if (!configured) "Map unavailable. Search for a place to select its coordinates."
                    else "Map could not load. Check your connection or search for a place.",
                    modifier = Modifier.padding(12.dp), style = MaterialTheme.typography.bodySmall)
            }
        } else if (!styleReady) CircularProgressIndicator(Modifier.align(Alignment.Center).size(32.dp))
    }
}
/** Code-drawn marker: its bottom tip is anchored to the selected coordinate. */
internal fun locationPinBitmap(color: Int): Bitmap {
    val bitmap = Bitmap.createBitmap(48, 64, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    val path = Path().apply {
        moveTo(24f, 62f)
        cubicTo(18f, 48f, 3f, 35f, 3f, 23f)
        cubicTo(3f, -4f, 45f, -4f, 45f, 23f)
        cubicTo(45f, 35f, 30f, 48f, 24f, 62f)
        close()
    }
    paint.color = android.graphics.Color.WHITE
    paint.style = Paint.Style.STROKE
    paint.strokeWidth = 4f
    canvas.drawPath(path, paint)
    paint.style = Paint.Style.FILL
    paint.color = color
    canvas.drawPath(path, paint)
    paint.color = android.graphics.Color.WHITE
    canvas.drawCircle(24f, 23f, 8f, paint)
    return bitmap
}
