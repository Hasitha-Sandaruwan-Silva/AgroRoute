@file:OptIn(ExperimentalMaterial3Api::class)

package com.agroroute.farmer

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Receipt
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
import com.agroroute.common.navigation.Screen
import com.agroroute.common.ui.EmptyState
import java.text.SimpleDateFormat
import java.util.*

/**
 * Screen showing the current status and timeline of orders for the farmer user.
 */
@Composable
fun FarmerOrdersScreen(
    navController: NavController,
    viewModel: FarmerOrderViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Load orders for the current user (placeholder ID for now)
    LaunchedEffect(Unit) {
        viewModel.loadOrdersForFarmer("current_farmer_id") // TODO: Get from auth context
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
            uiState.orders.isEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    EmptyState(
                        icon = Icons.Default.Receipt,
                        title = "No Orders Yet",
                        subtitle = "Your order history will appear here"
                    )
                }
            }
            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(uiState.orders) { order ->
                        OrderCard(
                            order = order,
                            onCardClick = {
                                navController.navigate(Screen.FarmerOrderDetail.createRoute(order.orderId))
                            }
                        )
                    }
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
private fun OrderCard(
    order: Order,
    onCardClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onCardClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = order.cropName,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                OrderStatusBadge(order.status)
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "${order.quantity} ${order.quantityUnit.name}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Rs. ${order.agreedPrice}/${order.quantityUnit.name}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (order.pickupLocation.isNotBlank()) {
                Text(
                    text = "Pickup: ${order.pickupLocation}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (order.deliveryLocation.isNotBlank()) {
                Text(
                    text = "Delivery: ${order.deliveryLocation}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Created ${formatDate(order.createdAt)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
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

private fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
