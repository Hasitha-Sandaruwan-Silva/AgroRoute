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
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.agroroute.core.model.Bid
import com.agroroute.core.model.BidStatus
import com.agroroute.core.widgets.AgroSnackbarHost
import com.agroroute.core.widgets.AppCard
import com.agroroute.core.widgets.BidStatusChip
import com.agroroute.core.widgets.EmptyState
import com.agroroute.core.widgets.ErrorSnackbarEffect
import com.agroroute.core.widgets.ListSkeleton
import com.agroroute.core.widgets.LoadingButton
import com.agroroute.core.widgets.StatusChip
import com.agroroute.core.widgets.ChipTone
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ViewBidsScreen(
    listingId: String,
    navController: NavController,
    viewModel: BidsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(listingId) {
        viewModel.loadBids(listingId)
    }

    LaunchedEffect(uiState.actionSuccess) {
        if (uiState.actionSuccess) {
            navController.popBackStack()
        }
    }

    ErrorSnackbarEffect(uiState.error, snackbarHostState) { viewModel.clearError() }

    val bestPendingPrice = uiState.bids
        .filter { it.status == BidStatus.PENDING }
        .maxOfOrNull { it.offerPrice }

    Box(modifier = Modifier.fillMaxSize()) {
        when {
            uiState.isLoading && uiState.bids.isEmpty() -> ListSkeleton()
            uiState.bids.isEmpty() -> Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                EmptyState(
                    icon = Icons.Default.Person,
                    title = "No Bids Yet",
                    subtitle = "Suppliers haven't placed any bids on this listing yet"
                )
            }
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                uiState.listing?.let { listing ->
                    item {
                        Text(
                            text = listing.cropName,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }
                }
                items(uiState.bids) { bid ->
                    val isBest = bid.status == BidStatus.PENDING &&
                        bestPendingPrice != null &&
                        bid.offerPrice >= bestPendingPrice
                    BidCard(
                        bid = bid,
                        isBestOffer = isBest,
                        onAcceptClick = { viewModel.acceptBid(bid) },
                        isAccepting = uiState.isLoading
                    )
                }
            }
        }
        AgroSnackbarHost(
            snackbarHostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@Composable
private fun BidCard(
    bid: Bid,
    isBestOffer: Boolean,
    onAcceptClick: () -> Unit,
    isAccepting: Boolean
) {
    AppCard(
        containerColor = if (isBestOffer) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
        } else {
            MaterialTheme.colorScheme.surface
        }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = bid.supplierName,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            BidStatusChip(bid.status)
        }
        if (isBestOffer) {
            StatusChip(label = "Best offer", tone = ChipTone.Success, modifier = Modifier.padding(top = 8.dp))
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Rs. ${bid.offerPrice} per unit",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold
        )
        if (bid.message.isNotBlank()) {
            Text(
                text = bid.message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
        Text(
            text = "Placed ${formatDate(bid.createdAt)}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp)
        )
        if (bid.status == BidStatus.PENDING) {
            Spacer(Modifier.height(12.dp))
            LoadingButton(
                text = "Accept Bid",
                isLoading = isAccepting,
                onClick = onAcceptClick,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

private fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
