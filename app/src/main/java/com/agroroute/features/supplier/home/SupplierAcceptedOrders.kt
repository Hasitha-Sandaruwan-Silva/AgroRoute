package com.agroroute.features.supplier.home

import androidx.compose.animation.*
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.agroroute.core.model.*
import com.github.thelacspace.icons.Lucide
import java.util.Locale

/** Presentation projection of the existing supplier order flow; never infers acceptance from a bid. */
internal fun homeAcceptedOrders(orders: List<Order>, supplierId: String): List<Order> =
    if (supplierId.isBlank()) emptyList() else orders
        .filter { it.supplierId == supplierId && it.orderId.isNotBlank() &&
            it.status in listOf(OrderStatus.AWAITING_PICKUP, OrderStatus.IN_TRANSIT) }
        .sortedWith(compareByDescending<Order> { it.updatedAt }.thenByDescending { it.createdAt }.thenBy { it.orderId })
        .distinctBy { it.orderId }

internal fun homeOrderFarmer(order: Order, listings: List<HarvestListing>, requests: List<OrderRequest>): String =
    requests.firstOrNull { it.requestId == order.orderId && it.supplierId == order.supplierId &&
        it.farmerId == order.farmerId && it.status == OrderRequestStatus.ACCEPTED }
        ?.farmerName?.takeIf(String::isNotBlank)
        ?: listings.firstOrNull { it.listingId == order.listingId && it.farmerId == order.farmerId }
            ?.farmerName?.takeIf(String::isNotBlank)
        ?: "Name unavailable"

@Composable
internal fun SupplierAcceptedOrders(
    orders: List<Order>, listings: List<HarvestListing>, requests: List<OrderRequest>, now: Long,
    onViewAll: () -> Unit, onOrder: (Order) -> Unit
) {
    val visible = remember { MutableTransitionState(false).apply { targetState = true } }
    AnimatedVisibility(visibleState = visible,
        enter = fadeIn(tween(180)) + slideInVertically(tween(180)) { it / 12 }) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp).testTag("accepted-orders"),
            verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Accepted Orders", Modifier.weight(1f, fill = false),
                            style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface)
                        Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.primaryContainer) {
                            Text(orders.size.toString(), Modifier.animateContentSize().padding(horizontal = 8.dp, vertical = 3.dp)
                                .testTag("accepted-order-count"), style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                    }
                    Text("Confirmed and ready for the next step.", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                TextButton(onViewAll, Modifier.testTag("accepted-orders-view-all")) { Text("View All") }
            }
            orders.take(2).forEach { order ->
                key(order.orderId) {
                    AcceptedOrderCard(order, homeOrderFarmer(order, listings, requests), now) { onOrder(order) }
                }
            }
        }
    }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun AcceptedOrderCard(order: Order, farmer: String, now: Long, onClick: () -> Unit) {
    val pickup = order.status == OrderStatus.AWAITING_PICKUP
    val unit = order.quantityUnit.name.lowercase(Locale.ROOT)
    Surface(onClick = onClick, modifier = Modifier.fillMaxWidth().animateContentSize().testTag("accepted-order-${order.orderId}"),
        shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)), tonalElevation = 1.dp) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.primaryContainer) {
                    Icon(if (pickup) Lucide.Package else Lucide.Truck, null, Modifier.padding(10.dp).size(20.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer)
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(order.cropName.ifBlank { "Produce order" }, style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text("Farmer: $farmer", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            // FlowRow keeps long labels and large fonts readable on compact screens.
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("${homeNumber(order.quantity)} $unit", style = MaterialTheme.typography.labelLarge)
                if (order.agreedPrice.isFinite() && order.agreedPrice > 0) {
                    Text("Rs. ${homeNumber(order.agreedPrice)} / $unit", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            if (pickup) {
                val location = com.agroroute.core.components.profileLocationLabel(order.pickupLocation)
                if (order.pickupLocation.isNotBlank() && location != "Location unavailable") {
                    Row(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Lucide.MapPin, null, Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(location, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.secondaryContainer) {
                    Text(if (pickup) "Awaiting pickup" else "In transit", Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSecondaryContainer)
                }
                if (order.updatedAt > 0) Text("Updated ${homeRelativeTime(order.updatedAt, now)}",
                    Modifier.padding(vertical = 4.dp), style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(if (pickup) "View Pickup Details" else "View Delivery", Modifier.weight(1f),
                    style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                Icon(Lucide.ChevronRight, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
            }
        }
    }
}
