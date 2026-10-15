package com.agroroute.core.components

import android.Manifest
import android.location.Location
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.background
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.agroroute.core.location.LocationServices
import com.agroroute.core.location.SelectedLocation
import com.agroroute.core.location.*
import com.agroroute.core.theme.GreenEmerald
import com.github.thelacspace.icons.Lucide
import com.agroroute.core.location.openOrderDirections
import com.agroroute.core.model.Order
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

@Composable
fun DeliveryLocationField(value: String, onSelected: (String) -> Unit, enabled: Boolean = true) {
    var choosing by remember { mutableStateOf(false) }
    val location = SelectedLocation.fromStoredText(value)
    AppCard {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            AgroIconBox(icon = Lucide.MapPin)
            Column {
                Text("Delivery destination", style = MaterialTheme.typography.titleMedium)
                Text(if (location == null) "Select on the map" else "Saved destination", style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Spacer(Modifier.height(12.dp))
        Text(location?.address?.ifBlank { location.coordinates() } ?: "Choose where this order should be delivered.",
            style = MaterialTheme.typography.bodyMedium)
        OutlinedButton(onClick = { choosing = true }, enabled = enabled, shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
            Text(if (location == null) "Choose delivery location on map" else "Change delivery location")
        }
    }
    if (choosing) LocationPickerDialog(value, onDismiss = { choosing = false }, onConfirm = {
        onSelected(it)
        choosing = false
    })
}

/** Read-only stage-aware navigation; no snapshot/profile writes or lifecycle actions. */
@Composable
fun OrderLocationSection(order: Order, forSupplier: Boolean, routeRepository: RouteRepository? = null) {
    val presentation = orderMapPresentation(order, forSupplier)
    val destination = presentation.destination
    val active = presentation.navigationEnabled
    val mapHeight = if (active) (LocalConfiguration.current.screenHeightDp.dp * 0.36f).coerceIn(220.dp, 300.dp)
        else if (forSupplier) 140.dp else 180.dp
    val context = LocalContext.current
    val services = remember(context) { LocationServices(context.applicationContext) }
    val repository = routeRepository ?: remember(order.orderId) { CachedRouteRepository() }
    val scope = rememberCoroutineScope()
    var fix by remember(presentation.sceneKey) { mutableStateOf<Location?>(null) }
    var locating by remember(presentation.sceneKey) { mutableStateOf(false) }
    var locationReady by remember(presentation.sceneKey) { mutableStateOf(!active || !services.hasPermission()) }
    var recenterRequest by remember(presentation.sceneKey) { mutableIntStateOf(0) }
    var routeResult by remember(presentation.sceneKey) { mutableStateOf<RouteResult?>(null) }
    var message by remember(presentation.sceneKey) { mutableStateOf<String?>(null) }
    suspend fun locate(recenter: Boolean) {
        if (locating) return
        locating = true
        try {
            val updatedFix = services.currentLocation()
            if (updatedFix == null) routeResult = RouteResult.Unavailable
            else if (fix == null) routeResult = null
            fix = updatedFix
            message = if (fix == null) "Current location unavailable. The saved destination is still shown." else null
            if (recenter) recenterRequest++
        } catch (cancelled: CancellationException) { throw cancelled }
        catch (_: Exception) {
            fix = null
            routeResult = RouteResult.Unavailable
            message = "Could not get your current location. The saved destination is still shown."
            if (recenter) recenterRequest++
        } finally { locating = false; locationReady = true }
    }
    val permissions = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        if (active && services.hasPermission()) scope.launch { locate(recenter = true) }
        else if (active) {
            message = "Location permission denied. You can still view the saved destination and get directions."
            locationReady = true
            recenterRequest++
        }
    }
    LaunchedEffect(presentation.sceneKey) {
        if (active && services.hasPermission()) locate(recenter = false)
        else locationReady = true
    }
    LaunchedEffect(presentation.sceneKey, locationReady, fix != null, repository) {
        // A one-shot route per stage. Recenter/GPS updates reuse this route; no automatic rerouting.
        if (active && locationReady) {
            val start = fix?.let { SelectedLocation("", it.latitude, it.longitude) }
            routeResult = if (start == null || destination == null) RouteResult.Unavailable else try {
                repository.route(start, destination)
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { RouteResult.Unavailable }
        }
    }
    val roadRoute = (routeResult as? RouteResult.Available)?.route?.takeIf { active }
    val pinColor = if (presentation.kind == OrderDestinationKind.PICKUP) GreenEmerald else Color(0xFFB96F27)
    AppCard {
        Text(presentation.heading, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(10.dp))
        if (destination != null) {
            Box(Modifier.fillMaxWidth().height(mapHeight).clip(RoundedCornerShape(22.dp))) {
                LocationMap(location = destination, currentFix = fix.takeIf { active },
                    orderViewport = OrderMapViewport(presentation.sceneKey, presentation.kind,
                        initialCameraPending = active && (!locationReady || (fix != null && routeResult == null)),
                        recenterRequest = recenterRequest, route = roadRoute),
                    modifier = Modifier.fillMaxSize())
                if (active) MapLocationControl(locating = locating,
                    description = "Recenter current location and destination", onClick = {
                    if (services.hasPermission()) scope.launch { locate(recenter = true) }
                    else permissions.launch(arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION))
                }, modifier = Modifier.align(Alignment.TopEnd).padding(12.dp).testTag("order-map-recenter"))
            }
        } else {
            Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(16.dp)) {
                Text("This older order has no saved ${if (presentation.kind == OrderDestinationKind.PICKUP) "pickup" else "delivery"} coordinates. Address information is shown below.",
                    modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.bodyMedium)
            }
        }
        Spacer(Modifier.height(10.dp))
        Surface(shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.surfaceContainerLow) {
        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(Lucide.MapPin, null, tint = pinColor, modifier = Modifier.size(20.dp))
            Column(Modifier.weight(1f)) {
                Text(presentation.destinationLabel, style = MaterialTheme.typography.labelLarge)
                Text(presentation.address, style = MaterialTheme.typography.bodySmall,
                    maxLines = if (active) 2 else 1, overflow = TextOverflow.Ellipsis)
                if (active) Text(roadRoute?.summary() ?: when {
                    !locationReady -> "Getting current position…"
                    fix != null && routeResult == null -> "Finding road route…"
                    else -> "Road route unavailable. Use external navigation."
                },
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.testTag("order-route-summary"))
            }
        }
        }
        if (active) {
            Row(Modifier.padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(Modifier.size(8.dp).background(Color(0xFF4285F4), CircleShape))
                Text(if (fix != null) "You" else "Your position unavailable", style = MaterialTheme.typography.labelSmall)
                Spacer(Modifier.width(8.dp))
                Box(Modifier.size(8.dp).background(pinColor, CircleShape))
                Text(if (presentation.kind == OrderDestinationKind.PICKUP) "Pickup" else "Delivery", style = MaterialTheme.typography.labelSmall)
            }
            Button(onClick = {
                if (destination != null && !openOrderDirections(context, destination)) message = "No maps app or browser is available."
            }, enabled = destination != null, shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp).heightIn(min = 52.dp)) {
                Icon(Lucide.Navigation, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(presentation.navigationLabel.orEmpty())
            }
        }
        if (active) message?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
    }
}
