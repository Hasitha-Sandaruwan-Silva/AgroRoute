@file:OptIn(ExperimentalMaterial3Api::class)

package com.agroroute.farmer

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ToggleOff
import androidx.compose.material.icons.filled.ToggleOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.agroroute.common.model.HarvestListing
import com.agroroute.common.model.ListingStatus
import com.agroroute.common.navigation.Screen
import com.agroroute.common.ui.EmptyState
import com.agroroute.common.util.Constants
import java.text.SimpleDateFormat
import java.util.*

/**
 * Screen showing all listings created by the currently signed-in farmer.
 */
@Composable
fun MyListingsScreen(
    navController: NavController,
    viewModel: ListingViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Load listings for the current farmer (using a placeholder ID for now)
    LaunchedEffect(Unit) {
        viewModel.loadFarmerListings("current_farmer_id") // TODO: Get from auth context
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Listings") }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { navController.navigate(Screen.CreateListing.route) }
            ) {
                Icon(Icons.Default.Add, contentDescription = "Create Listing")
            }
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
                uiState.listings.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        EmptyState(
                            icon = Icons.Default.Add,
                            title = "No Listings Yet",
                            subtitle = "Create your first harvest listing to start selling"
                        )
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(uiState.listings) { listing ->
                            ListingCard(
                                listing = listing,
                                onCardClick = {
                                    navController.navigate(Screen.ViewBids.createRoute(listing.listingId))
                                },
                                onToggleBidding = { enabled ->
                                    viewModel.toggleBidding(listing.listingId, enabled)
                                }
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
private fun ListingCard(
    listing: HarvestListing,
    onCardClick: () -> Unit,
    onToggleBidding: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onCardClick),
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
                    text = listing.cropName,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                StatusBadge(listing.status)
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "${listing.quantity} ${listing.quantityUnit.name}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Rs. ${listing.reservePrice}/${listing.quantityUnit.name}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )
            }

            if (listing.location.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = listing.location,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Created ${formatDate(listing.createdAt)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                IconButton(
                    onClick = { onToggleBidding(!listing.biddingEnabled) }
                ) {
                    Icon(
                        imageVector = if (listing.biddingEnabled) Icons.Default.ToggleOn else Icons.Default.ToggleOff,
                        contentDescription = if (listing.biddingEnabled) "Disable Bidding" else "Enable Bidding",
                        tint = if (listing.biddingEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun StatusBadge(status: ListingStatus) {
    val (label, containerColor, contentColor) = when (status) {
        ListingStatus.OPEN -> Triple(
            "Open",
            MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.onPrimaryContainer
        )
        ListingStatus.CLOSED -> Triple(
            "Closed",
            MaterialTheme.colorScheme.secondaryContainer,
            MaterialTheme.colorScheme.onSecondaryContainer
        )
        ListingStatus.FULFILLED -> Triple(
            "Fulfilled",
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
    val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
