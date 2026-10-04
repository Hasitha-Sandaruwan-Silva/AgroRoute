package com.agroroute.features.farmer.orders

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
import com.github.thelacspace.icons.Lucide
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

@Composable
fun FarmerOrderDetailScreen(
    orderId: String,
    navController: NavController,
    viewModel: FarmerOrderViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

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
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    OrderDetailCard(order = uiState.selectedOrder!!)
                    OrderStatusTimeline(currentStatus = uiState.selectedOrder!!.status)

                    Text(
                        text = "Orders are not payments. No payment is processed in this app.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    when (uiState.selectedOrder!!.status) {
                        OrderStatus.AWAITING_PICKUP -> LoadingButton(
                            text = "Mark as In Transit",
                            isLoading = uiState.isLoading,
                            onClick = { viewModel.updateOrderStatus(orderId, OrderStatus.IN_TRANSIT) },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OrderStatus.IN_TRANSIT -> LoadingButton(
                            text = "Cancel Order",
                            isLoading = uiState.isLoading,
                            onClick = { viewModel.updateOrderStatus(orderId, OrderStatus.CANCELLED) },
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
                    icon = Lucide.Receipt,
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
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column {
                Text("Quantity", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("${order.quantity} ${order.quantityUnit.name}", fontWeight = FontWeight.Medium)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("Agreed Price", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    "Rs. ${order.agreedPrice}/${order.quantityUnit.name}",
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
        if (order.pickupLocation.isNotBlank()) DetailRow("Pickup Location", order.pickupLocation)
        if (order.deliveryLocation.isNotBlank()) DetailRow("Delivery Location", order.deliveryLocation)
        if (order.notes.isNotBlank()) DetailRow("Notes", order.notes)
        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
        DetailRow("Created", formatDateTime(order.createdAt))
        DetailRow("Last Updated", formatDateTime(order.updatedAt))
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}

private fun formatDateTime(timestamp: Long): String {
    val sdf = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
