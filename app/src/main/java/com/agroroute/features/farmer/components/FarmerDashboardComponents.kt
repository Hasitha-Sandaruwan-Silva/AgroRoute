package com.agroroute.features.farmer.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.agroroute.core.components.ListingImage
import com.agroroute.core.components.AppCard
import com.agroroute.core.components.OrderStatusChip
import com.agroroute.core.model.Order
import com.github.thelacspace.icons.Lucide

@Composable
fun FarmerSectionHeader(title: String, subtitle: String? = null, action: String? = null, onAction: () -> Unit = {}) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (action != null) TextButton(onClick = onAction) {
            Text(action, style = MaterialTheme.typography.labelMedium)
            Icon(Lucide.ArrowRight, null, Modifier.padding(start = 4.dp).size(15.dp))
        }
    }
}

/** Uses only the supplied listing photo; no decorative substitute is treated as produce. */
@Composable
fun FarmerProduceThumbnail(imageUrl: String?, modifier: Modifier = Modifier, size: androidx.compose.ui.unit.Dp = 60.dp) {
    Box(modifier.size(size)) { ListingImage(imageUrl, height = size) }
}

/** A one-shot reveal; no loops and no changes to child loading or navigation. */
@Composable
fun FarmerHeroReveal(content: @Composable () -> Unit) {
    val reveal = remember { Animatable(0f) }
    LaunchedEffect(Unit) { reveal.animateTo(1f, tween(280)) }
    Box(Modifier.graphicsLayer { alpha = reveal.value; translationY = (1f - reveal.value) * 6.dp.toPx() }) { content() }
}

@Composable
fun FarmerRecentOrderCard(order: Order, onOpen: () -> Unit) {
    AppCard(Modifier.fillMaxWidth(), onClick = onOpen) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.primaryContainer) {
                Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                    Icon(Lucide.Sprout, null, Modifier.size(24.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
                }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(order.cropName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text("${order.quantity} ${order.quantityUnit.name} · Rs. ${order.quantity * order.agreedPrice}",
                    style = MaterialTheme.typography.bodySmall)
                OrderStatusChip(order.status)
                Text(java.text.DateFormat.getDateTimeInstance(java.text.DateFormat.SHORT, java.text.DateFormat.SHORT)
                    .format(java.util.Date(order.createdAt)), style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Lucide.ChevronRight, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
