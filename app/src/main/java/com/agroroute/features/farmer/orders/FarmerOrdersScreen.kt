package com.agroroute.features.farmer.orders

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import com.github.thelacspace.icons.Lucide
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
import com.agroroute.core.model.Order
import com.agroroute.core.model.OrderStatus
import com.agroroute.core.navigation.Screen
import com.agroroute.core.components.AgroSnackbarHost
import com.agroroute.core.components.ErrorSnackbarEffect
import com.agroroute.core.components.ListSkeleton
import com.agroroute.core.components.OrderProgressStrip
import com.agroroute.core.components.OrderStatusChip
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
    var ordersLoaded by remember { mutableStateOf(false) }
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

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        if (ordersLoaded) viewModel.refreshOrdersForFarmer(viewModel.currentFarmerId)
        else { ordersLoaded = true; viewModel.loadOrdersForFarmer(viewModel.currentFarmerId) }
    }
    ErrorSnackbarEffect(uiState.error, snackbarHostState) { viewModel.clearError() }

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Box(
            Modifier.fillMaxWidth()
                .background(Brush.linearGradient(listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.primary.copy(alpha = 0.88f), MaterialTheme.colorScheme.secondary.copy(alpha = 0.92f))))
                .padding(horizontal = 22.dp, vertical = 26.dp)
        ) {
            Box(Modifier.size(140.dp).offset(x = 200.dp, y = (-40).dp).clip(CircleShape).background(Color.White.copy(alpha = 0.07f)).align(Alignment.TopEnd))
            Box(Modifier.size(90.dp).offset(x = (-20).dp, y = 70.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.05f)).align(Alignment.BottomStart))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = CircleShape, color = Color.White.copy(alpha = 0.20f), modifier = Modifier.size(60.dp).shadow(2.dp, CircleShape)) {
                    Box(contentAlignment = Alignment.Center) { Icon(imageVector = Lucide.Receipt, contentDescription = null, tint = Color.White, modifier = Modifier.size(28.dp)) }
                }
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    Text("ORDER HISTORY", style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = 0.85f), fontWeight = FontWeight.ExtraBold, letterSpacing = 1.4.sp, fontSize = 11.sp)
                    Spacer(Modifier.height(4.dp))
                    Text("Order history", style = MaterialTheme.typography.headlineSmall, color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 24.sp)
                    Text("Orders are not payments. No payment is processed in this app.", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.85f), modifier = Modifier.padding(top = 4.dp), lineHeight = 15.sp, fontSize = 12.sp)
                }
            }
        }

        Surface(modifier = Modifier.fillMaxWidth().weight(1f).offset(y = (-14).dp), shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp), color = MaterialTheme.colorScheme.background, shadowElevation = 8.dp) {
            Column(Modifier.fillMaxSize()) {
                Column(Modifier.padding(horizontal = 20.dp, vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(28.dp).clip(RoundedCornerShape(8.dp)).background(MaterialTheme.colorScheme.primaryContainer), contentAlignment = Alignment.Center) {
                            Icon(imageVector = Lucide.Calendar, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                        Spacer(Modifier.width(8.dp))
                        Text("Time range", fontWeight = FontWeight.ExtraBold, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
                    }
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(OrderRange.entries.size) { index ->
                            val range = OrderRange.entries[index]
                            val selected = selectedRange == range
                            val bg by animateColorAsState(if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface, label = "")
                            val contentColor by animateColorAsState(if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant, label = "")
                            Surface(onClick = { selectedRange = range }, shape = RoundedCornerShape(50.dp), color = bg, border = BorderStroke(1.2.dp, if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)), shadowElevation = if (selected) 6.dp else 1.dp, modifier = Modifier.heightIn(min = 40.dp)) {
                                Row(Modifier.padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                                    if (selected) { Box(Modifier.size(6.dp).clip(CircleShape).background(Color.White)); Spacer(Modifier.width(6.dp)) }
                                    Text(range.label, fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.Bold, fontSize = 12.sp, color = contentColor)
                                }
                            }
                        }
                    }

                    Surface(modifier = Modifier.fillMaxWidth().shadow(2.dp, RoundedCornerShape(20.dp)), shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surface, border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))) {
                        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(Modifier.size(32.dp).clip(RoundedCornerShape(10.dp)).background(Brush.linearGradient(listOf(MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.secondaryContainer))), contentAlignment = Alignment.Center) {
                                        Icon(imageVector = Lucide.Trophy, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
                                    }
                                    Spacer(Modifier.width(10.dp))
                                    Column {
                                        Text("Delivered order value", fontWeight = FontWeight.ExtraBold, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
                                        Text(selectedRange.label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Medium)
                                    }
                                }
                                Surface(shape = RoundedCornerShape(50.dp), color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)) {
                                    Row(Modifier.padding(horizontal = 10.dp, vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Box(Modifier.size(6.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary))
                                        Spacer(Modifier.width(6.dp))
                                        Text("${deliveredOrders.size} delivered", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                    }
                                }
                            }
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                PremiumSummaryCard(label = "All orders", value = rangedOrders.size.toString(), icon = Lucide.Package, modifier = Modifier.weight(1f))
                                PremiumSummaryCard(label = "Delivered", value = deliveredOrders.size.toString(), icon = Lucide.CheckCircle, modifier = Modifier.weight(1f))
                            }
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                PremiumSummaryCard(label = "Delivered qty", value = deliveredQuantity.toString(), icon = Lucide.Box, modifier = Modifier.weight(1f))
                                Surface(modifier = Modifier.weight(1f).shadow(4.dp, RoundedCornerShape(14.dp)), shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f), border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))) {
                                    Column(Modifier.padding(12.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(imageVector = Lucide.BadgeDollarSign, contentDescription = null, modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
                                            Spacer(Modifier.width(4.dp))
                                            Text("Order value", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimaryContainer, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                        }
                                        Spacer(Modifier.height(4.dp))
                                        Text("Rs. $deliveredValue", fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, color = MaterialTheme.colorScheme.onPrimaryContainer, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    }
                                }
                            }
                            HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                            Row(verticalAlignment = Alignment.Top) {
                                Icon(imageVector = Lucide.Info, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(Modifier.width(6.dp))
                                Text("Based on delivered orders; this does not indicate payment status.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp, lineHeight = 14.sp)
                            }
                        }
                    }

                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { newValue -> searchQuery = newValue },
                        label = { Text("Search crop, supplier, or pickup", fontSize = 13.sp) },
                        leadingIcon = {
                            Box(Modifier.size(32.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer), contentAlignment = Alignment.Center) {
                                Icon(imageVector = Lucide.Search, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
                            }
                        },
                        trailingIcon = {
                            if (searchQuery.isNotBlank()) {
                                Surface(onClick = { searchQuery = "" }, shape = CircleShape, color = MaterialTheme.colorScheme.surfaceVariant) {
                                    Icon(imageVector = Lucide.X, contentDescription = null, modifier = Modifier.padding(6.dp).size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(28.dp).clip(RoundedCornerShape(8.dp)).background(MaterialTheme.colorScheme.secondaryContainer), contentAlignment = Alignment.Center) {
                            Icon(imageVector = Lucide.Tag, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSecondaryContainer)
                        }
                        Spacer(Modifier.width(8.dp))
                        Text("Filter by status", fontWeight = FontWeight.ExtraBold, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
                    }
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        item {
                            val selected = selectedStatus == null
                            val bg by animateColorAsState(if (selected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.surface, label = "")
                            val contentColor by animateColorAsState(if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant, label = "")
                            Surface(onClick = { selectedStatus = null }, shape = RoundedCornerShape(50.dp), color = bg, border = BorderStroke(1.2.dp, if (selected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)), shadowElevation = if (selected) 4.dp else 1.dp, modifier = Modifier.heightIn(min = 40.dp)) {
                                Text("All statuses", Modifier.padding(horizontal = 16.dp, vertical = 10.dp), fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.Bold, fontSize = 12.sp, color = contentColor)
                            }
                        }
                        items(OrderStatus.entries.size) { index ->
                            val status = OrderStatus.entries[index]
                            val selected = selectedStatus == status
                            val bg by animateColorAsState(if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface, label = "")
                            val contentColor by animateColorAsState(if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant, label = "")
                            Surface(onClick = { selectedStatus = status }, shape = RoundedCornerShape(50.dp), color = bg, border = BorderStroke(1.2.dp, if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)), shadowElevation = if (selected) 4.dp else 1.dp, modifier = Modifier.heightIn(min = 40.dp)) {
                                Text(status.displayLabel(), Modifier.padding(horizontal = 16.dp, vertical = 10.dp), fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.Bold, fontSize = 12.sp, color = contentColor)
                            }
                        }
                    }
                    Surface(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                        Text(text = stringResource(R.string.orders_payment_caption), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center, fontSize = 11.sp, lineHeight = 14.sp, modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp))
                    }
                }

                Box(modifier = Modifier.weight(1f)) {
                    when {
                        uiState.isLoading && uiState.orders.isEmpty() -> ListSkeleton()
                        uiState.orders.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Surface(modifier = Modifier.padding(20.dp).shadow(2.dp, RoundedCornerShape(18.dp)), shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.surface, border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))) {
                                Column(Modifier.padding(vertical = 36.dp, horizontal = 24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Box(Modifier.size(80.dp).clip(CircleShape).background(Brush.linearGradient(listOf(MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.secondaryContainer))), contentAlignment = Alignment.Center) {
                                        Icon(imageVector = Lucide.Receipt, contentDescription = null, modifier = Modifier.size(32.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
                                    }
                                    Text("No Orders Yet", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurface)
                                    Text("Your order history will appear here", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                                }
                            }
                        }
                        visibleOrders.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Box(Modifier.size(72.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) {
                                    Icon(imageVector = Lucide.Search, contentDescription = null, modifier = Modifier.size(28.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Text("No matching orders", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
                                Text("Try changing the search, date range, or status", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                            }
                        }
                        else -> LazyColumn(contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.fillMaxSize()) {
                            items(visibleOrders, key = { it.orderId }) { order ->
                                OrderCard(order = order, supplierName = uiState.supplierNames[order.orderId], onCardClick = { navController.navigate(Screen.FarmerOrderDetail.createRoute(order.orderId)) })
                            }
                            item { Spacer(Modifier.height(20.dp)) }
                        }
                    }
                    AgroSnackbarHost(snackbarHostState = snackbarHostState, modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 12.dp))
                }
            }
        }
    }
}

@Composable
private fun PremiumSummaryCard(label: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier = Modifier) {
    Surface(modifier = modifier.shadow(2.dp, RoundedCornerShape(14.dp)), shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f), border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.width(4.dp))
                Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Spacer(Modifier.height(4.dp))
            Text(value, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun OrderCard(order: Order, supplierName: String?, onCardClick: () -> Unit) {
    Surface(onClick = onCardClick, modifier = Modifier.fillMaxWidth().shadow(2.dp, RoundedCornerShape(22.dp)), shape = RoundedCornerShape(22.dp), color = MaterialTheme.colorScheme.surface, border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Box(Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(Brush.linearGradient(listOf(MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.secondaryContainer))), contentAlignment = Alignment.Center) {
                        Icon(imageVector = Lucide.Sprout, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                    Spacer(Modifier.width(10.dp))
                    Text(text = order.cropName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                }
                Spacer(Modifier.width(10.dp))
                OrderStatusChip(order.status)
            }
            if (supplierName != null) {
                Surface(shape = RoundedCornerShape(50.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f), border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))) {
                    Row(Modifier.padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(22.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)), contentAlignment = Alignment.Center) {
                            Text(supplierName.take(1).uppercase(), fontWeight = FontWeight.ExtraBold, fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                        }
                        Spacer(Modifier.width(8.dp))
                        Text("Supplier: $supplierName", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.Medium, fontSize = 12.sp)
                    }
                }
            }
            Surface(shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f), border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))) {
                Row(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text("${order.quantity}", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurface)
                        Spacer(Modifier.width(4.dp))
                        Text(order.quantityUnit.name, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 2.dp))
                    }
                    Surface(shape = RoundedCornerShape(50.dp), color = MaterialTheme.colorScheme.primary, shadowElevation = 3.dp) {
                        Text("Rs. ${order.agreedPrice}/${order.quantityUnit.name}", Modifier.padding(horizontal = 12.dp, vertical = 6.dp), fontWeight = FontWeight.ExtraBold, fontSize = 12.sp, color = Color.White)
                    }
                }
            }
            if (order.pickupLocation.isNotBlank()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(24.dp).clip(CircleShape).background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f)), contentAlignment = Alignment.Center) {
                        Icon(imageVector = Lucide.MapPin, contentDescription = null, modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.secondary)
                    }
                    Spacer(Modifier.width(8.dp))
                    Text("Pickup: ${order.pickupLocation}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                }
            }
            HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
            OrderProgressStrip(currentStatus = order.status)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Lucide.Calendar, contentDescription = null, modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.width(6.dp))
                    Text("Created ${formatDate(order.createdAt)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                }
                Icon(imageVector = Lucide.ChevronRight, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
            }
        }
    }
}

private fun OrderStatus.displayLabel(): String = name.lowercase().replace('_', ' ').replaceFirstChar { it.uppercase() }
private fun formatDate(timestamp: Long): String = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(timestamp))