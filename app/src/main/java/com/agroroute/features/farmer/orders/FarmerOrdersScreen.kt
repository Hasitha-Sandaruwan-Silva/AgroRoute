package com.agroroute.features.farmer.orders

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.agroroute.core.runtime.appViewModel as hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.agroroute.R
import com.agroroute.core.components.AgroSnackbarHost
import com.agroroute.core.components.ErrorSnackbarEffect
import com.agroroute.core.components.ListSkeleton
import com.agroroute.core.components.OrderProgressStrip
import com.agroroute.core.components.OrderStatusChip
import com.agroroute.core.model.Order
import com.agroroute.core.model.OrderStatus
import com.agroroute.core.navigation.Screen
import com.github.thelacspace.icons.Lucide
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


private enum class OrderRange(
    val label: String,
    val days: Int?
) {
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

    val snackbarHostState = remember {
        SnackbarHostState()
    }

    var searchQuery by remember {
        mutableStateOf("")
    }

    var selectedStatus by remember {
        mutableStateOf<OrderStatus?>(null)
    }

    var selectedRange by remember {
        mutableStateOf(OrderRange.ALL_TIME)
    }

    var ordersLoaded by remember {
        mutableStateOf(false)
    }

    val now = remember {
        System.currentTimeMillis()
    }


    // =========================================================
    // EXISTING FILTERING LOGIC
    // =========================================================

    val rangedOrders = remember(
        uiState.orders,
        selectedRange,
        now
    ) {
        selectedRange.days?.let { days ->

            val since =
                now - days * 24L * 60L * 60L * 1000L

            uiState.orders.filter {
                it.createdAt >= since
            }

        } ?: uiState.orders
    }


    val visibleOrders = remember(
        rangedOrders,
        searchQuery,
        selectedStatus,
        uiState.supplierNames
    ) {
        rangedOrders.filter { order ->

            val matchesSearch =
                searchQuery.isBlank() ||
                        order.cropName.contains(
                            searchQuery,
                            ignoreCase = true
                        ) ||
                        order.pickupLocation.contains(
                            searchQuery,
                            ignoreCase = true
                        ) ||
                        uiState.supplierNames[order.orderId]
                            ?.contains(
                                searchQuery,
                                ignoreCase = true
                            ) == true

            matchesSearch &&
                    (
                            selectedStatus == null ||
                                    order.status == selectedStatus
                            )
        }
    }


    val deliveredOrders = rangedOrders.filter {
        it.status == OrderStatus.DELIVERED
    }

    val deliveredQuantity =
        deliveredOrders.sumOf {
            it.quantity
        }

    val deliveredValue =
        deliveredOrders.sumOf {
            it.quantity * it.agreedPrice
        }


    LifecycleEventEffect(
        Lifecycle.Event.ON_RESUME
    ) {
        if (ordersLoaded) {

            viewModel.refreshOrdersForFarmer(
                viewModel.currentFarmerId
            )

        } else {

            ordersLoaded = true

            viewModel.loadOrdersForFarmer(
                viewModel.currentFarmerId
            )
        }
    }


    ErrorSnackbarEffect(
        uiState.error,
        snackbarHostState
    ) {
        viewModel.clearError()
    }


    // =========================================================
    // SCREEN
    // =========================================================

    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item(key = "controls") {
                FarmerOrdersControls {
                    OrdersHeader(rangedOrders.size, selectedRange)
                    Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        RangeSelector(selectedRange) { selectedRange = it }
                        OrderStatsStrip(deliveredOrders.size, deliveredQuantity.toString(), "Rs. $deliveredValue")
                        OrderSearchField(searchQuery, { searchQuery = it }, { searchQuery = "" })
                        StatusSelector(selectedStatus) { selectedStatus = it }
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text("Orders", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.width(8.dp))
                            Text("${visibleOrders.size}", style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            }
            when {
                uiState.isLoading && uiState.orders.isEmpty() -> item { ListSkeleton() }
                uiState.orders.isEmpty() -> item {
                    FarmerOrdersControls { OrdersEmptyState("No orders yet",
                        "Your orders will appear here once a supplier order is created.", false) }
                }
                visibleOrders.isEmpty() -> item {
                    FarmerOrdersControls { OrdersEmptyState("Nothing found",
                        "Try a different search, status, or time range.", true) }
                }
                else -> items(visibleOrders, key = { it.orderId }) { order ->
                    Box(Modifier.padding(horizontal = 16.dp)) {
                        ModernOrderCard(order, uiState.supplierNames[order.orderId]) {
                            navController.navigate(Screen.FarmerOrderDetail.createRoute(order.orderId))
                        }
                    }
                }
            }
        }
        AgroSnackbarHost(snackbarHostState, Modifier.align(Alignment.BottomCenter))
    }
}

@Composable
private fun FarmerOrdersControls(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onSurface, content = content)
}

