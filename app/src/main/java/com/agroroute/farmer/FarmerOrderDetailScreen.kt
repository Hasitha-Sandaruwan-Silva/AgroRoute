@file:OptIn(ExperimentalMaterial3Api::class)

package com.agroroute.farmer

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.agroroute.common.model.Order
import com.agroroute.common.model.OrderStatus
import java.text.SimpleDateFormat
import java.util.*

/**
 * Screen showing full details of a single [Order] for farmers.
 */
@Composable
fun FarmerOrderDetailScreen(
    orderId: String,
    navController: NavController,
    viewModel: FarmerOrderViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Load order details
    LaunchedEffect(orderId) {
        viewModel.loadOrderById(orderId)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            uiState.selectedOrder != null -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    OrderDetailCard(order = uiState.selectedOrder!!)

                    // Status update actions (placeholder for now)
                    if (uiState.selectedOrder!!.status == OrderStatus.AWAITING_PICKUP) {
                        Button(
                            onClick = {
                                viewModel.updateOrderStatus(orderId, OrderStatus.IN_TRANSIT)
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Mark as In Transit")
                        }
                    } else if (uiState.selectedOrder!!.status == OrderStatus.IN_TRANSIT) {
                        Button(
                            onClick = {
                                viewModel.updateOrderStatus(orderId, OrderStatus.DELIVERED)
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Mark as Delivered")
                        }
                    }
                }
            }
            else -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Order not found",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        }

        // Error handling
        uiState.error?.let { error ->
            LaunchedEffect(error) {
                // In a real app, you'd show a snackbar
            }
        }
    }
}

@Composable
private fun OrderDetailCard(order: Order) {
    Card(
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header with crop name and status
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
                OrderStatusBadge(order.status)
            }

            Divider()

            // Order ID
            DetailRow(label = "Order ID", value = order.orderId)

            // Quantity and price
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
                Column {
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

            Divider()

            // Locations
            if (order.pickupLocation.isNotBlank()) {
                DetailRow(label = "Pickup Location", value = order.pickupLocation)
            }
            if (order.deliveryLocation.isNotBlank()) {
                DetailRow(label = "Delivery Location", value = order.deliveryLocation)
            }

            // Notes
            if (order.notes.isNotBlank()) {
                DetailRow(label = "Notes", value = order.notes)
            }

            Divider()

            // Timestamps
            DetailRow(label = "Created", value = formatDateTime(order.createdAt))
            DetailRow(label = "Last Updated", value = formatDateTime(order.updatedAt))
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
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

@Composable
private fun OrderStatusBadge(status: OrderStatus) {
    val (label, containerColor, contentColor) = when (status) {
        OrderStatus.AWAITING_PICKUP -> Triple(
            "Awaiting Pickup",
            MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.onPrimaryContainer
        )
        OrderStatus.IN_TRANSIT -> Triple(
            "In Transit",
            MaterialTheme.colorScheme.secondaryContainer,
            MaterialTheme.colorScheme.onSecondaryContainer
        )
        OrderStatus.DELIVERED -> Triple(
            "Delivered",
            MaterialTheme.colorScheme.tertiaryContainer,
            MaterialTheme.colorScheme.onTertiaryContainer
        )
        OrderStatus.CANCELLED -> Triple(
            "Cancelled",
            MaterialTheme.colorScheme.errorContainer,
            MaterialTheme.colorScheme.onErrorContainer
        )
    }

    Surface(
        color = containerColor,
        shape = MaterialTheme.shapes.small
    ) {
        Text(
            text = label,
            color = contentColor,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

private fun formatDateTime(timestamp: Long): String {
    val sdf = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
