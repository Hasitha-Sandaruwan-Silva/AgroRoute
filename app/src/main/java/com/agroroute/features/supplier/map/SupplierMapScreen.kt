package com.agroroute.features.supplier.map

import android.Manifest
import android.location.Location
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.agroroute.core.components.*
import com.agroroute.core.location.*
import com.agroroute.core.model.Order
import com.agroroute.core.model.OrderStatus
import com.agroroute.core.navigation.Screen
import com.agroroute.core.runtime.appViewModel
import com.agroroute.features.supplier.orders.SupplierOrderViewModel
import com.agroroute.features.supplier.orders.orderListingPresentation
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import java.util.Locale

@Composable
fun SupplierMapScreen(navController: NavController, viewModel: SupplierOrderViewModel = appViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val listings = orderListingPresentation()
    var loaded by remember { mutableStateOf(false) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        if (loaded) viewModel.refreshOrdersForSupplier(viewModel.currentSupplierId)
        else { loaded = true; viewModel.loadOrdersForSupplier(viewModel.currentSupplierId) }
    }
    SupplierMapContent(state.orders, viewModel.currentSupplierId, state.isLoading,
        hasOrderError = state.error != null,
        farmerName = { listings[it.listingId]?.farmerName?.takeIf(String::isNotBlank) ?: "Name unavailable" },
        onDetails = { navController.navigate(Screen.SupplierOrderDetail.createRoute(it.orderId)) })
}

