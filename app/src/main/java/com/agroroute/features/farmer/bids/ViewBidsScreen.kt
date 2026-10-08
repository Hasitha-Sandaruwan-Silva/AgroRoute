package com.agroroute.features.farmer.bids

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.rounded.FormatQuote
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
import com.agroroute.core.theme.ForestDeep
import com.agroroute.core.components.*
import com.agroroute.core.util.HarvestAvailabilityDate
import com.github.thelacspace.icons.Lucide
import java.text.SimpleDateFormat
import java.util.*

internal fun highestPendingOffer(bids: List<Bid>): Bid? = bids.filter { it.status == BidStatus.PENDING }.maxByOrNull { it.offerPrice }

@Composable
fun ViewBidsScreen(
    listingId: String,
    navController: NavController,
    viewModel: BidsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var bidToAccept by remember { mutableStateOf<Bid?>(null) }

    LaunchedEffect(listingId) { viewModel.loadBids(listingId) }
    LaunchedEffect(uiState.actionSuccess) { if (uiState.actionSuccess) navController.popBackStack() }
    ErrorSnackbarEffect(uiState.error, snackbarHostState) { viewModel.clearError() }

    val bestPendingBid = highestPendingOffer(uiState.bids)

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        when {
            uiState.isLoading && uiState.bids.isEmpty() -> ListSkeleton()
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(top = 12.dp, bottom = 100.dp, start = 12.dp, end = 12.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                uiState.listing?.let { listing ->
                    item {
                        Surface(
                            modifier = Modifier.fillMaxWidth().shadow(12.dp, RoundedCornerShape(24.dp)),
                            shape = RoundedCornerShape(24.dp),
                            color = MaterialTheme.colorScheme.surface
                        ) {
                            Column {
                                Box(Modifier.fillMaxWidth().height(200.dp).clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))) {
                                    ListingImage(imageUrl = listing.imageUrls.firstOrNull(), height = 200.dp)
                                    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.05f), Color.Black.copy(alpha = 0.5f)))))
                                    Surface(
                                        Modifier.align(Alignment.BottomStart).padding(12.dp),
                                        shape = RoundedCornerShape(12.dp),
                                        color = Color.Black.copy(alpha = 0.6f)
                                    ) {
                                        Text(
                                            "Rs. ${listing.reservePrice} / ${listing.quantityUnit.name}",
                                            Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                            color = Color.White,
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 13.sp
                                        )
                                    }
                                }
                                Column(Modifier.padding(16.dp), Arrangement.spacedBy(6.dp)) {
                                    Text("Bids for", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant, letterSpacing = 0.8.sp)
                                    Text(
                                        listing.cropName,
                                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold, fontSize = 22.sp, letterSpacing = (-0.3).sp),
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    listing.category?.let { cat ->
                                        Spacer(Modifier.height(4.dp))
                                        StatusChip(label = "${cat.displayName} (farmer-provided)", tone = ChipTone.Secondary)
                                    }
                                    listing.qualityGrade?.takeIf { it.isNotBlank() }?.let { grade ->
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Lucide.Star, null, Modifier.size(12.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Spacer(Modifier.width(4.dp))
                                            Text("Quality: $grade (farmer-provided)", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                    Spacer(Modifier.height(6.dp))
                                    Surface(shape = RoundedCornerShape(50.dp), color = MaterialTheme.colorScheme.primaryContainer) {
                                        Row(Modifier.padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Lucide.Package, null, Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
                                            Spacer(Modifier.width(6.dp))
                                            Text("${listing.quantity} ${listing.quantityUnit.name} • Rs. ${listing.reservePrice} per ${listing.quantityUnit.name}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                                        }
                                    }
                                    listing.expectedHarvestAt?.let { at ->
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Lucide.Calendar, null, Modifier.size(12.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Spacer(Modifier.width(6.dp))
                                            Text(HarvestAvailabilityDate.label(at), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                    if (listing.location.isNotBlank()) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Lucide.MapPin, null, Modifier.size(12.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Spacer(Modifier.width(6.dp))
                                            Text("Location: ${listing.location}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        }
                                    }
                                    listing.description.takeIf { it.isNotBlank() }?.let { notes ->
                                        Text(notes, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis, lineHeight = 16.sp)
                                    }
                                }
                            }
                        }
                    }
                }

                if (uiState.bids.isEmpty()) {
                    item {
                        Box(Modifier.fillMaxWidth().padding(top = 40.dp), contentAlignment = Alignment.Center) {
                            EmptyState(icon = Lucide.User, title = "No Bids Yet", subtitle = "Suppliers haven't placed any bids on this listing yet")
                        }
                    }
                } else {
                    item {
                        Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                            Text("${uiState.bids.size} bids", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onBackground)
                            if (bestPendingBid != null) {
                                Surface(shape = RoundedCornerShape(50.dp), color = MaterialTheme.colorScheme.primary) {
                                    Row(Modifier.padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Star, null, Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onPrimary)
                                        Spacer(Modifier.width(4.dp))
                                        Text("Best: Rs. ${bestPendingBid.offerPrice}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary)
                                    }
                                }
                            }
                        }
                    }
                    items(uiState.bids, key = { it.bidId }) { bid ->
                        val isBest = bid.status == BidStatus.PENDING && bid.bidId == bestPendingBid?.bidId
                        SuperbBidCard(bid = bid, isBestOffer = isBest, onAcceptClick = { bidToAccept = bid }, onRejectClick = { viewModel.rejectBid(bid) }, isAccepting = uiState.isLoading)
                    }
                }
            }
        }

        bidToAccept?.let { bid ->
            uiState.listing?.let { listing ->
                AcceptBidConfirmationDialog(
                    bid = bid, listing = listing,
                    onDismiss = { bidToAccept = null },
                    onConfirm = { bidToAccept = null; viewModel.acceptBid(bid, listing) }
                )
            }
        }

        AgroSnackbarHost(snackbarHostState = snackbarHostState, modifier = Modifier.align(Alignment.BottomCenter))
    }
}

