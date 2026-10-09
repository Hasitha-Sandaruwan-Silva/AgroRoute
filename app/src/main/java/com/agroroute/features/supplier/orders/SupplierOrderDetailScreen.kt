package com.agroroute.features.supplier.orders

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.agroroute.core.model.Order
import com.agroroute.core.model.OrderStatus
import com.agroroute.core.navigation.Screen
import com.agroroute.core.theme.ForestDeep
import com.agroroute.core.theme.ForestMid
import com.agroroute.core.theme.ForestSoft
import com.agroroute.core.theme.LeafAccent
import com.agroroute.core.components.AgroSnackbarHost
import com.agroroute.core.components.AppCard
import com.agroroute.core.components.EmptyState
import com.agroroute.core.components.ErrorSnackbarEffect
import com.agroroute.core.components.ListSkeleton
import com.agroroute.core.components.LoadingButton
import com.agroroute.core.components.OrderStatusChip
import com.agroroute.core.components.OrderStatusTimeline
import com.agroroute.core.components.ProfilePhoneContact
import com.github.thelacspace.icons.Lucide
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
    var showLocationPicker by remember { mutableStateOf(false) }
    if (showLocationPicker && uiState.selectedOrder != null) {
        com.agroroute.core.components.LocationPickerDialog(
            initialValue = uiState.selectedOrder!!.deliveryLocation,
            onDismiss = { showLocationPicker = false },
            onConfirm = {
                viewModel.updateDeliveryLocation(orderId, it)
                showLocationPicker = false
            }
        )
    }

    // Load order details
    LaunchedEffect(orderId) {
        viewModel.loadOrderById(orderId)
    }

    ErrorSnackbarEffect(uiState.error, snackbarHostState) { viewModel.clearError() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
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
                    OrderHeroCard(order = uiState.selectedOrder!!)
                    OrderStatusTimeline(currentStatus = uiState.selectedOrder!!.status)
                    OrderDetailCard(order = uiState.selectedOrder!!)
                    if (uiState.selectedOrder!!.status in listOf(OrderStatus.AWAITING_PICKUP, OrderStatus.IN_TRANSIT)) {
                        OutlinedButton(onClick = { showLocationPicker = true }, enabled = !uiState.isLoading,
                            modifier = Modifier.fillMaxWidth()) {
                            Text("Choose / edit delivery location")
                        }
                    }
                    ProfilePhoneContact(userId = uiState.selectedOrder!!.farmerId, personLabel = "Farmer")

                    Text(
                        text = "Orders are not payments. No payment is processed in this app.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    when (uiState.selectedOrder!!.status) {
                        OrderStatus.IN_TRANSIT -> LoadingButton(
                            text = "Mark as Delivered",
                            isLoading = uiState.isLoading,
                            onClick = { viewModel.updateOrderStatus(orderId, OrderStatus.DELIVERED) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                        )
                        OrderStatus.AWAITING_PICKUP -> LoadingButton(
                            text = "Cancel Order",
                            isLoading = uiState.isLoading,
                            onClick = { viewModel.updateOrderStatus(orderId, OrderStatus.CANCELLED) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                        )
                        OrderStatus.DELIVERED -> OutlinedButton(
                            onClick = {
                                navController.navigate(
                                    Screen.SubmitReview.createRoute(
                                        orderId = orderId,
                                        revieweeId = uiState.selectedOrder!!.farmerId,
                                        revieweeName = "Farmer"
                                    )
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                        ) {
                            Icon(
                                imageVector = Lucide.Star,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("Rate Farmer", fontWeight = FontWeight.SemiBold)
                        }
                        else -> Unit
                    }
                    Spacer(Modifier.height(4.dp))
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

/** Deep forest gradient summary card: crop, status, and the key figures in white. */
@Composable
private fun OrderHeroCard(order: Order) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(ForestDeep, ForestMid, ForestSoft)
                )
            )
            .padding(22.dp)
    ) {
        Icon(
            imageVector = Lucide.Truck,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.08f),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .size(88.dp)
        )
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = order.cropName,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(12.dp))
                OrderStatusChip(order.status)
            }
            Spacer(Modifier.height(18.dp))
            HorizontalDivider(color = Color.White.copy(alpha = 0.14f), thickness = 1.dp)
            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Quantity",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "${order.quantity} ${order.quantityUnit.name}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = "Agreed Price",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Rs. ${order.agreedPrice}/${order.quantityUnit.name}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = LeafAccent
                    )
                }
            }
        }
    }
}

@Composable
private fun OrderDetailCard(order: Order) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Order ID",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = order.orderId,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
        if (order.pickupLocation.isNotBlank()) {
            DetailRow(label = "Pickup Location", value = order.pickupLocation, icon = Lucide.Store)
        }
        if (order.deliveryLocation.isNotBlank()) {
            DetailRow(label = "Delivery Location", value = order.deliveryLocation, icon = Lucide.Truck)
        }
        if (order.notes.isNotBlank()) {
            DetailRow(label = "Notes", value = order.notes, icon = Lucide.Description)
        }
        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
        DetailRow(label = "Created", value = formatDateTime(order.createdAt), icon = Lucide.CalendarDays)
        DetailRow(label = "Last Updated", value = formatDateTime(order.updatedAt), icon = Lucide.RefreshCw)
    }
}

@Composable
private fun DetailRow(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.Top
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .padding(top = 2.dp, end = 10.dp)
                    .size(16.dp)
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

private fun formatDateTime(timestamp: Long): String {
    val sdf = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
