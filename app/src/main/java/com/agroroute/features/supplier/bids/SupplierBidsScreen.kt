package com.agroroute.features.supplier.bids

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.agroroute.core.model.Bid
import com.agroroute.core.model.BidStatus
import com.agroroute.core.navigation.Screen
import com.agroroute.core.components.AgroSnackbarHost
import com.agroroute.core.components.AppCard
import com.agroroute.core.components.BidStatusChip
import com.agroroute.core.components.EmptyState
import com.agroroute.core.components.ErrorSnackbarEffect
import com.agroroute.core.components.ListSkeleton
import com.github.thelacspace.icons.Lucide
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Screen showing all bids placed by the supplier.
 */
@Composable
fun SupplierBidsScreen(
    navController: NavController,
    viewModel: SupplierBidsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.loadBids(viewModel.currentSupplierId)
    }

    ErrorSnackbarEffect(uiState.error, snackbarHostState) { viewModel.clearError() }

    val pendingBids = uiState.bids.filter { it.status == BidStatus.PENDING }
    val isEmpty = uiState.bids.isEmpty()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            when {
                uiState.isLoading && isEmpty -> ListSkeleton()
                isEmpty && uiState.error != null -> Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    EmptyState(
                        icon = Lucide.ShoppingCart,
                        title = "Bids couldn't be loaded",
                        subtitle = "Check your connection and try again"
                    )
                }
                isEmpty -> Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    EmptyState(
                        icon = Lucide.ShoppingCart,
                        title = "No Bids Yet",
                        subtitle = "Your bid history will appear here after you place bids on listings"
                    )
                }
                else -> LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (pendingBids.isNotEmpty()) {
                        item {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    text = "Pending bids",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                                Text(
                                    text = "${pendingBids.size} awaiting response",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(top = 3.dp)
                                )
                            }
                        }
                        items(pendingBids, key = { it.bidId }) { bid ->
                            BidCard(
                                bid = bid,
                                onWithdraw = { viewModel.withdrawBid(bid.bidId) },
                                navController = navController
                            )
                        }
                    }
                    item {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = "All bids",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground,
                                modifier = Modifier.padding(top = if (pendingBids.isEmpty()) 0.dp else 8.dp)
                            )
                            Text(
                                text = if (uiState.bids.size == 1) "1 bid" else "${uiState.bids.size} bids",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 3.dp)
                            )
                        }
                    }
                    items(uiState.bids, key = { it.bidId }) { bid ->
                        BidCard(
                            bid = bid,
                            onWithdraw = if (bid.status == BidStatus.PENDING) {
                                { viewModel.withdrawBid(bid.bidId) }
                            } else null,
                            navController = navController
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
}

@Composable
private fun BidCard(
    bid: Bid,
    onWithdraw: (() -> Unit)?,
    navController: NavController
) {
    AppCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                            shape = RoundedCornerShape(12.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Lucide.Gavel,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(Modifier.width(12.dp))
                Text(
                    text = "Bid #${bid.bidId.take(8)}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(Modifier.width(8.dp))
            BidStatusChip(bid.status)
        }
        Spacer(Modifier.height(12.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Offer price", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    "Rs. ${bid.offerPrice}/unit",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text("Placed", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    formatDate(bid.createdAt),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
        if (bid.message.isNotBlank()) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp)
            ) {
                Text(
                    text = "Message: ${bid.message}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                )
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(
                onClick = { navController.navigate(Screen.PlaceBid.createRoute(bid.listingId)) },
                shape = MaterialTheme.shapes.large,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
            ) {
                Text("View listing", fontWeight = FontWeight.SemiBold)
            }
            if (onWithdraw != null) {
                Spacer(Modifier.width(8.dp))
                OutlinedButton(
                    onClick = onWithdraw,
                    shape = MaterialTheme.shapes.large,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                ) {
                    Text("Withdraw", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

private fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
