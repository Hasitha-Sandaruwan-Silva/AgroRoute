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
import androidx.compose.foundation.layout.heightIn
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
import com.agroroute.core.runtime.appViewModel as hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.agroroute.core.model.Order
import com.agroroute.core.model.HarvestListing
import com.agroroute.features.supplier.components.SupplierListingImage as ListingImage
import com.agroroute.core.components.profileLocationLabel
import com.agroroute.core.model.OrderRequest
import com.agroroute.core.model.OrderRequestStatus
import com.agroroute.core.navigation.Screen
import com.agroroute.core.components.AgroSnackbarHost
import com.agroroute.features.supplier.components.SupplierCard as AppCard
import com.agroroute.core.components.ChipTone
import com.agroroute.features.supplier.components.SupplierEmptyState as EmptyState
import com.agroroute.core.components.ErrorSnackbarEffect
import com.agroroute.core.components.ListSkeleton
import com.agroroute.features.supplier.components.SupplierOrderProgress as OrderProgressStrip
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
    val listingPresentation = orderListingPresentation()
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
    val cancelledRequests = uiState.requests.filter { it.status == OrderRequestStatus.CANCELLED }
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
                    if (uiState.requests.isEmpty()) {
                        item(key = "no-requests") {
                            com.agroroute.features.supplier.components.SupplierSectionHeader(
                                "No requests yet", "Your fixed-price requests will appear here", Lucide.Inbox)
                        }
                    }
                    if (uiState.requests.isNotEmpty()) {
                        item {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    text = "Order requests",
                                    style = MaterialTheme.typography.titleMedium,
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
                            SupplierOrderRequestCard(
                                request = request,
                                listing = listingPresentation[request.listingId],
                                onCancel = { viewModel.cancelRequest(request.requestId) },
                                loading = uiState.isLoading || uiState.isSubmitting
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
                                SupplierOrderRequestCard(
                                    request = request,
                                listing = listingPresentation[request.listingId],
                                    onCancel = null
                                )
                            }
                        }
                        if (cancelledRequests.isNotEmpty()) {
                            item {
                                Text(
                                    text = "Cancelled requests",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onBackground,
                                    modifier = Modifier.padding(top = 8.dp)
                                )
                            }
                            items(cancelledRequests, key = { "req-${it.requestId}" }) { request ->
                                SupplierOrderRequestCard(request = request,
                                listing = listingPresentation[request.listingId], onCancel = null)
                            }
                        }
                    }
                    item {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = "Your orders",
                                style = MaterialTheme.typography.titleMedium,
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
                            listing = listingPresentation[order.listingId],
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
internal fun SupplierOrderRequestCard(
    request: OrderRequest,
    listing: HarvestListing?,
    onCancel: (() -> Unit)?,
    loading: Boolean = false
) {
    AppCard {
        ListingImage(listing?.imageUrls?.firstOrNull(), height = 120.dp)
        Spacer(Modifier.height(14.dp))
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
            Column(verticalArrangement = Arrangement.spacedBy(6.dp), horizontalAlignment = Alignment.End) {
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
        if (request.deliveryLocation.isNotBlank()) {
            Text("Delivery: ${profileLocationLabel(request.deliveryLocation)}",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 6.dp))
        }
        if (request.isPreHarvest && request.status == OrderRequestStatus.PENDING &&
            listing?.harvestStatus == com.agroroute.core.model.HarvestStatus.EXPECTED) {
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
                    text = "This request was cancelled. Request cancellation does not deduct order points.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                )
            }
        }
        if (request.status == OrderRequestStatus.PENDING && onCancel != null) {
            TextButton(
                onClick = onCancel,
                enabled = com.agroroute.core.runtime.LocalMutationAllowed.current && (!loading),
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                modifier = Modifier
                    .align(Alignment.End)
                    .padding(top = 4.dp)
                    .heightIn(min = 48.dp)
            ) {
                Text("Cancel request", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun OrderCard(
    order: Order,
    listing: HarvestListing?,
    onCardClick: () -> Unit
) {
    AppCard(onClick = onCardClick) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ListingImage(listing?.imageUrls?.firstOrNull(), Modifier.width(92.dp), height = 112.dp)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                OrderStatusChip(order.status)
                Text(order.cropName, style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text("Farmer: ${listing?.farmerName?.takeIf(String::isNotBlank) ?: "Name unavailable"}",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("#${order.orderId.takeLast(12)}", style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Spacer(Modifier.height(14.dp))
        Text(com.agroroute.core.components.orderNextStep(order.status, true), style = MaterialTheme.typography.bodySmall)
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
        Text("Total agreed: Rs. ${order.quantity * order.agreedPrice}", style = MaterialTheme.typography.bodySmall)
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
                    text = "Pickup: ${profileLocationLabel(order.pickupLocation)}",
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
                    text = "Delivery: ${profileLocationLabel(order.deliveryLocation)}",
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
                .heightIn(min = 48.dp),
            shape = RoundedCornerShape(24.dp),
            colors = ButtonDefaults.textButtonColors(containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer)
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
