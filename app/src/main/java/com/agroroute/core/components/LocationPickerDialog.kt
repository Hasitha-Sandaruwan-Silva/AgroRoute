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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import com.agroroute.core.location.LocationServices
import com.agroroute.core.location.SelectedLocation
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun LocationPickerDialog(initialValue: String, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
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
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilledTonalIconButton(onClick = onDismiss, modifier = Modifier.size(48.dp)) { Icon(Lucide.ArrowLeft, "Back") }
                        OutlinedTextField(query, { query = it }, placeholder = {
                            Text("Search a place or address", maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }, singleLine = true, shape = RoundedCornerShape(16.dp), modifier = Modifier.weight(1f),
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
                            Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surface,
                                modifier = Modifier.align(Alignment.TopCenter).padding(top = 8.dp)) {
                                Text("Tap to place a pin", style = MaterialTheme.typography.labelSmall,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp))
                            }
                        }
                        Surface(onClick = {
                            if (services.hasPermission()) locate() else permissionLauncher.launch(arrayOf(
                                Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION))
                        }, shape = CircleShape, color = MaterialTheme.colorScheme.surface, shadowElevation = 2.dp,
                            modifier = Modifier.align(Alignment.BottomEnd)
                                .padding(if (maxHeight < 100.dp) 4.dp else 8.dp).size(48.dp)) {
                            Box(contentAlignment = Alignment.Center) {
                                if (locating) CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp)
                                else Icon(Lucide.MyLocation, "Go to my current location", tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                },
                address = {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Lucide.MapPin, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                            Text("Your selected location", style = MaterialTheme.typography.labelLarge,
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
                        else Text(selected.coordinates(), style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        message?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error) }
                    }
                },
                confirm = {
                    Button(onClick = { focusManager.clearFocus(); keyboard?.hide(); onConfirm(selected.toStoredText()) },
                        enabled = hasSelection, shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) {
                        Text("Use this location")
                    }
                }
            )
        }
    }
}
