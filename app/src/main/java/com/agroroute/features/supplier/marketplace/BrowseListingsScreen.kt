package com.agroroute.features.supplier.marketplace

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
import com.github.thelacspace.icons.Lucide
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.agroroute.core.model.HarvestListing
import com.agroroute.core.navigation.Screen
import com.agroroute.core.widgets.AgroSnackbarHost
import com.agroroute.core.widgets.AppCard
import com.agroroute.core.widgets.ChipTone
import com.agroroute.core.widgets.EmptyState
import com.agroroute.core.widgets.ErrorSnackbarEffect
import com.agroroute.core.widgets.ListSkeleton
import com.agroroute.core.widgets.ListingImage
import com.agroroute.core.widgets.StatusChip
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private enum class MarketFilter { ALL, BIDDING_OPEN, NEAR_ME }

@Composable
fun BrowseListingsScreen(
    navController: NavController,
    viewModel: MarketplaceViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var searchQuery by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf(MarketFilter.ALL) }

    ErrorSnackbarEffect(uiState.error, snackbarHostState) { /* flows from VM */ }

    val filtered = uiState.listings.filter { listing ->
        val matchesSearch = searchQuery.isBlank() ||
            listing.cropName.contains(searchQuery, ignoreCase = true) ||
            listing.location.contains(searchQuery, ignoreCase = true)
        val matchesFilter = when (filter) {
            MarketFilter.ALL -> true
            MarketFilter.BIDDING_OPEN -> listing.biddingEnabled
            MarketFilter.NEAR_ME -> listing.location.isNotBlank()
        }
        matchesSearch && matchesFilter
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("Search crops or location") },
                leadingIcon = { androidx.compose.material3.Icon(Lucide.Search, contentDescription = null) },
                singleLine = true,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    FilterChip(
                        selected = filter == MarketFilter.ALL,
                        onClick = { filter = MarketFilter.ALL },
                        label = { Text("All") }
                    )
                }
                item {
                    FilterChip(
                        selected = filter == MarketFilter.BIDDING_OPEN,
                        onClick = { filter = MarketFilter.BIDDING_OPEN },
                        label = { Text("Bidding open") }
                    )
                }
                item {
                    FilterChip(
                        selected = filter == MarketFilter.NEAR_ME,
                        onClick = { filter = MarketFilter.NEAR_ME },
                        label = { Text("With location") }
                    )
                }
            }
        }

        Box(modifier = Modifier.fillMaxSize()) {
            when {
                uiState.isLoading && uiState.listings.isEmpty() -> ListSkeleton()
                filtered.isEmpty() -> Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    EmptyState(
                        icon = Lucide.Search,
                        title = "No Listings Available",
                        subtitle = "Try adjusting search or filters"
                    )
                }
                else -> LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filtered) { listing ->
                        MarketplaceListingCard(
                            listing = listing,
                            onCardClick = {
                                navController.navigate(Screen.PlaceBid.createRoute(listing.listingId))
                            }
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
private fun MarketplaceListingCard(
    listing: HarvestListing,
    onCardClick: () -> Unit
) {
    AppCard(onClick = onCardClick) {
        ListingImage(imageUrl = listing.imageUrls.firstOrNull(), height = 180.dp)
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
            if (listing.biddingEnabled) {
                StatusChip(label = "Bidding open", tone = ChipTone.Success)
            }
        }
        Spacer(Modifier.height(8.dp))
        listing.category?.let { category ->
            StatusChip(label = category.displayName, tone = ChipTone.Secondary)
            Spacer(Modifier.height(8.dp))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(
                text = "${listing.quantity} ${listing.quantityUnit.name}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Reserve: Rs. ${listing.reservePrice}/${listing.quantityUnit.name}",
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
        if (listing.description.isNotBlank()) {
            Text(
                text = listing.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "by ${listing.farmerName}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Posted ${formatDate(listing.createdAt)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
