package com.agroroute.features.supplier.orders

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.agroroute.core.model.Order
import com.agroroute.core.model.OrderRequest
import com.agroroute.core.model.OrderRequestStatus
import com.agroroute.core.navigation.Screen
import com.agroroute.core.components.AgroSnackbarHost
import com.agroroute.core.components.AppCard
import com.agroroute.core.components.ChipTone
import com.agroroute.core.components.EmptyState
import com.agroroute.core.components.ErrorSnackbarEffect
import com.agroroute.core.components.ListSkeleton
import com.agroroute.core.components.OrderProgressStrip
import com.agroroute.core.components.OrderRequestStatusChip
import com.agroroute.core.components.OrderStatusChip
import com.agroroute.core.components.StatusChip
import com.github.thelacspace.icons.Lucide
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
    var ordersLoaded by remember { mutableStateOf(false) }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        if (ordersLoaded) {
            viewModel.refreshOrdersForSupplier(viewModel.currentSupplierId)
        } else {
            ordersLoaded = true
            viewModel.loadOrdersForSupplier(viewModel.currentSupplierId)
        }
    }

    ErrorSnackbarEffect(uiState.error, snackbarHostState) { viewModel.clearError() }

    val pendingRequests = uiState.requests.filter { it.status == OrderRequestStatus.PENDING }
    val rejectedRequests = uiState.requests.filter { it.status == OrderRequestStatus.REJECTED }
    val isEmpty = uiState.requests.isEmpty() && uiState.orders.isEmpty()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Text(
            text = "Orders are not payments. No payment is processed in this app.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
        )
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
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onBackground
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
                                    color = MaterialTheme.colorScheme.onBackground,
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
                                color = MaterialTheme.colorScheme.onBackground,
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
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                            shape = RoundedCornerShape(12.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Lucide.Receipt,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(Modifier.width(12.dp))
                Text(
                    text = request.cropName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(Modifier.width(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (request.isPreHarvest) {
                    StatusChip(label = "Pre-harvest", tone = ChipTone.Tertiary)
                }
                OrderRequestStatusChip(request.status)
            }
        }
        Spacer(Modifier.height(12.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        Spacer(Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Quantity: ${request.requestedQuantity} ${request.quantityUnit.name}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Text(
                text = "Listed price: Rs. ${request.listedPrice}/${request.quantityUnit.name}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold
            )
        }
        Text(
            text = "Farmer: ${request.farmerName.ifBlank { "Name unavailable" }}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 2.dp)
        )
        Text(
            text = "Requested ${formatDate(request.createdAt)}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (request.isPreHarvest && request.status == OrderRequestStatus.PENDING) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            ) {
                Text(
                    text = "Waiting for harvest confirmation — the farmer can confirm only after " +
                        "the crop is harvested and the actual quantity is known.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                )
            }
        }
        if (request.isPreHarvest && request.status == OrderRequestStatus.CANCELLED) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            ) {
                Text(
                    text = "Cancelled — the farmer marked this harvest as unavailable.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                )
            }
        }
        if (request.status == OrderRequestStatus.PENDING && onCancel != null) {
            TextButton(
                onClick = onCancel,
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                modifier = Modifier
                    .align(Alignment.End)
                    .padding(top = 4.dp)
                    .height(48.dp)
            ) {
                Text("Cancel request", fontWeight = FontWeight.SemiBold)
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
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(8.dp))
            OrderStatusChip(order.status)
        }
        Spacer(Modifier.height(14.dp))
        OrderProgressStrip(currentStatus = order.status)
        Spacer(Modifier.height(14.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Quantity", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    "${order.quantity} ${order.quantityUnit.name}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text("Agreed price", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    "Rs. ${order.agreedPrice}/${order.quantityUnit.name}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
        if (order.pickupLocation.isNotBlank()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 8.dp)
            ) {
                Icon(
                    imageVector = Lucide.Store,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "Pickup: ${order.pickupLocation}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        if (order.deliveryLocation.isNotBlank()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 4.dp)
            ) {
                Icon(
                    imageVector = Lucide.Truck,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "Delivery: ${order.deliveryLocation}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        Text(
            text = "Created ${formatDate(order.createdAt)}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp)
        )
        TextButton(
            onClick = onCardClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        ) {
            Text("View order details", fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.width(4.dp))
            Icon(
                imageVector = Lucide.ChevronRight,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

private fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
