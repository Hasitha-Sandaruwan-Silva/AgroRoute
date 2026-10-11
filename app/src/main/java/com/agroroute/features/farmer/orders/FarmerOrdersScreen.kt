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
import androidx.hilt.navigation.compose.hiltViewModel
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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                MaterialTheme.colorScheme.background
            )
    ) {

        Column(
            modifier = Modifier.fillMaxSize()
        ) {

            // =================================================
            // SIMPLE UNIQUE HEADER
            // =================================================

            OrdersHeader(
                totalOrders = rangedOrders.size,
                selectedRange = selectedRange
            )


            // =================================================
            // FILTER AREA
            // =================================================

            Column(
                modifier = Modifier.padding(
                    start = 18.dp,
                    end = 18.dp,
                    top = 8.dp,
                    bottom = 10.dp
                ),
                verticalArrangement =
                    Arrangement.spacedBy(12.dp)
            ) {

                // ---------------------------------------------
                // Range selector
                // ---------------------------------------------

                RangeSelector(
                    selectedRange = selectedRange,
                    onRangeSelected = {
                        selectedRange = it
                    }
                )


                // ---------------------------------------------
                // Stats
                // ---------------------------------------------

                OrderStatsStrip(
                    deliveredCount =
                        deliveredOrders.size,
                    deliveredQuantity =
                        deliveredQuantity.toString(),
                    deliveredValue =
                        "Rs. $deliveredValue"
                )


                // ---------------------------------------------
                // Search
                // ---------------------------------------------

                OrderSearchField(
                    value = searchQuery,
                    onValueChange = {
                        searchQuery = it
                    },
                    onClear = {
                        searchQuery = ""
                    }
                )


                // ---------------------------------------------
                // Status
                // ---------------------------------------------

                StatusSelector(
                    selectedStatus = selectedStatus,
                    onStatusSelected = {
                        selectedStatus = it
                    }
                )
            }


            // =================================================
            // RESULT HEADER
            // =================================================

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = 20.dp,
                        vertical = 6.dp
                    ),
                verticalAlignment = Alignment.CenterVertically
            ) {

                Text(
                    text = "Orders",
                    style =
                        MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color =
                        MaterialTheme.colorScheme.onBackground
                )

                Spacer(Modifier.width(7.dp))

                Surface(
                    shape = CircleShape,
                    color =
                        MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        text = visibleOrders.size.toString(),
                        modifier = Modifier.padding(
                            horizontal = 8.dp,
                            vertical = 3.dp
                        ),
                        style =
                            MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color =
                            MaterialTheme.colorScheme
                                .onPrimaryContainer
                    )
                }


                Spacer(Modifier.weight(1f))


                Text(
                    text = selectedRange.label,
                    style =
                        MaterialTheme.typography.labelSmall,
                    color =
                        MaterialTheme.colorScheme
                            .onSurfaceVariant
                )
            }


            // =================================================
            // ORDER LIST
            // =================================================

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {

                when {

                    uiState.isLoading &&
                            uiState.orders.isEmpty() -> {

                        ListSkeleton()
                    }


                    uiState.orders.isEmpty() -> {

                        OrdersEmptyState(
                            title = "No orders yet",
                            message =
                                "Your orders will appear here once a supplier order is created.",
                            searchState = false
                        )
                    }


                    visibleOrders.isEmpty() -> {

                        OrdersEmptyState(
                            title = "Nothing found",
                            message =
                                "Try a different search, status, or time range.",
                            searchState = true
                        )
                    }


                    else -> {

                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(
                                start = 18.dp,
                                end = 18.dp,
                                top = 8.dp,
                                bottom = 28.dp
                            ),
                            verticalArrangement =
                                Arrangement.spacedBy(11.dp)
                        ) {

                            items(
                                items = visibleOrders,
                                key = {
                                    it.orderId
                                }
                            ) { order ->

                                ModernOrderCard(
                                    order = order,
                                    supplierName =
                                        uiState.supplierNames[
                                            order.orderId
                                        ],
                                    onCardClick = {

                                        navController.navigate(
                                            Screen
                                                .FarmerOrderDetail
                                                .createRoute(
                                                    order.orderId
                                                )
                                        )
                                    }
                                )
                            }
                        }
                    }
                }


                AgroSnackbarHost(
                    snackbarHostState =
                        snackbarHostState,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 12.dp)
                )
            }
        }
    }
}


// =============================================================
// HEADER
// =============================================================

