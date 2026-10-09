package com.agroroute.core.components

import android.Manifest
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.agroroute.core.location.LocationServices
import com.agroroute.core.location.SelectedLocation
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun LocationPickerDialog(initialValue: String, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    val context = LocalContext.current
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
    var currentFix by remember { mutableStateOf<android.location.Location?>(null) }
    var focusRequest by remember { mutableIntStateOf(0) }
    var selectionRevision by remember { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()
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

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize(), color = Color(0xFFF1F5F2)) {
            BoxWithConstraints(Modifier.fillMaxSize().safeDrawingPadding().imePadding()) {
                val panelMaxHeight = maxHeight * 0.55f
                Column(Modifier.fillMaxSize()) {
                    Box(Modifier.fillMaxWidth().weight(1f).clipToBounds()) {
                        LocationMap(location = selected.takeIf { hasSelection }, currentFix = currentFix,
                            focusRequest = focusRequest, onSelect = { select(it, focus = false) },
                            modifier = Modifier.fillMaxSize())
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
