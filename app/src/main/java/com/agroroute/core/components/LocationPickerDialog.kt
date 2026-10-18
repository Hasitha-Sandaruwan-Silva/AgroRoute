package com.agroroute.core.components

import com.github.thelacspace.icons.Lucide
import android.Manifest
import android.view.ViewGroup
import android.view.WindowManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import com.agroroute.core.location.LocationServices
import com.agroroute.core.location.SelectedLocation
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.animation.Crossfade

@Composable
fun LocationPickerDialog(initialValue: String, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    val keyboardVisible = WindowInsets.ime.getBottom(LocalDensity.current) > 0
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
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
    var permissionDenied by remember { mutableStateOf(false) }
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
        else {
            permissionDenied = true
            message = "Location permission denied. Tap the map, search, or enable permission in settings."
        }
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
        if (hasSelection && selectionRevision > 0) {
            delay(350)
            val address = services.address(latitude, longitude)
            if (!addressEdited) {
                label = address?.takeIf { it.isNotBlank() } ?: "Selected location"
                if (address.isNullOrBlank()) message = "Address unavailable. The selected coordinates can still be saved."
            }
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

    // Compose 1.7's non-default-width path measures against screenHeightDp instead of
    // the available window/IME bounds. Keep window-based measurement and set full size.
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = true, decorFitsSystemWindows = false)) {
        val dialogWindow = (LocalView.current.parent as DialogWindowProvider).window
        SideEffect {
            dialogWindow.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            dialogWindow.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        }
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            LocationPickerLayout(
                search = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (!keyboardVisible) Surface(shape = RoundedCornerShape(24.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerLow,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                            shadowElevation = 4.dp) {
                            Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            FilledTonalIconButton(onClick = onDismiss, modifier = Modifier.size(48.dp)) { Icon(Lucide.ArrowLeft, "Back") }
                            Column(Modifier.weight(1f)) {
                                Text("Choose location", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text("Tap the map or use your location", style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        }
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (keyboardVisible) FilledTonalIconButton(onClick = onDismiss, modifier = Modifier.size(48.dp)) {
                                Icon(Lucide.ArrowLeft, "Back")
                            }
                            OutlinedTextField(query, { query = it }, placeholder = {
                                Text("Search a place or address", maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }, singleLine = true, shape = RoundedCornerShape(24.dp), modifier = Modifier.weight(1f),
                                colors = OutlinedTextFieldDefaults.colors(
                                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                                    focusedContainerColor = MaterialTheme.colorScheme.surface),
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                keyboardActions = KeyboardActions(onSearch = {
                                    search()
                                    focusManager.clearFocus()
                                    keyboard?.hide()
                                }),
                                trailingIcon = {
                                    IconButton(onClick = { search(); focusManager.clearFocus(); keyboard?.hide() },
                                        enabled = query.isNotBlank() && !searching) {
                                        if (searching) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                                        else Icon(Lucide.Search, "Search")
                                    }
                                })
                        }
                    }
                },
                results = if (results.isEmpty()) null else ({
                    results.forEach { result ->
                        TextButton(onClick = { select(result, focus = true); focusManager.clearFocus(); keyboard?.hide() },
                            modifier = Modifier.fillMaxWidth()) {
                            Text(result.address.ifBlank { result.coordinates() })
                        }
                    }
                }),
                map = {
                    BoxWithConstraints(Modifier.fillMaxSize()) {
                        LocationMap(location = selected.takeIf { hasSelection }, currentFix = currentFix,
                            focusRequest = focusRequest, onSelect = { select(it, focus = false) },
                            modifier = Modifier.fillMaxSize())
                        if (maxHeight >= 140.dp) {
                            Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surface,
                                shadowElevation = 2.dp,
                                modifier = Modifier.align(Alignment.TopStart).padding(top = 12.dp, start = 12.dp, end = 72.dp)) {
                                Row(Modifier.padding(horizontal = 10.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                                    Icon(Lucide.MapPin, null, Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
                                    Text("Tap to place a pin", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                        MapLocationControl(locating = locating, description = "Go to my current location", onClick = {
                            if (services.hasPermission()) locate() else permissionLauncher.launch(arrayOf(
                                Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION))
                        }, modifier = Modifier.align(Alignment.BottomEnd)
                            .padding(if (maxHeight < 100.dp) 4.dp else 12.dp))
                    }
                },
                address = {
                    Surface(shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surfaceContainerLow,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha =
                            if (com.agroroute.features.farmer.components.LocalFarmerPresentation.current) 1f else 0.5f)),
                        shadowElevation = 2.dp) {
                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(Lucide.MapPin, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                Text("Your selected location", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold,
                                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            // The editable address is shown once; saved coordinates remain unchanged.
                            OutlinedTextField(label, { label = it; addressEdited = true },
                                label = { Text("Selected address / place name") }, maxLines = 2,
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus(); keyboard?.hide() }),
                                shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth())
                            if (!hasSelection) Text("Tap the map or search for your pickup / delivery location.",
                                style = MaterialTheme.typography.bodySmall)
                            else Crossfade(targetState = selected.coordinates(), label = "selected-coordinates") { coordinates ->
                                Text(coordinates, style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            message?.let { MapStatusMessage("Location update", it, error = true) }
                            if (permissionDenied) TextButton(onClick = {
                                context.startActivity(android.content.Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                    android.net.Uri.parse("package:${context.packageName}")))
                            }) { Text("Open app settings") }
                        }
                    }
                },
                confirm = {
                    Button(onClick = { focusManager.clearFocus(); keyboard?.hide(); onConfirm(selected.toStoredText()) },
                        enabled = hasSelection, shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) {
                        Icon(Lucide.Check, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Use this location", fontWeight = FontWeight.SemiBold)
                    }
                }
            )
        }
    }
}
