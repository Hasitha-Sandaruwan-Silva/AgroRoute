package com.agroroute.features.farmer.orders

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import com.github.thelacspace.icons.Lucide
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.agroroute.R
import com.agroroute.core.model.Order
import com.agroroute.core.model.OrderStatus
import com.agroroute.core.navigation.Screen
import com.agroroute.core.theme.ForestDeep
import com.agroroute.core.theme.ForestMid
import com.agroroute.core.theme.ForestSoft
import com.agroroute.core.widgets.AgroSnackbarHost
import com.agroroute.core.widgets.AppCard
import com.agroroute.core.widgets.EmptyState
import com.agroroute.core.widgets.ErrorSnackbarEffect
import com.agroroute.core.widgets.ListSkeleton
import com.agroroute.core.widgets.OrderProgressStrip
import com.agroroute.core.widgets.OrderStatusChip
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private enum class OrderRange(val label: String, val days: Int?) {
    ALL_TIME("All time", null),
    LAST_30_DAYS("30 days", 30),
    LAST_90_DAYS("90 days", 90)
}

@Composable
fun FarmerOrdersScreen(
    navController: NavController,
    viewModel: FarmerOrderViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var searchQuery by remember { mutableStateOf("") }
    var selectedStatus by remember { mutableStateOf<OrderStatus?>(null) }
    var selectedRange by remember { mutableStateOf(OrderRange.ALL_TIME) }
    val now = remember { System.currentTimeMillis() }

    val rangedOrders = remember(uiState.orders, selectedRange, now) {
        selectedRange.days?.let { days ->
            val since = now - days * 24L * 60L * 60L * 1000L
            uiState.orders.filter { it.createdAt >= since }
        } ?: uiState.orders
    }
    val visibleOrders = remember(rangedOrders, searchQuery, selectedStatus, uiState.supplierNames) {
        rangedOrders.filter { order ->
            val matchesSearch = searchQuery.isBlank() ||
                order.cropName.contains(searchQuery, ignoreCase = true) ||
                order.pickupLocation.contains(searchQuery, ignoreCase = true) ||
                uiState.supplierNames[order.orderId]?.contains(searchQuery, ignoreCase = true) == true
            matchesSearch && (selectedStatus == null || order.status == selectedStatus)
        }
    }
    val deliveredOrders = rangedOrders.filter { it.status == OrderStatus.DELIVERED }
    val deliveredQuantity = deliveredOrders.sumOf { it.quantity }
    val deliveredValue = deliveredOrders.sumOf { it.quantity * it.agreedPrice }

    LaunchedEffect(Unit) {
        viewModel.loadOrdersForFarmer(viewModel.currentFarmerId)
    }

    ErrorSnackbarEffect(uiState.error, snackbarHostState) { viewModel.clearError() }

    Column(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Order history", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(
                text = "Orders are not payments. No payment is processed in this app.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(OrderRange.entries.size) { index ->
                    val range = OrderRange.entries[index]
                    FilterChip(
                        selected = selectedRange == range,
                        onClick = { selectedRange = range },
                        label = { Text(range.label) },
                        modifier = Modifier.heightIn(min = 48.dp)
                    )
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .background(
                        Brush.linearGradient(listOf(ForestDeep, ForestMid, ForestSoft))
                    )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    Text(
                        text = "Delivered order value - ${selectedRange.label}",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White.copy(alpha = 0.72f)
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.padding(top = 12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            SummaryValue("All orders", rangedOrders.size.toString())
                            SummaryValue("Delivered", deliveredOrders.size.toString())
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            SummaryValue("Delivered quantity", deliveredQuantity.toString())
                            SummaryValue("Order value", "Rs. $deliveredValue")
                        }
                    }
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 14.dp),
                        thickness = 1.dp,
                        color = Color.White.copy(alpha = 0.14f)
                    )
                    Text(
                        text = "Based on delivered orders; this does not indicate payment status.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.72f)
                    )
                }
            }
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("Search crop, supplier, or pickup") },
                leadingIcon = { Icon(Lucide.Search, contentDescription = null) },
                singleLine = true,
                shape = MaterialTheme.shapes.medium,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                ),
                modifier = Modifier.fillMaxWidth()
            )
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    FilterChip(
                        selected = selectedStatus == null,
                        onClick = { selectedStatus = null },
                        label = { Text("All statuses") },
                        modifier = Modifier.heightIn(min = 48.dp)
                    )
                }
                items(OrderStatus.entries.size) { index ->
                    val status = OrderStatus.entries[index]
                    FilterChip(
                        selected = selectedStatus == status,
                        onClick = { selectedStatus = status },
                        label = { Text(status.displayLabel()) },
                        modifier = Modifier.heightIn(min = 48.dp)
                    )
                }
            }
            Text(
                text = stringResource(R.string.orders_payment_caption),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp)
            )
        }
        Box(modifier = Modifier.weight(1f)) {
            when {
                uiState.isLoading && uiState.orders.isEmpty() -> ListSkeleton()
                uiState.orders.isEmpty() -> Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    EmptyState(
                        icon = Lucide.Receipt,
                        title = "No Orders Yet",
                        subtitle = "Your order history will appear here"
                    )
                }
                visibleOrders.isEmpty() -> Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    EmptyState(
                        icon = Lucide.Search,
                        title = "No matching orders",
                        subtitle = "Try changing the search, date range, or status"
                    )
                }
                else -> LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(visibleOrders) { order ->
                        OrderCard(
                            order = order,
                            supplierName = uiState.supplierNames[order.orderId],
                            onCardClick = {
                                navController.navigate(Screen.FarmerOrderDetail.createRoute(order.orderId))
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
private fun OrderCard(
    order: Order,
    supplierName: String?,
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
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(8.dp))
            OrderStatusChip(order.status)
        }
        Spacer(Modifier.height(8.dp))
        supplierName?.let {
            Text(
                "Supplier: $it",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(4.dp))
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            Text(
                text = "${order.quantity} ${order.quantityUnit.name}",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Rs. ${order.agreedPrice}/${order.quantityUnit.name}",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
        }
        if (order.pickupLocation.isNotBlank()) {
            Text(
                text = "Pickup: ${order.pickupLocation}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
        HorizontalDivider(
            modifier = Modifier.padding(vertical = 12.dp),
            thickness = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        )
        OrderProgressStrip(currentStatus = order.status)
        Text(
            text = "Created ${formatDate(order.createdAt)}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 12.dp)
        )
    }
}

@Composable
private fun SummaryValue(label: String, value: String) {
    Column {
        Text(label, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.66f))
        Text(
            value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
    }
}

private fun OrderStatus.displayLabel(): String = name.lowercase()
    .replace('_', ' ')
    .replaceFirstChar { it.uppercase() }

private fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