@Composable
private fun OrdersHeader(
    totalOrders: Int,
    selectedRange: OrderRange
) {

    com.agroroute.core.components.PremiumHero(Modifier.padding(16.dp)) {
        Icon(Lucide.Receipt, null, Modifier.size(28.dp))
        Text("Orders", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text("Manage pickups, deliveries and completed harvest orders.", style = MaterialTheme.typography.bodyMedium)
        Text("$totalOrders orders | ${selectedRange.label}", style = MaterialTheme.typography.labelLarge)
    }
}


// =============================================================
// RANGE SELECTOR
// =============================================================

@Composable
private fun RangeSelector(
    selectedRange: OrderRange,
    onRangeSelected: (OrderRange) -> Unit
) {

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color =
            MaterialTheme.colorScheme.surfaceVariant
                .copy(alpha = 0.55f)
    ) {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp)
        ) {

            OrderRange.entries.forEach { range ->

                val selected =
                    selectedRange == range


                val containerColor by animateColorAsState(
                    targetValue =
                        if (selected) {
                            MaterialTheme.colorScheme.surface
                        } else {
                            Color.Transparent
                        },
                    label = "rangeContainer"
                )


                val contentColor by animateColorAsState(
                    targetValue =
                        if (selected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme
                                .onSurfaceVariant
                        },
                    label = "rangeContent"
                )


                Surface(
                    onClick = {
                        onRangeSelected(range)
                    },
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp),
                    shape = RoundedCornerShape(24.dp),
                    color = containerColor,
                    shadowElevation =
                        if (selected) 1.dp else 0.dp
                ) {

                    Box(
                        contentAlignment = Alignment.Center
                    ) {

                        Text(
                            text = range.label,
                            fontSize = 12.sp,
                            fontWeight =
                                if (selected) {
                                    FontWeight.Bold
                                } else {
                                    FontWeight.Medium
                                },
                            color = contentColor
                        )
                    }
                }
            }
        }
    }
}


// =============================================================
// STATS
// =============================================================

@Composable
private fun OrderStatsStrip(
    deliveredCount: Int,
    deliveredQuantity: String,
    deliveredValue: String
) {

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 2.dp,
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant
                .copy(alpha = 0.45f)
        )
    ) {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = 8.dp,
                    vertical = 14.dp
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {

            StatItem(
                modifier = Modifier.weight(1f),
                value = deliveredCount.toString(),
                label = "Delivered"
            )


            VerticalDivider(
                modifier = Modifier.height(34.dp),
                color =
                    MaterialTheme.colorScheme.outlineVariant
            )


            StatItem(
                modifier = Modifier.weight(1f),
                value = deliveredQuantity,
                label = "Quantity"
            )


            VerticalDivider(
                modifier = Modifier.height(34.dp),
                color =
                    MaterialTheme.colorScheme.outlineVariant
            )


            StatItem(
                modifier = Modifier.weight(1.25f),
                value = deliveredValue,
                label = "Order value"
            )
        }
    }
}


@Composable
private fun StatItem(
    value: String,
    label: String,
    modifier: Modifier = Modifier
) {

    Column(
        modifier = modifier.padding(
            horizontal = 6.dp
        ),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )


        Spacer(Modifier.height(3.dp))


        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color =
                MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1
        )
    }
}


// =============================================================
// SEARCH
// =============================================================

@Composable
private fun OrderSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    onClear: () -> Unit
) {

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = {
            Text(
                text = "Search crop, supplier or pickup"
            )
        },
        leadingIcon = {

            Icon(
                imageVector = Lucide.Search,
                contentDescription = null,
                modifier = Modifier.size(19.dp)
            )
        },
        trailingIcon = {

            if (value.isNotBlank()) {

                IconButton(
                    onClick = onClear
                ) {

                    Icon(
                        imageVector = Lucide.X,
                        contentDescription = "Clear search",
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        },
        singleLine = true,
        shape = RoundedCornerShape(16.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor =
                MaterialTheme.colorScheme.surface,
            unfocusedContainerColor =
                MaterialTheme.colorScheme.surface,
            focusedBorderColor =
                MaterialTheme.colorScheme.primary,
            unfocusedBorderColor =
                MaterialTheme.colorScheme.outlineVariant
        ),
        modifier = Modifier.fillMaxWidth()
    )
}


// =============================================================
// STATUS FILTER
// =============================================================

@Composable
private fun StatusSelector(
    selectedStatus: OrderStatus?,
    onStatusSelected: (OrderStatus?) -> Unit
) {

    Column(
        verticalArrangement = Arrangement.spacedBy(7.dp)
    ) {

        Text(
            text = "Status",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )


        LazyRow(
            horizontalArrangement =
                Arrangement.spacedBy(8.dp)
        ) {

            item {

                StatusFilterChip(
                    label = "All",
                    selected = selectedStatus == null,
                    onClick = {
                        onStatusSelected(null)
                    }
                )
            }


            items(
                count = OrderStatus.entries.size
            ) { index ->

                val status =
                    OrderStatus.entries[index]

                StatusFilterChip(
                    label = status.displayLabel(),
                    selected =
                        selectedStatus == status,
                    onClick = {
                        onStatusSelected(status)
                    }
                )
            }
        }
    }
}


@Composable
private fun StatusFilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {

    val containerColor by animateColorAsState(
        targetValue =
            if (selected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            },
        label = "statusContainer"
    )


    val contentColor by animateColorAsState(
        targetValue =
            if (selected) {
                MaterialTheme.colorScheme.onPrimaryContainer
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
        label = "statusContent"
    )


    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(24.dp),
        color = containerColor,
        border = BorderStroke(
            1.dp,
            if (selected) {
                MaterialTheme.colorScheme.primary
                    .copy(alpha = 0.20f)
            } else {
                MaterialTheme.colorScheme.outlineVariant
            }
        )
    ) {

        Row(
            modifier = Modifier.padding(
                horizontal = 13.dp,
                vertical = 8.dp
            ),
            verticalAlignment = Alignment.CenterVertically
        ) {

            if (selected) {

                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(
                            MaterialTheme.colorScheme.primary
                        )
                )

                Spacer(Modifier.width(7.dp))
            }


            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight =
                    if (selected) {
                        FontWeight.SemiBold
                    } else {
                        FontWeight.Medium
                    },
                color = contentColor
            )
        }
    }
}


