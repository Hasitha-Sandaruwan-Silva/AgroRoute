package com.agroroute.core.widgets

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.agroroute.core.model.OrderStatus
import com.agroroute.core.theme.AgroRouteTheme
import com.github.thelacspace.icons.Lucide

/** The fulfilment path every order travels (cancelled orders never start it). */
private val orderSteps = listOf(
    OrderStatus.AWAITING_PICKUP to "Awaiting pickup",
    OrderStatus.IN_TRANSIT to "In transit",
    OrderStatus.DELIVERED to "Delivered"
)

/** Index of the furthest step reached; -1 when the order was cancelled. */
private fun currentIndexFor(status: OrderStatus): Int = when (status) {
    OrderStatus.CANCELLED -> -1
    OrderStatus.AWAITING_PICKUP -> 0
    OrderStatus.IN_TRANSIT -> 1
    OrderStatus.DELIVERED -> 2
}

@Composable
fun OrderStatusTimeline(
    currentStatus: OrderStatus,
    modifier: Modifier = Modifier
) {
    val currentIndex = currentIndexFor(currentStatus)

    AppCard(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "Order progress",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.height(16.dp))

        if (currentStatus == OrderStatus.CANCELLED) {
            Surface(
                color = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.onErrorContainer,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Lucide.AlertCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = "Cancelled — this order stopped before pickup.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
        }

        orderSteps.forEachIndexed { index, (status, label) ->
            TimelineStep(
                label = label,
                completed = index <= currentIndex,
                active = index == currentIndex,
                isLast = index == orderSteps.lastIndex
            )
        }
    }
}

@Composable
private fun TimelineStep(
    label: String,
    completed: Boolean,
    active: Boolean,
    isLast: Boolean
) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(if (active) 20.dp else 14.dp)
                    .clip(CircleShape)
                    .background(
                        when {
                            active -> MaterialTheme.colorScheme.primary
                            completed -> MaterialTheme.colorScheme.secondary
                            else -> MaterialTheme.colorScheme.outline
                        }
                    )
                    .then(
                        if (active) {
                            Modifier.border(
                                width = 3.dp,
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f),
                                shape = CircleShape
                            )
                        } else Modifier
                    )
            )
            if (!isLast) {
                Box(
                    modifier = Modifier
                        .width(3.dp)
                        .height(28.dp)
                        .clip(CircleShape)
                        .background(
                            if (completed) MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
                            else MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
                        )
                )
            }
        }
        Spacer(Modifier.width(16.dp))
        Column(
            modifier = Modifier.padding(
                top = if (active) 0.dp else 1.dp,
                bottom = if (isLast) 0.dp else 12.dp
            )
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                color = when {
                    active -> MaterialTheme.colorScheme.primary
                    completed -> MaterialTheme.colorScheme.onSurface
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
            if (active) {
                Text(
                    text = "Current step",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * Compact three-dot progress strip used inside order list cards, so the
 * fulfilment stage is scannable without opening the order.
 */
@Composable
fun OrderProgressStrip(
    currentStatus: OrderStatus,
    modifier: Modifier = Modifier
) {
    val currentIndex = currentIndexFor(currentStatus)
    val cancelled = currentStatus == OrderStatus.CANCELLED

    Column(modifier = modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            orderSteps.forEachIndexed { index, _ ->
                val reached = index <= currentIndex
                Box(
                    modifier = Modifier
                        .size(if (index == currentIndex) 10.dp else 8.dp)
                        .clip(CircleShape)
                        .background(
                            when {
                                cancelled -> MaterialTheme.colorScheme.outline
                                reached -> MaterialTheme.colorScheme.primary
                                else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)
                            }
                        )
                )
                if (index != orderSteps.lastIndex) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(3.dp)
                            .clip(CircleShape)
                            .background(
                                if (!cancelled && index < currentIndex) {
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)
                                } else {
                                    MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
                                }
                            )
                    )
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = if (cancelled) "Cancelled" else orderSteps[currentIndex].second,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = if (cancelled) MaterialTheme.colorScheme.error
            else MaterialTheme.colorScheme.primary
        )
    }
}

@Preview(name = "Light", showBackground = true, backgroundColor = 0xFFFAF8F1)
@Preview(name = "Dark", showBackground = true, backgroundColor = 0xFF0C1410, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun OrderStatusTimelinePreview() {
    AgroRouteTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            OrderStatusTimeline(currentStatus = OrderStatus.IN_TRANSIT)
            Spacer(Modifier.height(12.dp))
            OrderStatusTimeline(currentStatus = OrderStatus.CANCELLED)
        }
    }
}

@Preview(name = "Strips", showBackground = true, backgroundColor = 0xFFFFFFFF)
@Composable
private fun OrderProgressStripPreview() {
    AgroRouteTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OrderProgressStrip(currentStatus = OrderStatus.AWAITING_PICKUP)
            OrderProgressStrip(currentStatus = OrderStatus.IN_TRANSIT)
            OrderProgressStrip(currentStatus = OrderStatus.DELIVERED)
            OrderProgressStrip(currentStatus = OrderStatus.CANCELLED)
        }
    }
}
