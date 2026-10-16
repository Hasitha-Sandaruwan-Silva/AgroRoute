package com.agroroute.core.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.agroroute.core.runtime.appViewModel as hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.agroroute.core.model.Order
import com.agroroute.core.model.OrderStatus
import com.agroroute.features.reviews.ReviewViewModel

fun orderNextStep(status: OrderStatus, forSupplier: Boolean): String = when (status) {
    OrderStatus.AWAITING_PICKUP -> if (forSupplier) "Collect from the Farmer and confirm pickup" else "Supplier pickup pending · Verify physical handover"
    OrderStatus.IN_TRANSIT -> if (forSupplier) "Transporting order · Confirm delivery at destination" else "Supplier is transporting your order"
    OrderStatus.DELIVERED -> "Delivered"
    OrderStatus.CANCELLED -> "Cancelled"
}

@Composable
fun OrderSummarySection(order: Order, forSupplier: Boolean, counterpartName: String? = null) {
    AppCard {
        Text(order.cropName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        OrderStatusChip(order.status)
        Text("${if (forSupplier) "Farmer" else "Supplier"}: ${counterpartName ?: "Name unavailable"}",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text("${order.quantity} ${order.quantityUnit.name} · Rs. ${order.agreedPrice}/${order.quantityUnit.name}")
        Text("Total agreed: Rs. ${order.quantity * order.agreedPrice}", style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(8.dp))
        if (forSupplier && com.agroroute.features.supplier.components.LocalSupplierPresentation.current) {
            com.agroroute.features.supplier.components.SupplierOrderProgress(order.status)
        } else if (!forSupplier && com.agroroute.features.farmer.components.LocalFarmerPresentation.current) {
            com.agroroute.features.farmer.components.FarmerOrderProgress(order.status)
        } else OrderProgressStrip(order.status)
        Text(orderNextStep(order.status, forSupplier), style = MaterialTheme.typography.bodySmall)
        if (order.status == OrderStatus.AWAITING_PICKUP && !forSupplier) {
            Text("Acceptance reward: 1 point, awarded once. Your profile shows your confirmed points.", style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (order.status == OrderStatus.DELIVERED) {
            Text("Delivery reward: +6 each, once for eligible orders. Your profile shows the confirmed total.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun OrderLifecycleActions(order: Order, forSupplier: Boolean, loading: Boolean,
                          onCancel: () -> Unit, onDeliver: () -> Unit = {}) {
    var confirmingCancel by remember(order.orderId) { mutableStateOf(false) }
    when (order.status) {
        OrderStatus.AWAITING_PICKUP -> OutlinedButton(
            onClick = { confirmingCancel = true }, enabled = com.agroroute.core.runtime.LocalMutationAllowed.current && (!loading),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
        ) { Text("Cancel Order") }
        OrderStatus.IN_TRANSIT -> if (forSupplier) {
            Text("Confirm only after delivery. Eligible delivery awards +6 to each participant once.",
                style = MaterialTheme.typography.bodySmall)
            LoadingButton(text = "Confirm Delivery", isLoading = loading, onClick = onDeliver,
                modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp))
        }
        else -> Unit
    }
    if (confirmingCancel && order.status == OrderStatus.AWAITING_PICKUP) {
        AlertDialog(onDismissRequest = { if (!loading) confirmingCancel = false },
            title = { Text("Cancel ${order.cropName} order?") },
            text = { Text("Cancelling this order will deduct 2 points from your score only. Your score cannot go below 0. The reserved stock will be released.") },
            dismissButton = { TextButton(onClick = { confirmingCancel = false }, enabled = !loading) { Text("Keep Order") } },
            confirmButton = { TextButton(onClick = { confirmingCancel = false; onCancel() }, enabled = com.agroroute.core.runtime.LocalMutationAllowed.current && (!loading),
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) { Text("Cancel Order") } })
    }
}

@Composable
fun OrderReviewSection(order: Order, counterpartId: String, counterpartLabel: String,
                       onReview: () -> Unit, viewModel: ReviewViewModel = hiltViewModel()) {
    if (order.status != OrderStatus.DELIVERED) return
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(order.orderId, counterpartId) { viewModel.checkHasReviewed(order.orderId, counterpartId) }
    ReviewEligibilitySection(state.isChecking, state.canReview, state.hasReviewed, state.error,
        counterpartLabel, onReview, { viewModel.checkHasReviewed(order.orderId, counterpartId) })
}

@Composable
fun ReviewEligibilitySection(checking: Boolean, eligible: Boolean, reviewed: Boolean, error: String?,
                             counterpartLabel: String, onReview: () -> Unit, onRetry: () -> Unit) {
    when {
        checking -> Text("Checking review eligibility…", style = MaterialTheme.typography.bodySmall)
        reviewed -> Text("Review submitted", color = MaterialTheme.colorScheme.primary)
        error != null -> { Text("Could not check review eligibility. Please retry.", style = MaterialTheme.typography.bodySmall)
            TextButton(onClick = onRetry) { Text("Retry review check") } }
        eligible -> OutlinedButton(onClick = onReview, enabled = com.agroroute.core.runtime.LocalMutationAllowed.current, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("Rate $counterpartLabel") }
        else -> Text("Reviews are available to participants after delivery.", style = MaterialTheme.typography.bodySmall)
    }
}