// =============================================================
// ORDER CARD
// =============================================================

@Composable
internal fun ModernOrderCard(
    order: Order,
    supplierName: String?,
    onCardClick: () -> Unit
) {
    Surface(onClick = onCardClick, modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surface,
        shadowElevation = 1.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text("Order #${order.orderId.takeLast(12)}", style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            OrderStatusChip(order.status)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Surface(shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.primaryContainer) {
                    Box(Modifier.size(52.dp), contentAlignment = Alignment.Center) {
                        Icon(Lucide.Sprout, null, Modifier.size(26.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(order.cropName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold,
                        maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text("${order.quantity} ${order.quantityUnit.name}", style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.surfaceContainerLow) {
                Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Total agreed", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Rs. ${order.quantity * order.agreedPrice}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("Rs. ${order.agreedPrice}/${order.quantityUnit.name}", style = MaterialTheme.typography.bodySmall)
                }
            }
            if (!supplierName.isNullOrBlank()) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    com.agroroute.core.components.UserProfileAvatar(supplierName, null, size = 36.dp)
                    Column(Modifier.weight(1f)) {
                        Text("Supplier", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(supplierName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold,
                            maxLines = 2, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
            if (order.pickupLocation.isNotBlank()) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Lucide.MapPin, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                    Text(com.agroroute.core.components.profileLocationLabel(order.pickupLocation),
                        style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f))
                }
            }
            OrderProgressStrip(order.status)
            Text(com.agroroute.core.components.orderNextStep(order.status, false),
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(formatDate(order.createdAt), Modifier.weight(1f), style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("View order", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary)
                Icon(Lucide.ChevronRight, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
            }
        }
    }
}


// =============================================================
// ORDER METRIC
// =============================================================

@Composable
private fun OrderMetric(
    icon: ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {

    Column(
        modifier = modifier
    ) {

        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {

            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(13.dp),
                tint =
                    MaterialTheme.colorScheme.onSurfaceVariant
            )


            Spacer(Modifier.width(5.dp))


            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color =
                    MaterialTheme.colorScheme.onSurfaceVariant
            )
        }


        Spacer(Modifier.height(3.dp))


        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}


// =============================================================
// EMPTY STATE
// =============================================================

@Composable
private fun OrdersEmptyState(
    title: String,
    message: String,
    searchState: Boolean
) {

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {

        Column(
            modifier = Modifier.padding(30.dp),
            horizontalAlignment =
                Alignment.CenterHorizontally
        ) {

            Surface(
                modifier = Modifier.size(68.dp),
                shape = CircleShape,
                color =
                    MaterialTheme.colorScheme
                        .surfaceVariant
            ) {

                Box(
                    contentAlignment = Alignment.Center
                ) {

                    Icon(
                        imageVector =
                            if (searchState) {
                                Lucide.Search
                            } else {
                                Lucide.Receipt
                            },
                        contentDescription = null,
                        modifier = Modifier.size(27.dp),
                        tint =
                            MaterialTheme.colorScheme.primary
                    )
                }
            }


            Spacer(Modifier.height(16.dp))


            Text(
                text = title,
                style =
                    MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color =
                    MaterialTheme.colorScheme.onBackground
            )


            Spacer(Modifier.height(5.dp))


            Text(
                text = message,
                style =
                    MaterialTheme.typography.bodySmall,
                color =
                    MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}


// =============================================================
// HELPERS
// =============================================================

private fun OrderStatus.displayLabel(): String =
    name
        .lowercase()
        .replace('_', ' ')
        .replaceFirstChar {
            it.uppercase()
        }


private fun formatDate(
    timestamp: Long
): String =
    SimpleDateFormat(
        "MMM dd, yyyy",
        Locale.getDefault()
    ).format(
        Date(timestamp)
    )