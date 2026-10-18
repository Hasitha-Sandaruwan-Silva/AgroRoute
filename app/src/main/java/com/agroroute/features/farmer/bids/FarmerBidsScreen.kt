package com.agroroute.features.farmer.bids

import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.agroroute.core.runtime.appViewModel as hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.agroroute.core.model.Bid
import com.agroroute.core.model.BidStatus
import com.agroroute.core.model.availableStock
import com.agroroute.core.model.HarvestListing
import com.agroroute.core.model.availableStock
import com.agroroute.core.navigation.Screen
import com.agroroute.core.components.AgroSnackbarHost
import com.agroroute.core.components.AppCard
import com.agroroute.core.components.BidStatusChip
import com.agroroute.core.components.EmptyState
import com.agroroute.core.components.ErrorSnackbarEffect
import com.agroroute.core.components.ListSkeleton
import com.agroroute.core.components.LoadingButton
import com.agroroute.core.components.ProfilePhoneContact
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

    val pendingCount = remember(uiState.bids) { uiState.bids.count { it.status == BidStatus.PENDING } }
    val visibleBids = remember(uiState.bids, selectedStatus) {
        uiState.bids.filter { it.status == selectedStatus }.sortedByDescending { it.createdAt }
    }

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
                    style = MaterialTheme.typography.titleMedium,
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
                            label = { Text("${status.label()} ($count)") },
                            modifier = Modifier.heightIn(min = 48.dp)
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
                                isLoading = uiState.isLoading || uiState.isSubmitting,
                                onOpen = {
                                    navController.navigate(Screen.ViewBids.createRoute(listing.listingId))
                                },
                                onProfile = { navController.navigate(com.agroroute.features.participants.ParticipantRoutes.profile(bid.supplierId)) },
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
    onReject: () -> Unit,
    onProfile: () -> Unit = {}
) {
    AppCard(onClick = onOpen) {
        com.agroroute.core.components.ListingImage(listing.imageUrls.firstOrNull(), height = 96.dp)
        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    listing.cropName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

            }
            Spacer(Modifier.width(8.dp))
            BidStatusChip(bid.status)
        }
        Spacer(Modifier.height(8.dp))
        com.agroroute.features.participants.ParticipantIdentityRow(bid.supplierName, "Supplier", onProfile)
        ProfilePhoneContact(userId = bid.supplierId, personLabel = "Supplier")
        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = "Offer",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Rs. ${bid.offerPrice} / ${listing.quantityUnit.name}",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                Text(
                    text = "Requested quantity",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${bid.requestedQuantity ?: listing.availableStock()} ${listing.quantityUnit.name}",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
        HorizontalDivider(
            modifier = Modifier.padding(vertical = 12.dp),
            thickness = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        )
        Text("Available stock: ${listing.availableStock()} ${listing.quantityUnit.name}",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (listing.location.isNotBlank()) Text(com.agroroute.core.components.profileLocationLabel(listing.location),
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
        TextButton(
            onClick = onOpen,
            modifier = Modifier
                .align(Alignment.End)
                .heightIn(min = 48.dp)
        ) {
            Text(if (bid.status == BidStatus.ACCEPTED) "Accepted ? View listing and order details" else "View listing and bids")
        }
        if (bid.status == BidStatus.PENDING) {
            com.agroroute.core.components.listingAcceptanceBlockedReason(listing, true, quantity = bid.requestedQuantity, deliveryLocation = bid.deliveryLocation)?.let {
                Text(it, style = MaterialTheme.typography.bodySmall)
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                LoadingButton(
                    text = "Accept",
                    enabled = com.agroroute.core.runtime.LocalMutationAllowed.current && com.agroroute.core.components.listingAcceptanceBlockedReason(listing, true, quantity = bid.requestedQuantity, deliveryLocation = bid.deliveryLocation) == null,
                    isLoading = isLoading,
                    onClick = onAccept,
                    modifier = Modifier.weight(1f)
                )
                TextButton(
                    onClick = onReject,
                    enabled = com.agroroute.core.runtime.LocalMutationAllowed.current && (!isLoading),
                    modifier = Modifier.heightIn(min = 48.dp)
                ) {
                    Text(
                        text = "Reject",
                        color = if (!com.agroroute.core.runtime.LocalMutationAllowed.current || isLoading) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                        else MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

private fun BidStatus.label(): String = name.lowercase().replaceFirstChar { it.uppercase() }

private fun formatBidDate(timestamp: Long): String =
    SimpleDateFormat("MMM dd, yyyy · hh:mm a", Locale.getDefault()).format(Date(timestamp))
