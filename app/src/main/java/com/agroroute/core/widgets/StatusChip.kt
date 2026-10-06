package com.agroroute.core.widgets

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.agroroute.core.model.BidStatus
import com.agroroute.core.model.HarvestStatus
import com.agroroute.core.model.ListingStatus
import com.agroroute.core.model.OrderRequestStatus
import com.agroroute.core.model.OrderStatus
import com.agroroute.core.theme.AgroRouteTheme
import com.agroroute.core.theme.LocalAgroExtendedColors
import com.agroroute.core.theme.PillShape

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
        shape = PillShape
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = content,
            maxLines = 1,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
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
fun OrderRequestStatusChip(status: OrderRequestStatus, modifier: Modifier = Modifier) {
    val (label, tone) = when (status) {
        OrderRequestStatus.PENDING -> "Pending" to ChipTone.Warning
        OrderRequestStatus.ACCEPTED -> "Accepted" to ChipTone.Success
        OrderRequestStatus.REJECTED -> "Rejected" to ChipTone.Error
        OrderRequestStatus.CANCELLED -> "Cancelled" to ChipTone.Neutral
    }
    StatusChip(label = label, tone = tone, modifier = modifier)
}

/** Pure display mapping for a harvest-status badge (label, tone). */
fun harvestStatusBadge(harvestStatus: HarvestStatus): Pair<String, ChipTone> =
    when (harvestStatus) {
        HarvestStatus.EXPECTED -> "Expected harvest" to ChipTone.Warning
        HarvestStatus.READY -> "Ready" to ChipTone.Success
        HarvestStatus.FAILED -> "Failed" to ChipTone.Error
    }

/**
 * Clear, colour-coded harvest badge: amber = expected, green = ready, red = failed.
 */
@Composable
fun HarvestStatusBadge(harvestStatus: HarvestStatus, modifier: Modifier = Modifier) {
    val (label, tone) = harvestStatusBadge(harvestStatus)
    StatusChip(label = label, tone = tone, modifier = modifier)
}

/** Bidding on/off chip — green when suppliers can bid, neutral when paused. */
@Composable
fun BiddingChip(biddingEnabled: Boolean, modifier: Modifier = Modifier) {
    StatusChip(
        label = if (biddingEnabled) "Bidding on" else "Bidding off",
        tone = if (biddingEnabled) ChipTone.Success else ChipTone.Neutral,
        modifier = modifier
    )
}

@Composable
private fun chipColors(tone: ChipTone): Pair<Color, Color> {
    val extended = LocalAgroExtendedColors.current
    return when (tone) {
        ChipTone.Primary -> MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
        ChipTone.Secondary -> MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.onSecondaryContainer
        ChipTone.Tertiary -> MaterialTheme.colorScheme.tertiaryContainer to MaterialTheme.colorScheme.onTertiaryContainer
        ChipTone.Success -> extended.successContainer to extended.successOnContainer
        ChipTone.Warning -> extended.warningContainer to extended.warningOnContainer
        ChipTone.Error -> MaterialTheme.colorScheme.errorContainer to MaterialTheme.colorScheme.onErrorContainer
        ChipTone.Neutral -> extended.neutralContainer to extended.neutralOnContainer
    }
}

@Preview(name = "Light", showBackground = true, backgroundColor = 0xFFFAF8F1)
@Preview(name = "Dark", showBackground = true, backgroundColor = 0xFF0C1410, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun HarvestStatusBadgePreview() {
    AgroRouteTheme {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            HarvestStatusBadge(HarvestStatus.EXPECTED)
            HarvestStatusBadge(HarvestStatus.READY)
            HarvestStatusBadge(HarvestStatus.FAILED)
            BiddingChip(biddingEnabled = true)
            BiddingChip(biddingEnabled = false)
        }
    }
}
