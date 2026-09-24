package com.agroroute.core.widgets

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.agroroute.core.model.BidStatus
import com.agroroute.core.model.ListingStatus
import com.agroroute.core.model.OrderStatus
import com.agroroute.core.theme.AccentAmber
import com.agroroute.core.theme.GreenDeep
import com.agroroute.core.theme.MintContainer
import com.agroroute.core.theme.TextDarkGreen

enum class ChipTone {
    Primary, Secondary, Tertiary, Success, Warning, Error, Neutral
}

@Composable
fun StatusChip(
    label: String,
    tone: ChipTone = ChipTone.Primary,
    modifier: Modifier = Modifier
) {
    val (container, content) = chipColors(tone)
    Surface(
        modifier = modifier,
        color = container,
        shape = MaterialTheme.shapes.small
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = content,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
        )
    }
}

@Composable
fun ListingStatusChip(status: ListingStatus, modifier: Modifier = Modifier) {
    val (label, tone) = when (status) {
        ListingStatus.OPEN -> "Open" to ChipTone.Success
        ListingStatus.CLOSED -> "Closed" to ChipTone.Neutral
        ListingStatus.FULFILLED -> "Fulfilled" to ChipTone.Primary
    }
    StatusChip(label = label, tone = tone, modifier = modifier)
}

@Composable
fun BidStatusChip(status: BidStatus, modifier: Modifier = Modifier) {
    val (label, tone) = when (status) {
        BidStatus.PENDING -> "Pending" to ChipTone.Warning
        BidStatus.ACCEPTED -> "Accepted" to ChipTone.Success
        BidStatus.REJECTED -> "Rejected" to ChipTone.Error
        BidStatus.WITHDRAWN -> "Withdrawn" to ChipTone.Neutral
    }
    StatusChip(label = label, tone = tone, modifier = modifier)
}

@Composable
fun OrderStatusChip(status: OrderStatus, modifier: Modifier = Modifier) {
    val (label, tone) = when (status) {
        OrderStatus.AWAITING_PICKUP -> "Awaiting Pickup" to ChipTone.Primary
        OrderStatus.IN_TRANSIT -> "In Transit" to ChipTone.Warning
        OrderStatus.DELIVERED -> "Delivered" to ChipTone.Success
        OrderStatus.CANCELLED -> "Cancelled" to ChipTone.Error
    }
    StatusChip(label = label, tone = tone, modifier = modifier)
}

@Composable
private fun chipColors(tone: ChipTone): Pair<Color, Color> = when (tone) {
    ChipTone.Primary -> MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
    ChipTone.Secondary -> MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.onSecondaryContainer
    ChipTone.Tertiary -> MaterialTheme.colorScheme.tertiaryContainer to MaterialTheme.colorScheme.onTertiaryContainer
    ChipTone.Success -> MintContainer to GreenDeep
    ChipTone.Warning -> AccentAmber to TextDarkGreen
    ChipTone.Error -> MaterialTheme.colorScheme.errorContainer to MaterialTheme.colorScheme.onErrorContainer
    ChipTone.Neutral -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
}
