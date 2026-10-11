package com.agroroute.core.components

import android.Manifest
import android.location.Location
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import com.agroroute.core.location.LocationServices
import com.agroroute.core.location.SelectedLocation
import com.agroroute.core.location.openOrderDirections
import com.agroroute.core.model.Order
import com.agroroute.core.model.OrderStatus
import com.agroroute.core.model.pickupSnapshot
import com.agroroute.core.model.deliverySnapshot
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

@Composable
fun DeliveryLocationField(value: String, onSelected: (String) -> Unit, enabled: Boolean = true) {
    var choosing by remember { mutableStateOf(false) }
    val location = SelectedLocation.fromStoredText(value)
    AppCard {
        Text("Delivery destination", style = MaterialTheme.typography.titleMedium)
        Text(location?.address?.ifBlank { location.coordinates() } ?: "Choose where this order should be delivered.",
            style = MaterialTheme.typography.bodyMedium)
        OutlinedButton(onClick = { choosing = true }, enabled = enabled, modifier = Modifier.fillMaxWidth()) {
            Text(if (location == null) "Choose delivery location on map" else "Change delivery location")
        }
    }
    if (choosing) LocationPickerDialog(value, onDismiss = { choosing = false }, onConfirm = {
        onSelected(it)
        choosing = false
    })
}

/** Both destinations come exclusively from this order's immutable saved values. */
@Composable
fun OrderLocationSection(order: Order, forSupplier: Boolean) {
    val mapHeight = (LocalConfiguration.current.screenHeightDp.dp * 0.3f).coerceIn(140.dp, 280.dp)
    val pickup = order.pickupSnapshot()
    val delivery = order.deliverySnapshot()
    val navigatingToPickup = forSupplier && order.status == OrderStatus.AWAITING_PICKUP
    val destination = if (navigatingToPickup) pickup else delivery
    val context = LocalContext.current
    val services = remember(context) { LocationServices(context.applicationContext) }
    val scope = rememberCoroutineScope()
    var fix by remember(order.orderId) { mutableStateOf<Location?>(null) }
    var locating by remember { mutableStateOf(false) }
    var message by remember(order.orderId) { mutableStateOf<String?>(null) }
    fun locate() {
        if (locating) return
        scope.launch {
            locating = true
            try {
                fix = services.currentLocation()
                if (fix == null) message = "Current location unavailable. The saved destination is still shown."
                else message = null
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { message = "Could not get your current location." }
            finally { locating = false }
        }
    }
    val permissions = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        if (services.hasPermission()) locate()
        else message = "Location permission denied. You can still view the saved destination and get directions."
    }
    LaunchedEffect(order.orderId, forSupplier) {
        if (forSupplier && services.hasPermission()) locate()
    }
    AppCard {
        Text(if (forSupplier) "Pickup & delivery" else "Delivery destination", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(12.dp))
        if (destination != null) {
            LocationMap(location = destination, currentFix = fix, focusRequest = 1,
                modifier = Modifier.fillMaxWidth().height(mapHeight))
        } else {
            Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(16.dp)) {
                Text("This older order has no saved ${if (navigatingToPickup) "pickup" else "delivery"} coordinates. Address information is shown below.",
                    modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.bodyMedium)
            }
        }
        Spacer(Modifier.height(12.dp))
        Text("Farmer pickup", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        Text(pickup?.address?.ifBlank { pickup.coordinates() } ?: order.pickupLocation.ifBlank { "Not provided" })
        Text("Supplier delivery", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        Text(delivery?.address?.ifBlank { delivery.coordinates() } ?: order.deliveryLocation.ifBlank { "Not provided" })
        if (forSupplier) {
            OutlinedButton(onClick = {
                if (services.hasPermission()) locate() else permissions.launch(arrayOf(
                    Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION))
            }, enabled = !locating, modifier = Modifier.fillMaxWidth()) {
                Text(if (locating) "Getting current location…" else "Show my current location")
            }
            Button(onClick = {
                if (destination != null && !openOrderDirections(context, destination)) message = "No maps app or browser is available."
            }, enabled = destination != null, modifier = Modifier.fillMaxWidth()) {
                Text(if (navigatingToPickup) "Navigate to Farmer" else "Navigate to delivery destination")
            }
        }
        message?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
    }
}
