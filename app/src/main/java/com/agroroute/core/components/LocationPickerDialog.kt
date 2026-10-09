package com.agroroute.core.components

import android.Manifest
import android.content.ComponentCallbacks2
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.agroroute.BuildConfig
import com.agroroute.core.location.LocationServices
import com.agroroute.core.location.SelectedLocation
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.log.Logger
import org.maplibre.android.module.http.HttpRequestUtil
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapLibreMapOptions
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.android.style.layers.CircleLayer
import org.maplibre.android.style.layers.Property
import org.maplibre.android.style.layers.PropertyFactory.*
import org.maplibre.android.style.layers.SymbolLayer
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.FeatureCollection
import org.maplibre.geojson.Point

@Composable
fun LocationPickerDialog(initialValue: String, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val services = remember(context) { LocationServices(context.applicationContext) }
    val saved = remember(initialValue) { SelectedLocation.fromStoredText(initialValue) }
    var latitude by rememberSaveable(initialValue) { mutableStateOf(saved?.latitude ?: 7.8731) }
    var longitude by rememberSaveable(initialValue) { mutableStateOf(saved?.longitude ?: 80.7718) }
    var hasSelection by rememberSaveable(initialValue) { mutableStateOf(saved != null) }
    var label by rememberSaveable(initialValue) { mutableStateOf(saved?.address ?: initialValue) }
    var addressEdited by rememberSaveable { mutableStateOf(false) }
    var query by rememberSaveable { mutableStateOf("") }
    var results by remember { mutableStateOf(emptyList<SelectedLocation>()) }
    var message by remember { mutableStateOf<String?>(null) }
    var searching by remember { mutableStateOf(false) }
    var locating by remember { mutableStateOf(false) }
    var styleReady by remember { mutableStateOf(false) }
    var mapFailed by remember { mutableStateOf(false) }
    var map by remember { mutableStateOf<MapLibreMap?>(null) }
    var currentFix by remember { mutableStateOf<android.location.Location?>(null) }
    var focusRequest by remember { mutableIntStateOf(0) }
    var selectionRevision by remember { mutableIntStateOf(0) }
    val configured = BuildConfig.MAPTILER_API_KEY.isNotBlank()
    val scope = rememberCoroutineScope()
    val pinColor = MaterialTheme.colorScheme.primary.toArgb()
    val selected = SelectedLocation(label, latitude, longitude)

    fun select(location: SelectedLocation, focus: Boolean) {
        latitude = location.latitude
        longitude = location.longitude
        label = location.address
        addressEdited = false
        hasSelection = true
        selectionRevision++
        results = emptyList()
        message = null
        if (focus) focusRequest++
    }
    fun locate() {
        if (locating) return
        scope.launch {
            locating = true
            message = null
            try {
                val fix = services.currentLocation()
                if (fix == null) message = "Could not get your location. Turn on device location, or tap the map."
                else {
                    currentFix = fix
                    select(SelectedLocation("", fix.latitude, fix.longitude), focus = true)
                }
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { message = "Location unavailable. You can still tap the map or search." }
            finally { locating = false }
        }
    }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        if (services.hasPermission()) locate()
        else message = "Location permission denied. Tap the map or search to choose a location."
    }
    fun search() {
        if (searching || query.isBlank()) return
        scope.launch {
            searching = true
            message = null
            try {
                results = services.search(query.trim())
                if (results.isEmpty()) message = "No results. Tap the map and enter an address below."
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { message = "Search unavailable. Tap the map and enter an address below." }
            finally { searching = false }
        }
    }

    LaunchedEffect(latitude, longitude, hasSelection, selectionRevision) {
        if (hasSelection) {
            delay(350)
            val address = services.address(latitude, longitude)
            if (!addressEdited && !address.isNullOrBlank()) label = address
        }
    }
    LaunchedEffect(Unit) {
        // Do not prompt on entry; the floating button requests permission when needed.
        if (services.hasPermission()) {
            val fix = try { services.currentLocation() }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { null }
            currentFix = fix
            if (saved == null && !hasSelection && fix != null) {
                select(SelectedLocation("", fix.latitude, fix.longitude), focus = true)
            }
        }
    }

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
    val latestSelect by rememberUpdatedState<(SelectedLocation, Boolean) -> Unit> { location, focus -> select(location, focus) }
    DisposableEffect(mapView, lifecycle) {
        var alive = true
        var started = false
        var resumed = false
        val observer = LifecycleEventObserver { _, event ->
            if (alive && mapView != null) when (event) {
                Lifecycle.Event.ON_START -> { mapView.onStart(); started = true }
                Lifecycle.Event.ON_RESUME -> { mapView.onResume(); resumed = true }
                Lifecycle.Event.ON_PAUSE -> { mapView.onPause(); resumed = false }
                Lifecycle.Event.ON_STOP -> { mapView.onStop(); started = false }
                else -> Unit
            }
        }
        val memoryCallbacks = object : ComponentCallbacks2 {
            override fun onConfigurationChanged(newConfig: Configuration) = Unit
            override fun onLowMemory() { if (alive) mapView?.onLowMemory() }
            @Suppress("DEPRECATION")
            override fun onTrimMemory(level: Int) { if (level >= ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW) onLowMemory() }
        }
        lifecycle.addObserver(observer)
        context.applicationContext.registerComponentCallbacks(memoryCallbacks)
        mapView?.addOnDidFailLoadingMapListener {
            if (alive) { mapFailed = true; styleReady = false }
        }
        mapView?.getMapAsync { readyMap ->
            if (alive) {
                map = readyMap
                readyMap.uiSettings.isCompassEnabled = false
                readyMap.addOnMapClickListener { point ->
                    if (alive) latestSelect(SelectedLocation("", point.latitude, point.longitude), false)
                    true
                }
                readyMap.setStyle(Style.Builder().fromUri(
                    "https://api.maptiler.com/maps/streets-v2/style.json?key=${android.net.Uri.encode(BuildConfig.MAPTILER_API_KEY)}"
                )) { style ->
                    if (alive) {
                        style.addSource(GeoJsonSource("agroroute-selected", FeatureCollection.fromFeatures(emptyArray())))
                        style.addSource(GeoJsonSource("agroroute-current", FeatureCollection.fromFeatures(emptyArray())))
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
            if (resumed) mapView?.onPause()
            if (started) mapView?.onStop()
            mapView?.onDestroy()
            map = null
        }
    }
    LaunchedEffect(map, styleReady, latitude, longitude, hasSelection) {
        if (styleReady && hasSelection) map?.style?.getSourceAs<GeoJsonSource>("agroroute-selected")
            ?.setGeoJson(Point.fromLngLat(longitude, latitude))
    }
    LaunchedEffect(map, styleReady, currentFix) {
        if (styleReady) currentFix?.let { fix ->
            map?.style?.getSourceAs<GeoJsonSource>("agroroute-current")
                ?.setGeoJson(Point.fromLngLat(fix.longitude, fix.latitude))
        }
    }
    LaunchedEffect(map, focusRequest) {
        if (focusRequest > 0) map?.animateCamera(CameraUpdateFactory.newLatLngZoom(LatLng(latitude, longitude), 16.0))
    }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize(), color = Color(0xFFF1F5F2)) {
            BoxWithConstraints(Modifier.fillMaxSize().safeDrawingPadding().imePadding()) {
                val panelMaxHeight = maxHeight * 0.55f
                Column(Modifier.fillMaxSize()) {
                    Box(Modifier.fillMaxWidth().weight(1f).clipToBounds()) {
                        if (mapView != null) AndroidView(factory = { mapView }, modifier = Modifier.fillMaxSize())
                        else Text("Add your MapTiler key to local.properties and rebuild to load the map.",
                            modifier = Modifier.align(Alignment.Center).padding(32.dp))
                        Surface(onClick = onDismiss, shape = CircleShape, color = Color.White, shadowElevation = 4.dp,
                            modifier = Modifier.align(Alignment.TopStart).padding(16.dp).size(48.dp)) {
                            Box(contentAlignment = Alignment.Center) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
                        }
                        Surface(shape = RoundedCornerShape(24.dp), color = Color.White.copy(alpha = 0.95f),
                            modifier = Modifier.align(Alignment.TopCenter).padding(top = 20.dp)) {
                            Text("Tap to place a pin", style = MaterialTheme.typography.labelMedium,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp))
                        }
                        Surface(onClick = {
                            if (services.hasPermission()) locate() else permissionLauncher.launch(arrayOf(
                                Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION))
                        }, shape = CircleShape, color = Color.White, shadowElevation = 4.dp,
                            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp).size(52.dp)) {
                            Box(contentAlignment = Alignment.Center) {
                                if (locating) CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp)
                                else Icon(Icons.Default.MyLocation, "Go to my current location", tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                        if (configured && !styleReady && !mapFailed) {
                            CircularProgressIndicator(Modifier.align(Alignment.Center).size(32.dp))
                        }
                    }
                    Surface(shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp), color = Color.White,
                        shadowElevation = 8.dp, modifier = Modifier.fillMaxWidth().heightIn(max = panelMaxHeight)) {
                        Column(Modifier.padding(20.dp)) {
                            Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Icon(Icons.Default.LocationOn, null, tint = MaterialTheme.colorScheme.primary)
                                    Text("Choose your location", style = MaterialTheme.typography.titleLarge)
                                }
                                Text(if (hasSelection) label.ifBlank { "Address unavailable — enter a place name below" }
                                    else "Tap the map or search for your pickup / delivery location.",
                                    style = MaterialTheme.typography.bodyMedium, maxLines = 2)
                                OutlinedTextField(query, { query = it }, label = { Text("Search a place or address") },
                                    singleLine = true, shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth(),
                                    trailingIcon = {
                                        IconButton(onClick = { search() }, enabled = query.isNotBlank() && !searching) {
                                            if (searching) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                                            else Icon(Icons.Default.Search, "Search")
                                        }
                                    })
                                results.forEach { result ->
                                    TextButton(onClick = { select(result, focus = true) }, modifier = Modifier.fillMaxWidth()) {
                                        Text(result.address.ifBlank { result.coordinates() })
                                    }
                                }
                                OutlinedTextField(label, { label = it; addressEdited = true },
                                    label = { Text("Selected address / place name") }, maxLines = 2,
                                    shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth())
                                if (hasSelection) Text(selected.coordinates(), style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                                if (mapFailed) Text("Map could not load. Check your connection and MapTiler key, or use search/current location.",
                                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                                message?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error) }
                            }
                            Spacer(Modifier.height(12.dp))
                            Button(onClick = { onConfirm(selected.toStoredText()) }, enabled = hasSelection,
                                shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth().height(52.dp)) {
                                Text("Use this location")
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Code-drawn marker: its bottom tip is anchored to the selected coordinate. */
private fun locationPinBitmap(color: Int): Bitmap {
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
