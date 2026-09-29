package com.agroroute.features.farmer.listing

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
import com.github.thelacspace.icons.Lucide
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
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
import com.agroroute.core.model.HarvestListing
import com.agroroute.core.navigation.Screen
import com.agroroute.core.widgets.AgroSnackbarHost
import com.agroroute.core.widgets.AppCard
import com.agroroute.core.widgets.EmptyState
import com.agroroute.core.widgets.ErrorSnackbarEffect
import com.agroroute.core.widgets.ListSkeleton
import com.agroroute.core.widgets.ListingImage
import com.agroroute.core.widgets.ListingStatusChip
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun MyListingsScreen(
    navController: NavController,
    viewModel: ListingViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.loadFarmerListings("current_farmer_id")
    }

    ErrorSnackbarEffect(uiState.error, snackbarHostState) { viewModel.clearError() }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { navController.navigate(Screen.CreateListing.route) },
                containerColor = MaterialTheme.colorScheme.primary,
                shape = MaterialTheme.shapes.large
            ) {
                Icon(Lucide.Plus, contentDescription = "Create Listing")
            }
        },
        snackbarHost = { AgroSnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                when {
                    uiState.isLoading && uiState.listings.isEmpty() -> ListSkeleton()
                    uiState.listings.isEmpty() -> Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        EmptyState(
                            icon = Lucide.Plus,
                            title = "No Listings Yet",
                            subtitle = "Create your first harvest listing to start selling"
                        )
                    }
                    else -> LazyColumn(
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
    }
}

@Composable
private fun ListingCard(
    listing: HarvestListing,
    onCardClick: () -> Unit,
    onToggleBidding: (Boolean) -> Unit
) {
    AppCard(onClick = onCardClick) {
        ListingImage(imageUrl = listing.imageUrls.firstOrNull())
        Spacer(Modifier.height(12.dp))
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
            ListingStatusChip(listing.status)
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(
                text = "${listing.quantity} ${listing.quantityUnit.name}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Rs. ${listing.reservePrice}/${listing.quantityUnit.name}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold
            )
        }
        if (listing.location.isNotBlank()) {
            Text(
                text = listing.location,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Created ${formatDate(listing.createdAt)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Bidding",
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(end = 8.dp)
                )
                Switch(
                    checked = listing.biddingEnabled,
                    onCheckedChange = onToggleBidding
                )
            }
        }
    }
}

private fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