@Composable
internal fun SupplierMapContent(orders: List<Order>, supplierId: String, loading: Boolean = false,
    hasOrderError: Boolean = false, farmerName: (Order) -> String = { "Name unavailable" }, onDetails: (Order) -> Unit) {
    val context = LocalContext.current
    val services = remember(context) { LocationServices(context.applicationContext) }
    val scope = rememberCoroutineScope()
    var fix by remember { mutableStateOf<Location?>(null) }
    var locating by remember { mutableStateOf(false) }
    var locationReady by remember { mutableStateOf(!services.hasPermission()) }
    var message by remember { mutableStateOf<String?>(null) }
    var focus by remember { mutableIntStateOf(0) }
    var filter by remember { mutableStateOf(SupplierMapFilter.All) }
    var selectedId by remember { mutableStateOf<String?>(null) }
    suspend fun locate(recenter: Boolean) {
        if (locating) return
        locating = true
        try {
            fix = services.currentLocation()
            message = if (fix == null) "Current location unavailable. Your saved order locations are still available." else null
            if (recenter) focus++
        } catch (e: CancellationException) { throw e }
        catch (_: Exception) { message = "Could not find your location. Your saved order locations are still available." }
        finally { locating = false; locationReady = true }
    }
    val permissions = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        if (services.hasPermission()) scope.launch { locate(true) }
        else message = "Location access is off. Enable it in app settings to show your position. Saved orders are still available."
    }
    LaunchedEffect(Unit) { if (services.hasPermission()) locate(false) }
    val active = remember(orders, supplierId) { supplierMapOrders(orders, supplierId) }
    val visible = remember(orders, supplierId, filter) { supplierMapOrders(orders, supplierId, filter) }
    val selected = remember(visible, selectedId) { visible.firstOrNull { it.orderId == selectedId } }
    val presentations = remember(visible) { visible.associate { it.orderId to orderMapPresentation(it, true) } }
    val distances = remember(presentations, fix?.latitude, fix?.longitude) {
        val current = fix
        val result = FloatArray(1)
        presentations.mapValues { (_, presentation) ->
            val destination = presentation.destination
            if (current == null || destination == null) null else {
                Location.distanceBetween(current.latitude, current.longitude, destination.latitude, destination.longitude, result)
                result[0]
            }
        }
    }
    val pins = remember(visible, presentations, distances) {
        visible.mapNotNull { order -> presentations[order.orderId]?.destination?.let {
            OrderMapPin(order.orderId, it, order.status == OrderStatus.IN_TRANSIT, distances[order.orderId]?.let { meters -> meters <= 10000 } == true)
        } }
    }
    Column(Modifier.fillMaxSize().testTag("supplier-map-screen")) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween) {
            Text("${active.size} Active Orders", style = MaterialTheme.typography.labelLarge)
            if (loading) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
        }
        if (fix != null) Text("Nearby orders have a green halo ? within 10 km", Modifier.padding(horizontal = 16.dp), style = MaterialTheme.typography.labelSmall)
        Row(Modifier.padding(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SupplierMapFilter.entries.forEach { item -> FilterChip(selected = filter == item,
                onClick = { filter = item }, label = { Text(item.name) }) }
        }
        Box(Modifier.weight(1f).fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp)) {
            LocationMap(null, currentFix = fix, focusRequest = focus, orderPins = pins,
                onOrderSelected = { selectedId = it }, ordersCameraPending = !locationReady || loading,
                modifier = Modifier.fillMaxSize())
            MapLocationControl(locating, "Current location", {
                if (services.hasPermission()) scope.launch { locate(true) }
                else permissions.launch(arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION))
            }, Modifier.align(Alignment.TopEnd).padding(12.dp))
            if (!loading && pins.isEmpty()) Surface(Modifier.align(Alignment.Center).padding(24.dp),
                shape = RoundedCornerShape(20.dp), tonalElevation = 4.dp) {
                Text(if (visible.isEmpty()) "No active orders on the map yet." else "Location unavailable for these orders. Open an order below for its saved address.",
                    Modifier.padding(16.dp), style = MaterialTheme.typography.bodyMedium)
            }
            androidx.compose.animation.AnimatedVisibility(visible = selected != null, Modifier.align(Alignment.BottomCenter).padding(8.dp)) {
                selected?.let { order ->
                    val presentation = presentations.getValue(order.orderId)
                    Surface(shape = RoundedCornerShape(22.dp), shadowElevation = 6.dp,
                        color = MaterialTheme.colorScheme.surfaceContainerHigh) {
                        Column(Modifier.heightIn(max = 280.dp).verticalScroll(rememberScrollState()).padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(order.cropName, Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                                TextButton(onClick = { selectedId = null }) { Text("Close") }
                            }
                            Text("Farmer: ${farmerName(order)}", style = MaterialTheme.typography.bodyMedium)
                            Text("${order.quantity} ${order.quantityUnit}" + if (order.agreedPrice > 0) " � Rs ${order.agreedPrice} / ${order.quantityUnit}" else "",
                                style = MaterialTheme.typography.bodySmall)
                            OrderStatusChip(order.status)
                            Text(presentation.address, maxLines = 2, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodySmall)
                            distances[order.orderId]?.let { Text(String.format(Locale.getDefault(), "%.1f km away � straight-line distance", it / 1000), style = MaterialTheme.typography.labelSmall) }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(onClick = { onDetails(order) }) { Text(if (order.status == OrderStatus.AWAITING_PICKUP) "View Pickup" else "View Delivery") }
                                TextButton(enabled = presentation.destination != null, onClick = {
                                    presentation.destination?.let { if (!openOrderDirections(context, it)) message = "No navigation app is available." }
                                }) { Text("Navigate") }
                            }
                            TextButton(onClick = { onDetails(order) }) { Text("Order Details") }
                        }
                    }
                }
            }
        }
        // Every order remains reachable, including overlapping pins and legacy address-only orders.
        LazyRow(contentPadding = PaddingValues(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(visible, key = { it.orderId }) { order ->
                AssistChip(onClick = { selectedId = order.orderId }, label = {
                    Text(order.cropName + if (presentations[order.orderId]?.destination == null) " � Location unavailable" else "",
                        maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.widthIn(max = 220.dp))
                })
            }
        }
        val notice = message ?: if (hasOrderError) "Orders could not refresh. Saved orders are shown when available." else null
        notice?.let { Text(it, Modifier.padding(horizontal = 16.dp, vertical = 4.dp), style = MaterialTheme.typography.labelSmall) }
    }
}