@Composable
private fun OrdersHeader(
    totalOrders: Int,
    selectedRange: OrderRange
) {

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = 20.dp,
                end = 20.dp,
                top = 22.dp,
                bottom = 14.dp
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {

        Column(
            modifier = Modifier.weight(1f)
        ) {

            Text(
                text = "My orders",
                style =
                    MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color =
                    MaterialTheme.colorScheme.onBackground
            )


            Spacer(Modifier.height(3.dp))


            Text(
                text = when {
                    totalOrders == 0 ->
                        "No orders in ${selectedRange.label.lowercase()}"

                    totalOrders == 1 ->
                        "1 order • ${selectedRange.label}"

                    else ->
                        "$totalOrders orders • ${selectedRange.label}"
                },
                style =
                    MaterialTheme.typography.bodySmall,
                color =
                    MaterialTheme.colorScheme.onSurfaceVariant
            )
        }


        Surface(
            modifier = Modifier.size(48.dp),
            shape = RoundedCornerShape(16.dp),
            color =
                MaterialTheme.colorScheme.primaryContainer
        ) {

            Box(
                contentAlignment = Alignment.Center
            ) {

                Icon(
                    imageVector = Lucide.Receipt,
                    contentDescription = null,
                    modifier = Modifier.size(22.dp),
                    tint =
                        MaterialTheme.colorScheme
                            .onPrimaryContainer
                )
            }
        }
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
                        .height(40.dp),
                    shape = RoundedCornerShape(12.dp),
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
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
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
        shape = RoundedCornerShape(12.dp),
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
private fun ModernOrderCard(
    order: Order,
    supplierName: String?,
    onCardClick: () -> Unit
) {

    Surface(
        onClick = onCardClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant
                .copy(alpha = 0.50f)
        )
    ) {

        Row(
            modifier = Modifier.fillMaxWidth()
        ) {

            // Unique little accent instead of
            // making the whole card colourful.
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .heightIn(min = 178.dp)
                    .background(
                        MaterialTheme.colorScheme.primary
                    )
            )


            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(16.dp),
                verticalArrangement =
                    Arrangement.spacedBy(11.dp)
            ) {

                // ---------------------------------------------
                // TITLE
                // ---------------------------------------------

                Row(
                    verticalAlignment =
                        Alignment.CenterVertically
                ) {

                    Surface(
                        modifier = Modifier.size(38.dp),
                        shape = RoundedCornerShape(12.dp),
                        color =
                            MaterialTheme.colorScheme
                                .primaryContainer
                    ) {

                        Box(
                            contentAlignment =
                                Alignment.Center
                        ) {

                            Icon(
                                imageVector =
                                    Lucide.Sprout,
                                contentDescription = null,
                                modifier =
                                    Modifier.size(18.dp),
                                tint =
                                    MaterialTheme.colorScheme
                                        .onPrimaryContainer
                            )
                        }
                    }


                    Spacer(Modifier.width(11.dp))


                    Column(
                        modifier = Modifier.weight(1f)
                    ) {

                        Text(
                            text = order.cropName,
                            style =
                                MaterialTheme.typography
                                    .titleMedium,
                            fontWeight = FontWeight.Bold,
                            color =
                                MaterialTheme.colorScheme
                                    .onSurface,
                            maxLines = 1,
                            overflow =
                                TextOverflow.Ellipsis
                        )


                        if (!supplierName.isNullOrBlank()) {

                            Spacer(Modifier.height(2.dp))

                            Text(
                                text = "Supplier: $supplierName",
                                style =
                                    MaterialTheme.typography
                                        .bodySmall,
                                color =
                                    MaterialTheme.colorScheme
                                        .onSurfaceVariant,
                                maxLines = 1,
                                overflow =
                                    TextOverflow.Ellipsis
                            )
                        }
                    }


                    Spacer(Modifier.width(8.dp))


                    OrderStatusChip(
                        order.status
                    )
                }


                // ---------------------------------------------
                // QUANTITY + PRICE
                // ---------------------------------------------

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment =
                        Alignment.CenterVertically
                ) {

                    OrderMetric(
                        icon = Lucide.Package,
                        label = "Quantity",
                        value =
                            "${order.quantity} ${order.quantityUnit.name}",
                        modifier = Modifier.weight(1f)
                    )


                    Spacer(Modifier.width(10.dp))


                    OrderMetric(
                        icon = Lucide.BadgeDollarSign,
                        label = "Price",
                        value =
                            "Rs. ${order.agreedPrice}/${order.quantityUnit.name}",
                        modifier = Modifier.weight(1.2f)
                    )
                }


                // ---------------------------------------------
                // PICKUP
                // ---------------------------------------------

                if (order.pickupLocation.isNotBlank()) {

                    Row(
                        verticalAlignment =
                            Alignment.CenterVertically
                    ) {

                        Icon(
                            imageVector = Lucide.MapPin,
                            contentDescription = null,
                            modifier = Modifier.size(15.dp),
                            tint =
                                MaterialTheme.colorScheme
                                    .onSurfaceVariant
                        )


                        Spacer(Modifier.width(7.dp))


                        Text(
                            text = com.agroroute.core.components.profileLocationLabel(order.pickupLocation),
                            style =
                                MaterialTheme.typography.bodySmall,
                            color =
                                MaterialTheme.colorScheme
                                    .onSurfaceVariant,
                            maxLines = 1,
                            overflow =
                                TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }


                HorizontalDivider(
                    color =
                        MaterialTheme.colorScheme
                            .outlineVariant.copy(alpha = 0.55f)
                )


                Text("Total agreed: Rs. ${order.quantity * order.agreedPrice}", style = MaterialTheme.typography.bodySmall)
                Text(com.agroroute.core.components.orderNextStep(order.status, false), style = MaterialTheme.typography.bodySmall)
                OrderProgressStrip(
                    currentStatus = order.status
                )


                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment =
                        Alignment.CenterVertically
                ) {

                    Icon(
                        imageVector = Lucide.Calendar,
                        contentDescription = null,
                        modifier = Modifier.size(13.dp),
                        tint =
                            MaterialTheme.colorScheme
                                .onSurfaceVariant
                    )


                    Spacer(Modifier.width(6.dp))


                    Text(
                        text = formatDate(order.createdAt),
                        style =
                            MaterialTheme.typography.labelSmall,
                        color =
                            MaterialTheme.colorScheme
                                .onSurfaceVariant
                    )


                    Spacer(Modifier.weight(1f))


                    Text(
                        text = "View details",
                        style =
                            MaterialTheme.typography.labelMedium,
                        color =
                            MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )


                    Spacer(Modifier.width(3.dp))


                    Icon(
                        imageVector = Lucide.ChevronRight,
                        contentDescription = null,
                        modifier = Modifier.size(15.dp),
                        tint =
                            MaterialTheme.colorScheme.primary
                    )
                }
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