@Composable
private fun SuperbBidCard(
    bid: Bid,
    isBestOffer: Boolean,
    onAcceptClick: () -> Unit,
    onRejectClick: () -> Unit,
    isAccepting: Boolean
) {
    Surface(
        modifier = Modifier.fillMaxWidth().shadow(if (isBestOffer) 12.dp else 4.dp, RoundedCornerShape(20.dp), ambientColor = if (isBestOffer) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else Color.Black.copy(alpha = 0.05f)),
        shape = RoundedCornerShape(20.dp),
        color = if (isBestOffer) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f) else MaterialTheme.colorScheme.surface,
        border = if (isBestOffer) androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
        else androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp), Arrangement.spacedBy(12.dp)) {
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Box(
                        Modifier.size(46.dp).clip(CircleShape)
                            .background(if (isBestOffer) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primaryContainer)
                            .border(2.dp, if (isBestOffer) MaterialTheme.colorScheme.primary else Color.Transparent, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(bid.supplierName.take(1).uppercase(), fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = if (isBestOffer) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(bid.supplierName, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                            if (isBestOffer) {
                                Spacer(Modifier.width(6.dp))
                                Icon(Icons.Default.Star, null, tint = Color(0xFFFFA000), modifier = Modifier.size(14.dp))
                            }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Lucide.Clock, null, Modifier.size(10.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.width(4.dp))
                            Text(formatDate(bid.createdAt), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                Spacer(Modifier.width(8.dp))
                BidStatusChip(bid.status)
            }

            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                Text(
                    buildAnnotatedString {
                        withStyle(SpanStyle(fontSize = 14.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurfaceVariant)) { append("Rs. ") }
                        withStyle(SpanStyle(fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)) { append(bid.offerPrice.toString()) }
                        withStyle(SpanStyle(fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)) { append(" / unit") }
                    }
                )
                if (isBestOffer) {
                    Surface(shape = RoundedCornerShape(50.dp), color = Color(0xFFFFA000).copy(alpha = 0.15f), border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFFA000).copy(alpha = 0.3f))) {
                        Row(Modifier.padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Star, null, tint = Color(0xFFFFA000), modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Top Bid", fontWeight = FontWeight.ExtraBold, fontSize = 11.sp, color = Color(0xFF5D4037))
                        }
                    }
                }
            }

            if (bid.message.isNotBlank()) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                ) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.Top) {
                        Box(Modifier.size(28.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)), contentAlignment = Alignment.Center) {
                            Icon(Icons.Rounded.FormatQuote, null, tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f), modifier = Modifier.size(16.dp))
                        }
                        Spacer(Modifier.width(10.dp))
                        Text(bid.message, style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 20.sp, fontSize = 13.sp), color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                    }
                }
            }

            if (bid.status == BidStatus.PENDING) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    LoadingButton(
                        text = "Accept This Bid",
                        isLoading = isAccepting,
                        onClick = onAcceptClick,
                        modifier = Modifier.fillMaxWidth().height(52.dp).shadow(8.dp, RoundedCornerShape(14.dp))
                    )
                    TextButton(onClick = onRejectClick, enabled = !isAccepting, modifier = Modifier.align(Alignment.End).heightIn(min = 40.dp)) {
                        Text("Reject bid", color = if (isAccepting) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f) else MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

private fun formatDate(timestamp: Long): String = SimpleDateFormat("MMM dd • hh:mm a", Locale.getDefault()).format(Date(timestamp))

@Composable
internal fun AcceptBidConfirmationDialog(bid: Bid, listing: com.agroroute.core.model.HarvestListing, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Accept this offer?", fontWeight = FontWeight.ExtraBold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)) {
                    Column(Modifier.padding(12.dp), Arrangement.spacedBy(4.dp)) {
                        Text("Supplier: ${bid.supplierName.ifBlank { "Name unavailable" }}", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        Text("Listing: ${listing.cropName}", fontSize = 13.sp, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        Text("Offer: Rs. ${bid.offerPrice} per ${listing.quantityUnit.name}", fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
                        Text("Qty: ${listing.availableStock()} ${listing.quantityUnit.name}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                if (bid.message.isNotBlank()) {
                    Text("Message: ${bid.message}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        },
        confirmButton = {
            Button(onClick = onConfirm, modifier = Modifier.heightIn(min = 48.dp), shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = ForestDeep)) {
                Text("Accept and create order", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss, modifier = Modifier.heightIn(min = 48.dp)) { Text("Cancel") } }
    )
}
