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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.agroroute.core.model.Bid
import com.agroroute.core.model.BidStatus
import com.agroroute.core.model.availableStock
import com.agroroute.core.widgets.AgroSnackbarHost
import com.agroroute.core.widgets.BidStatusChip
import com.agroroute.core.widgets.EmptyState
import com.agroroute.core.widgets.ErrorSnackbarEffect
import com.agroroute.core.widgets.ListSkeleton
import com.agroroute.core.widgets.LoadingButton
import com.agroroute.core.widgets.ChipTone
import com.agroroute.core.widgets.StatusChip
import com.agroroute.core.util.HarvestAvailabilityDate
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Premium Fresh Green Theme Palette
private val PrimaryGreen = Color(0xFF2E7D32)
private val DarkGreenText = Color(0xFF14532D)
private val AgroLeaf = Color(0xFF43A047)
private val SoftMint = Color(0xFFE8F5E9)
private val AgroBg = Color(0xFFF6FAF6)
private val TextSecondary = Color(0xFF64748B)
private val BorderGray = Color(0xFFE2E8F0)
private val GoldAccent = Color(0xFFF59E0B)
private val GoldSoft = Color(0xFFFEF3C7)

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
            .background(AgroBg) // Soft white-green background
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
                        // Beautiful Listing Header
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp)
                        ) {
                            Text(
                                text = "Bids for",
                                style = MaterialTheme.typography.labelMedium,
                                color = TextSecondary
                            )
                            Text(
                                text = listing.cropName,
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = 0.5.sp
                                ),
                                color = DarkGreenText
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
                                    color = TextSecondary
                                )
                            }
                            Text(
                                text = "${listing.quantity} ${listing.quantityUnit.name} · Rs. ${listing.reservePrice} per ${listing.quantityUnit.name}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                            listing.expectedHarvestAt?.let { harvestAt ->
                                Text(
                                    text = HarvestAvailabilityDate.label(harvestAt),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                            listing.location.takeIf(String::isNotBlank)?.let { location ->
                                Text(
                                    text = "Location: $location",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                            listing.description.takeIf(String::isNotBlank)?.let { notes ->
                                Text(
                                    text = notes,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary,
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
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = if (isBestOffer) 12.dp else 4.dp,
                shape = RoundedCornerShape(22.dp),
                ambientColor = if (isBestOffer) PrimaryGreen.copy(alpha = 0.2f) else Color.Black.copy(alpha = 0.05f),
                spotColor = if (isBestOffer) PrimaryGreen.copy(alpha = 0.2f) else Color.Black.copy(alpha = 0.05f)
            ),
        shape = RoundedCornerShape(22.dp),
        color = Color.White,
        border = if (isBestOffer) {
            androidx.compose.foundation.BorderStroke(2.dp, PrimaryGreen)
        } else {
            androidx.compose.foundation.BorderStroke(1.dp, BorderGray)
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    if (isBestOffer) Brush.verticalGradient(
                        listOf(SoftMint.copy(alpha = 0.4f), Color.White)
                    ) else Brush.verticalGradient(
                        listOf(Color.White, Color.White)
                    )
                )
                .padding(20.dp)
        ) {
            // ─── Header: Supplier Name & Status ───
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Supplier Avatar Placeholder
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(if (isBestOffer) PrimaryGreen else SoftMint),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = bid.supplierName.take(1).uppercase(),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = if (isBestOffer) Color.White else PrimaryGreen
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            text = bid.supplierName,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = DarkGreenText
                        )
                        Text(
                            text = formatDate(bid.createdAt),
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                    }
                }

                BidStatusChip(bid.status)
            }

            Spacer(Modifier.height(18.dp))

            // ─── Center: Price & Best Offer Badge ───
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = buildAnnotatedString {
                        withStyle(style = SpanStyle(fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = TextSecondary)) {
                            append("Rs. ")
                        }
                        withStyle(style = SpanStyle(fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = PrimaryGreen)) {
                            append(bid.offerPrice.toString())
                        }
                        withStyle(style = SpanStyle(fontSize = 14.sp, fontWeight = FontWeight.Medium, color = TextSecondary)) {
                            append(" / unit")
                        }
                    }
                )

                if (isBestOffer) {
                    // Unique Top Bid Badge
                    Row(
                        modifier = Modifier
                            .background(GoldSoft, RoundedCornerShape(12.dp))
                            .border(1.dp, GoldAccent.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "Best Offer",
                            tint = GoldAccent,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = "Top Bid",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color(0xFFB45309) // Dark Gold
                        )
                    }
                }
            }

            // ─── Message Quote Box ───
            if (bid.message.isNotBlank()) {
                Spacer(Modifier.height(16.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFF8FAFC))
                        .border(1.dp, BorderGray, RoundedCornerShape(12.dp))
                        .padding(12.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = Icons.Rounded.FormatQuote,
                        contentDescription = null,
                        tint = PrimaryGreen.copy(alpha = 0.5f),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = bid.message,
                        style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 20.sp),
                        color = TextSecondary,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }


            if (bid.status == BidStatus.PENDING) {
                Spacer(Modifier.height(20.dp))
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
                    modifier = Modifier.align(Alignment.End)
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
            TextButton(onClick = onConfirm) {
                Text("Accept and create order")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}