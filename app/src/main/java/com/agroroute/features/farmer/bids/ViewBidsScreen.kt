package com.agroroute.features.farmer.bids

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.rounded.FormatQuote
import com.github.thelacspace.icons.Lucide
import androidx.compose.material3.Icon
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.agroroute.core.model.Bid
import com.agroroute.core.model.BidStatus
import com.agroroute.core.model.availableStock
import com.agroroute.core.theme.AccentAmber
import com.agroroute.core.widgets.AgroSnackbarHost
import com.agroroute.core.widgets.BidStatusChip
import com.agroroute.core.widgets.EmptyState
import com.agroroute.core.widgets.ErrorSnackbarEffect
import com.agroroute.core.widgets.ListSkeleton
import com.agroroute.core.widgets.ListingImage
import com.agroroute.core.widgets.LoadingButton
import com.agroroute.core.widgets.ChipTone
import com.agroroute.core.widgets.StatusChip
import com.agroroute.core.util.HarvestAvailabilityDate
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

internal fun highestPendingOffer(bids: List<Bid>): Bid? =
    bids.filter { it.status == BidStatus.PENDING }.maxByOrNull { it.offerPrice }

@Composable
fun ViewBidsScreen(
    listingId: String,
    navController: NavController,
    viewModel: BidsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var bidToAccept by remember { mutableStateOf<Bid?>(null) }

    // ✅ LOGIC UNCHANGED
    LaunchedEffect(listingId) {
        viewModel.loadBids(listingId)
    }

    LaunchedEffect(uiState.actionSuccess) {
        if (uiState.actionSuccess) {
            navController.popBackStack()
        }
    }

    ErrorSnackbarEffect(uiState.error, snackbarHostState) { viewModel.clearError() }

    val bestPendingBid = highestPendingOffer(uiState.bids)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        when {
            uiState.isLoading && uiState.bids.isEmpty() -> ListSkeleton()

            else -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(top = 16.dp, bottom = 100.dp, start = 16.dp, end = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                uiState.listing?.let { listing ->
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp)
                        ) {
                            ListingImage(
                                imageUrl = listing.imageUrls.firstOrNull(),
                                height = 200.dp
                            )
                            Spacer(Modifier.height(12.dp))
                            Text(
                                text = "Bids for",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = listing.cropName,
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    fontWeight = FontWeight.ExtraBold
                                ),
                                color = MaterialTheme.colorScheme.onBackground,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            listing.category?.let { category ->
                                Spacer(Modifier.height(8.dp))
                                StatusChip(label = "${category.displayName} (farmer-provided)", tone = ChipTone.Secondary)
                            }
                            listing.qualityGrade?.takeIf(String::isNotBlank)?.let { grade ->
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = "Quality: $grade (farmer-provided)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                text = "${listing.quantity} ${listing.quantityUnit.name} · Rs. ${listing.reservePrice} per ${listing.quantityUnit.name}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                            listing.expectedHarvestAt?.let { harvestAt ->
                                Text(
                                    text = HarvestAvailabilityDate.label(harvestAt),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                            listing.location.takeIf(String::isNotBlank)?.let { location ->
                                Text(
                                    text = "Location: $location",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                            listing.description.takeIf(String::isNotBlank)?.let { notes ->
                                Text(
                                    text = notes,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                        }
                    }
                }

                if (uiState.bids.isEmpty()) {
                    item {
                        EmptyState(
                            icon = Lucide.User,
                            title = "No Bids Yet",
                            subtitle = "Suppliers haven't placed any bids on this listing yet"
                        )
                    }
                } else {
                    items(uiState.bids) { bid ->
                        val isBest = bid.status == BidStatus.PENDING &&
                            bid.bidId == bestPendingBid?.bidId

                        PremiumBidCard(
                            bid = bid,
                            isBestOffer = isBest,
                            onAcceptClick = { bidToAccept = bid },
                            onRejectClick = { viewModel.rejectBid(bid) },
                            isAccepting = uiState.isLoading
                        )
                    }
                }
            }
        }

        bidToAccept?.let { bid ->
            uiState.listing?.let { listing ->
                AcceptBidConfirmationDialog(
                    bid = bid,
                    listing = listing,
                    onDismiss = { bidToAccept = null },
                    onConfirm = {
                        bidToAccept = null
                        viewModel.acceptBid(bid, listing)
                    }
                )
            }
        }

        AgroSnackbarHost(
            snackbarHostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@Composable
private fun PremiumBidCard(
    bid: Bid,
    isBestOffer: Boolean,
    onAcceptClick: () -> Unit,
    onRejectClick: () -> Unit,
    isAccepting: Boolean
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        border = if (isBestOffer) {
            androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
        } else {
            androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        },
        shadowElevation = 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (isBestOffer) {
                        Modifier.background(
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                        )
                    } else Modifier
                )
                .padding(16.dp)
        ) {
            // ─── Header: supplier name & status ───
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(
                                if (isBestOffer) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.primaryContainer
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = bid.supplierName.take(1).uppercase(),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = if (isBestOffer) MaterialTheme.colorScheme.onPrimary
                            else MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            text = bid.supplierName,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = formatDate(bid.createdAt),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(Modifier.width(8.dp))
                BidStatusChip(bid.status)
            }

            Spacer(Modifier.height(16.dp))

            // ─── Price & best-offer badge ───
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = buildAnnotatedString {
                        withStyle(
                            style = SpanStyle(
                                fontSize = 18.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        ) {
                            append("Rs. ")
                        }
                        withStyle(
                            style = SpanStyle(
                                fontSize = 28.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            append(bid.offerPrice.toString())
                        }
                        withStyle(
                            style = SpanStyle(
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        ) {
                            append(" / unit")
                        }
                    }
                )

                if (isBestOffer) {
                    Row(
                        modifier = Modifier
                            .background(AccentAmber.copy(alpha = 0.18f), MaterialTheme.shapes.small)
                            .border(1.dp, AccentAmber.copy(alpha = 0.6f), MaterialTheme.shapes.small)
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "Best offer",
                            tint = AccentAmber,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = "Top Bid",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            // ─── Supplier message ───
            if (bid.message.isNotBlank()) {
                Spacer(Modifier.height(16.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(MaterialTheme.shapes.small)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .border(1.dp, MaterialTheme.colorScheme.outline, MaterialTheme.shapes.small)
                        .padding(12.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = Icons.Rounded.FormatQuote,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = bid.message,
                        style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 20.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }

            if (bid.status == BidStatus.PENDING) {
                Spacer(Modifier.height(16.dp))
                LoadingButton(
                    text = "Accept This Bid",
                    isLoading = isAccepting,
                    onClick = onAcceptClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                )
                TextButton(
                    onClick = onRejectClick,
                    enabled = !isAccepting,
                    modifier = Modifier
                        .align(Alignment.End)
                        .heightIn(min = 48.dp)
                ) {
                    Text("Reject bid")
                }
            }
        }
    }
}

private fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("MMM dd, yyyy • hh:mm a", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

@Composable
internal fun AcceptBidConfirmationDialog(
    bid: Bid,
    listing: com.agroroute.core.model.HarvestListing,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Accept this offer?") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Supplier: ${bid.supplierName.ifBlank { "Name unavailable" }}")
                Text("Listing: ${listing.cropName}")
                Text("Offer: Rs. ${bid.offerPrice} per ${listing.quantityUnit.name}")
                Text("Order quantity: ${listing.availableStock()} ${listing.quantityUnit.name}")
                if (bid.message.isNotBlank()) Text("Supplier message: ${bid.message}")
            }
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                modifier = Modifier.heightIn(min = 48.dp)
            ) {
                Text("Accept and create order")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.heightIn(min = 48.dp)
            ) {
                Text("Cancel")
            }
        }
    )
}
