package com.agroroute.features.supplier.orders

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import com.github.thelacspace.icons.Lucide
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.agroroute.core.model.OrderRequest
import com.agroroute.core.model.OrderRequestStatus
import com.agroroute.core.navigation.Screen
import com.agroroute.core.widgets.AgroSnackbarHost
import com.agroroute.core.widgets.AppCard
import com.agroroute.core.widgets.ChipTone
import com.agroroute.core.widgets.EmptyState
import com.agroroute.core.widgets.ErrorSnackbarEffect
import com.agroroute.core.widgets.ListSkeleton
import com.agroroute.core.widgets.OrderRequestStatusChip
import com.agroroute.core.widgets.OrderStatusChip
import com.agroroute.core.widgets.StatusChip
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Screen showing the supplier's pending order requests, rejected requests,
 * and confirmed orders.
 */
@Composable
fun SupplierOrdersScreen(
    navController: NavController,
    viewModel: SupplierOrderViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.loadOrdersForSupplier(viewModel.currentSupplierId)
    }

    ErrorSnackbarEffect(uiState.error, snackbarHostState) { viewModel.clearError() }

    val pendingRequests = uiState.requests.filter { it.status == OrderRequestStatus.PENDING }
    val rejectedRequests = uiState.requests.filter { it.status == OrderRequestStatus.REJECTED }
    val isEmpty = uiState.requests.isEmpty() && uiState.orders.isEmpty()

    Column(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxSize()) {
            when {
                uiState.isLoading && isEmpty -> ListSkeleton()
                isEmpty -> Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    EmptyState(
                        icon = Lucide.Receipt,
                        title = "No Orders Yet",
                        subtitle = "Your order requests and order history will appear here"
                    )
                }
                else -> LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (uiState.requests.isNotEmpty()) {
                        item {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    text = "Order requests",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${pendingRequests.size} pending",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(top = 3.dp)
                                )
                            }
                        }
                        items(pendingRequests, key = { "req-${it.requestId}" }) { request ->
                            OrderRequestCard(
                                request = request,
                                onCancel = { viewModel.cancelRequest(request.requestId) }
                            )
                        }
                        if (rejectedRequests.isNotEmpty()) {
                            item {
                                Text(
                                    text = "Rejected requests",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(top = 8.dp)
                                )
                            }
                            items(rejectedRequests, key = { "req-${it.requestId}" }) { request ->
                                OrderRequestCard(
                                    request = request,
                                    onCancel = null
                                )
                            }
                        }
                    }
                    item {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = "Your orders",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(top = if (uiState.requests.isEmpty()) 0.dp else 8.dp)
                            )
                            Text(
                                text = if (uiState.orders.size == 1) "1 order" else "${uiState.orders.size} orders",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 3.dp)
                            )
                        }
                    }
                    items(uiState.orders, key = { "order-${it.orderId}" }) { order ->
                        OrderCard(
                            order = order,
                            onCardClick = {
                                navController.navigate(Screen.SupplierOrderDetail.createRoute(order.orderId))
                            }
                        )
                    }
                }
            }
            AgroSnackbarHost(
                snackbarHostState = snackbarHostState,
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}

@Composable
private fun OrderRequestCard(
    request: OrderRequest,
    onCancel: (() -> Unit)?
) {
    AppCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = request.cropName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (request.isPreHarvest) {
                    StatusChip(label = "Pre-harvest", tone = ChipTone.Tertiary)
                }
                OrderRequestStatusChip(request.status)
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Quantity: ${request.requestedQuantity} ${request.quantityUnit.name}",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = "Listed price: Rs. ${request.listedPrice}/${request.quantityUnit.name}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = "Farmer: ${request.farmerName.ifBlank { "Name unavailable" }}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = "Requested ${formatDate(request.createdAt)}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (request.isPreHarvest && request.status == OrderRequestStatus.PENDING) {
            Text(
                text = "Waiting for harvest confirmation — the farmer can confirm only after " +
                    "the crop is harvested and the actual quantity is known.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
        if (request.isPreHarvest && request.status == OrderRequestStatus.CANCELLED) {
            Text(
                text = "Cancelled — the farmer marked this harvest as unavailable.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
        if (request.status == OrderRequestStatus.PENDING && onCancel != null) {
            TextButton(onClick = onCancel, modifier = Modifier.align(Alignment.End)) {
                Text("Cancel request")
            }
        }
    }
}

@Composable
private fun OrderCard(
    order: Order,
    onCardClick: () -> Unit
) {
    AppCard(onClick = onCardClick) {
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
            OrderStatusChip(order.status)
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Quantity", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    "${order.quantity} ${order.quantityUnit.name}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text("Agreed price", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    "Rs. ${order.agreedPrice}/${order.quantityUnit.name}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
        if (order.pickupLocation.isNotBlank()) {
            Text(
                text = "Pickup: ${order.pickupLocation}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
        if (order.deliveryLocation.isNotBlank()) {
            Text(
                text = "Delivery: ${order.deliveryLocation}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
        Text(
            text = "Created ${formatDate(order.createdAt)}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp)
        )
        TextButton(
            onClick = onCardClick,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("View order details")
        }
    }
}

private fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
