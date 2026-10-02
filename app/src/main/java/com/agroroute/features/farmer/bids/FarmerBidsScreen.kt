package com.agroroute.features.farmer.bids

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.agroroute.core.model.Bid
import com.agroroute.core.model.BidStatus
import com.agroroute.core.model.HarvestListing
import com.agroroute.core.model.availableStock
import com.agroroute.core.navigation.Screen
import com.agroroute.core.widgets.AgroSnackbarHost
import com.agroroute.core.widgets.AppCard
import com.agroroute.core.widgets.BidStatusChip
import com.agroroute.core.widgets.EmptyState
import com.agroroute.core.widgets.ErrorSnackbarEffect
import com.agroroute.core.widgets.ListSkeleton
import com.agroroute.core.widgets.LoadingButton
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val farmerBidStatuses = listOf(
    BidStatus.PENDING,
    BidStatus.ACCEPTED,
    BidStatus.REJECTED,
    BidStatus.WITHDRAWN
)

@Composable
fun FarmerBidsScreen(
    navController: NavController,
    viewModel: BidsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var selectedStatus by remember { mutableStateOf(BidStatus.PENDING) }
    var bidToAccept by remember { mutableStateOf<Bid?>(null) }

    LaunchedEffect(Unit) {
        viewModel.loadFarmerBids()
    }
    ErrorSnackbarEffect(uiState.error, snackbarHostState) { viewModel.clearError() }

    val pendingCount = uiState.bids.count { it.status == BidStatus.PENDING }
    val visibleBids = uiState.bids
        .filter { it.status == selectedStatus }
        .sortedByDescending { it.createdAt }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Incoming bids",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "$pendingCount pending ${if (pendingCount == 1) "offer" else "offers"}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(farmerBidStatuses) { status ->
                        val count = uiState.bids.count { it.status == status }
                        FilterChip(
                            selected = selectedStatus == status,
                            onClick = { selectedStatus = status },
                            label = { Text("${status.label()} ($count)") }
                        )
                    }
                }
            }

            when {
                uiState.isLoading && uiState.bids.isEmpty() -> ListSkeleton()
                uiState.listingsById.isEmpty() -> EmptyState(
                    icon = com.github.thelacspace.icons.Lucide.Gavel,
                    title = "No listings yet",
                    subtitle = "Your incoming bids will appear here when Suppliers make offers on your listings"
                )
                visibleBids.isEmpty() -> EmptyState(
                    icon = com.github.thelacspace.icons.Lucide.Gavel,
                    title = when (selectedStatus) {
                        BidStatus.PENDING -> "No pending bids"
                        BidStatus.ACCEPTED -> "No accepted bids"
                        BidStatus.REJECTED -> "No rejected bids"
                        BidStatus.WITHDRAWN -> "No withdrawn bids"
                        else -> "No bids"
                    },
                    subtitle = if (selectedStatus == BidStatus.PENDING) {
                        "There are no offers waiting for your decision. New Supplier bids will appear here."
                    } else {
                        "Bids in this section will appear here."
                    }
                )
                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(visibleBids, key = { it.bidId }) { bid ->
                        val listing = uiState.listingsById[bid.listingId]
                        if (listing != null) {
                            FarmerBidCard(
                                bid = bid,
                                listing = listing,
                                isLoading = uiState.isLoading,
                                onOpen = {
                                    navController.navigate(Screen.ViewBids.createRoute(listing.listingId))
                                },
                                onAccept = { bidToAccept = bid },
                                onReject = { viewModel.rejectBid(bid) }
                            )
                        }
                    }
                }
            }
        }

        bidToAccept?.let { bid ->
            uiState.listingsById[bid.listingId]?.let { listing ->
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
private fun FarmerBidCard(
    bid: Bid,
    listing: HarvestListing,
    isLoading: Boolean,
    onOpen: () -> Unit,
    onAccept: () -> Unit,
    onReject: () -> Unit
) {
    AppCard(onClick = onOpen) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(listing.cropName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    text = "Supplier: ${bid.supplierName.ifBlank { "Name unavailable" }}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            BidStatusChip(bid.status)
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Offer: Rs. ${bid.offerPrice} per ${listing.quantityUnit.name}",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = "Listing quantity: ${listing.quantity} ${listing.quantityUnit.name}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = "Bid date: ${formatBidDate(bid.createdAt)}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (bid.message.isNotBlank()) {
            Text(
                text = "Supplier message: ${bid.message}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
        TextButton(onClick = onOpen, modifier = Modifier.align(Alignment.End)) {
            Text("View listing and bids")
        }
        if (bid.status == BidStatus.PENDING) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                LoadingButton(
                    text = "Accept",
                    isLoading = isLoading,
                    onClick = onAccept,
                    modifier = Modifier.weight(1f)
                )
                TextButton(onClick = onReject, enabled = !isLoading) {
                    Text("Reject")
                }
            }
        }
    }
}

private fun BidStatus.label(): String = name.lowercase().replaceFirstChar { it.uppercase() }

private fun formatBidDate(timestamp: Long): String =
    SimpleDateFormat("MMM dd, yyyy · hh:mm a", Locale.getDefault()).format(Date(timestamp))
