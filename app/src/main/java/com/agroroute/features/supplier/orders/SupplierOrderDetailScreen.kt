package com.agroroute.features.supplier.orders

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.agroroute.core.components.*
import com.agroroute.core.model.OrderStatus
import com.agroroute.core.navigation.Screen
import com.github.thelacspace.icons.Lucide
import java.text.DateFormat
import java.util.Date

@Composable
fun SupplierOrderDetailScreen(orderId: String, navController: NavController,
    viewModel: SupplierOrderViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    var counterpartName by remember(orderId) { mutableStateOf<String?>(null) }
    var showLocationPicker by remember { mutableStateOf(false) }
    if (showLocationPicker && state.selectedOrder != null) {
        LocationPickerDialog(initialValue = state.selectedOrder!!.deliveryLocation,
            onDismiss = { showLocationPicker = false }, onConfirm = {
                viewModel.updateDeliveryLocation(orderId, it); showLocationPicker = false
            })
    }
    LaunchedEffect(orderId) { viewModel.loadOrderById(orderId) }
    ErrorSnackbarEffect(state.error, snackbar) { viewModel.clearError() }
    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        val order = state.selectedOrder
        when {
            state.isLoading && order == null -> ListSkeleton(count = 2)
            order != null -> {
                Column(Modifier.fillMaxSize().imePadding().verticalScroll(rememberScrollState())
                    .padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OrderSummarySection(order, forSupplier = true, counterpartName = counterpartName)
                    OrderPickupSection(orderId, order.status, forSupplier = true,
                        verification = state.pickup, loading = state.pickupLoading || state.isLoading || state.isSubmitting,
                        error = state.pickupError, onRefresh = { viewModel.loadPickupVerification(orderId, generateNewCode = true) },
                        onCollect = { viewModel.updateOrderStatus(orderId, OrderStatus.IN_TRANSIT) })
                    OrderLifecycleActions(order, forSupplier = true, loading = state.isLoading || state.isSubmitting || state.pickupLoading,
                        onCancel = { viewModel.updateOrderStatus(orderId, OrderStatus.CANCELLED) },
                        onDeliver = { viewModel.updateOrderStatus(orderId, OrderStatus.DELIVERED) })
                    OrderReviewSection(order, order.farmerId, "Farmer", onReview = {
                        navController.navigate(Screen.SubmitReview.createRoute(orderId, order.farmerId, "Farmer"))
                    })
                    OrderLocationSection(order, forSupplier = true)
                    if (order.status in listOf(OrderStatus.AWAITING_PICKUP, OrderStatus.IN_TRANSIT)) {
                        OutlinedButton(onClick = { showLocationPicker = true }, enabled = !state.isLoading && !state.isSubmitting,
                            modifier = Modifier.fillMaxWidth()) { Text("Choose / edit delivery location") }
                    }
                    AppCard { ProfilePhoneContact(userId = order.farmerId, personLabel = "Farmer", onNameLoaded = { counterpartName = it }) }
                    AppCard {
                        Text("Order details", style = MaterialTheme.typography.titleMedium)
                        Text("Order ID: ${order.orderId}", style = MaterialTheme.typography.bodySmall)
                        if (order.notes.isNotBlank()) Text(order.notes)
                        Text("Created: ${DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(order.createdAt))}",
                            style = MaterialTheme.typography.bodySmall)
                        Text("Updated: ${DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(order.updatedAt))}",
                            style = MaterialTheme.typography.bodySmall)
                    }
                    Text("Orders are not payments. No payment is processed in this app.",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            else -> Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
                EmptyState(icon = Lucide.Receipt, title = "Order unavailable",
                    subtitle = "The order could not be loaded. Check your connection and retry.")
                TextButton(onClick = { viewModel.loadOrderById(orderId) }) { Text("Retry") }
            }
        }
        AgroSnackbarHost(snackbarHostState = snackbar, modifier = Modifier.align(Alignment.BottomCenter))
    }
}
