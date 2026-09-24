@file:OptIn(ExperimentalMaterial3Api::class)

package com.agroroute.farmer

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.agroroute.common.model.Bid
import com.agroroute.common.model.BidStatus
import com.agroroute.common.ui.EmptyState
import java.text.SimpleDateFormat
import java.util.*

/**
 * Screen showing all bids received for a specific listing.
 */
@Composable
fun ViewBidsScreen(
    listingId: String,
    navController: NavController,
    viewModel: BidsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Load bids for the listing
    LaunchedEffect(listingId) {
        viewModel.loadBids(listingId)
    }

    // Navigate back after successful bid acceptance
    LaunchedEffect(uiState.actionSuccess) {
        if (uiState.actionSuccess) {
            navController.popBackStack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text("Bids Received")
                        uiState.listing?.let { listing ->
                            Text(
                                text = listing.cropName,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                uiState.isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                uiState.bids.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        EmptyState(
                            icon = Icons.Default.Person,
                            title = "No Bids Yet",
                            subtitle = "Suppliers haven't placed any bids on this listing yet"
                        )
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(uiState.bids) { bid ->
                            BidCard(
                                bid = bid,
                                onAcceptClick = { viewModel.acceptBid(bid) },
                                isAccepting = uiState.isLoading
                            )
                        }
                    }
                }
            }
        }

        // Error snackbar
        uiState.error?.let { error ->
            LaunchedEffect(error) {
                viewModel.clearError()
            }
        }
    }
}

@Composable
private fun BidCard(
    bid: Bid,
    onAcceptClick: () -> Unit,
    isAccepting: Boolean
) {
    Card(
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
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
                BidStatusBadge(bid.status)
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Rs. ${bid.offerPrice} per unit",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Medium
            )

            if (bid.message.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = bid.message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Placed ${formatDate(bid.createdAt)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (bid.status == BidStatus.PENDING) {
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = onAcceptClick,
                    enabled = !isAccepting,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (isAccepting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Accept Bid")
                    }
                }
            }
        }
    }
}

@Composable
private fun BidStatusBadge(status: BidStatus) {
    val (label, containerColor, contentColor) = when (status) {
        BidStatus.PENDING -> Triple(
            "Pending",
            MaterialTheme.colorScheme.secondaryContainer,
            MaterialTheme.colorScheme.onSecondaryContainer
        )
        BidStatus.ACCEPTED -> Triple(
            "Accepted",
            MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.onPrimaryContainer
        )
        BidStatus.REJECTED -> Triple(
            "Rejected",
            MaterialTheme.colorScheme.errorContainer,
            MaterialTheme.colorScheme.onErrorContainer
        )
        BidStatus.WITHDRAWN -> Triple(
            "Withdrawn",
            MaterialTheme.colorScheme.tertiaryContainer,
            MaterialTheme.colorScheme.onTertiaryContainer
        )
    }

    Surface(
        color = containerColor,
        shape = MaterialTheme.shapes.small
    ) {
        Text(
            text = label,
            color = contentColor,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

private fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
