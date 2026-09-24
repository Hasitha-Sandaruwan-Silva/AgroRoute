package com.agroroute.features.supplier.orders

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.agroroute.core.model.Order
import com.agroroute.core.model.OrderStatus
import com.agroroute.core.widgets.AgroSnackbarHost
import com.agroroute.core.widgets.AppCard
import com.agroroute.core.widgets.EmptyState
import com.agroroute.core.widgets.ErrorSnackbarEffect
import com.agroroute.core.widgets.ListSkeleton
import com.agroroute.core.widgets.LoadingButton
import com.agroroute.core.widgets.OrderStatusChip
import com.agroroute.core.widgets.OrderStatusTimeline
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Screen showing full details of a single [Order] for supplier.
 */
@Composable
fun SupplierOrderDetailScreen(
    orderId: String,
    navController: NavController,
    viewModel: SupplierOrderViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    // Load order details
    LaunchedEffect(orderId) {
        viewModel.loadOrderById(orderId)
    }

    ErrorSnackbarEffect(uiState.error, snackbarHostState) { viewModel.clearError() }

    Box(modifier = Modifier.fillMaxSize()) {
        when {
            uiState.isLoading && uiState.selectedOrder == null -> ListSkeleton(count = 2)
            uiState.selectedOrder != null -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    OrderDetailCard(order = uiState.selectedOrder!!)
                    OrderStatusTimeline(currentStatus = uiState.selectedOrder!!.status)

                    when (uiState.selectedOrder!!.status) {
                        OrderStatus.AWAITING_PICKUP -> LoadingButton(
                            text = "Mark as In Transit",
                            isLoading = uiState.isLoading,
                            onClick = { viewModel.updateOrderStatus(orderId, OrderStatus.IN_TRANSIT) },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OrderStatus.IN_TRANSIT -> LoadingButton(
                            text = "Mark as Delivered",
                            isLoading = uiState.isLoading,
                            onClick = { viewModel.updateOrderStatus(orderId, OrderStatus.DELIVERED) },
                            modifier = Modifier.fillMaxWidth()
                        )
                        else -> Unit
                    }
                }
            }
            else -> Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                EmptyState(
                    icon = Icons.Default.Receipt,
                    title = "Order not found",
                    subtitle = "This order may have been removed or is no longer available"
                )
            }
        }

        AgroSnackbarHost(
            snackbarHostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@Composable
private fun OrderDetailCard(order: Order) {
    AppCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = order.cropName,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            OrderStatusChip(order.status)
        }
        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
        DetailRow(label = "Order ID", value = order.orderId)
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "Quantity",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${order.quantity} ${order.quantityUnit.name}",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "Agreed Price",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Rs. ${order.agreedPrice}/${order.quantityUnit.name}",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
        if (order.pickupLocation.isNotBlank()) {
            DetailRow(label = "Pickup Location", value = order.pickupLocation)
        }
        if (order.deliveryLocation.isNotBlank()) {
            DetailRow(label = "Delivery Location", value = order.deliveryLocation)
        }
        if (order.notes.isNotBlank()) {
            DetailRow(label = "Notes", value = order.notes)
        }
        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
        DetailRow(label = "Created", value = formatDateTime(order.createdAt))
        DetailRow(label = "Last Updated", value = formatDateTime(order.updatedAt))
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

private fun formatDateTime(timestamp: Long): String {
    val sdf = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
