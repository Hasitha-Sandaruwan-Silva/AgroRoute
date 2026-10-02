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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import com.github.thelacspace.icons.Lucide
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
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
import com.agroroute.core.model.HarvestListing
import com.agroroute.core.model.ListingStatus
import com.agroroute.core.model.availableStock
import com.agroroute.core.navigation.Screen
import com.agroroute.core.widgets.AgroSnackbarHost
import com.agroroute.core.widgets.AppCard
import com.agroroute.core.widgets.ChipTone
import com.agroroute.core.widgets.EmptyState
import com.agroroute.core.widgets.ErrorSnackbarEffect
import com.agroroute.core.widgets.ListSkeleton
import com.agroroute.core.widgets.ListingImage
import com.agroroute.core.widgets.ListingStatusChip
import com.agroroute.core.widgets.StatusChip
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private enum class ListingSort(val label: String) {
    NEWEST("Newest"),
    OLDEST("Oldest"),
    PRICE_LOW_TO_HIGH("Price: low to high"),
    PRICE_HIGH_TO_LOW("Price: high to low")
}

@Composable
fun MyListingsScreen(
    navController: NavController,
    viewModel: ListingViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var searchQuery by remember { mutableStateOf("") }
    var selectedStatus by remember { mutableStateOf<ListingStatus?>(null) }
    var selectedSort by remember { mutableStateOf(ListingSort.NEWEST) }
    var sortMenuExpanded by remember { mutableStateOf(false) }
    var listingPendingClosure by remember { mutableStateOf<HarvestListing?>(null) }

    val publishedListings = remember(uiState.listings) { uiState.listings.filterNot { it.isDraft } }
    val visibleListings = remember(publishedListings, searchQuery, selectedStatus, selectedSort) {
        publishedListings
            .filter { listing ->
                val matchesSearch = searchQuery.isBlank() ||
                    listing.cropName.contains(searchQuery, ignoreCase = true) ||
                    listing.location.contains(searchQuery, ignoreCase = true)
                matchesSearch && (selectedStatus == null || listing.status == selectedStatus)
            }
            .let { listings ->
                when (selectedSort) {
                    ListingSort.NEWEST -> listings.sortedByDescending { it.createdAt }
                    ListingSort.OLDEST -> listings.sortedBy { it.createdAt }
                    ListingSort.PRICE_LOW_TO_HIGH -> listings.sortedBy { it.reservePrice }
                    ListingSort.PRICE_HIGH_TO_LOW -> listings.sortedByDescending { it.reservePrice }
                }
            }
    }

    LaunchedEffect(Unit) {
        viewModel.loadFarmerListings("current_farmer_id")
        viewModel.loadFarmerDrafts("current_farmer_id")
    }

    ErrorSnackbarEffect(uiState.error, snackbarHostState) { viewModel.clearError() }

    listingPendingClosure?.let { listing ->
        AlertDialog(
            onDismissRequest = { listingPendingClosure = null },
            title = { Text("Close this listing?") },
            text = { Text("Suppliers will no longer be able to bid. Existing bids and orders will be preserved.") },
            confirmButton = {
                TextButton(onClick = {
                    listingPendingClosure = null
                    viewModel.closeListing(listing)
                }) {
                    Text("Close listing")
                }
            },
            dismissButton = {
                TextButton(onClick = { listingPendingClosure = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { navController.navigate(Screen.CreateListing.route) },
                icon = { Icon(Lucide.Plus, contentDescription = null) },
                text = { Text("New Listing") },
                containerColor = MaterialTheme.colorScheme.primary,
                shape = MaterialTheme.shapes.large
            )
        },
        snackbarHost = { AgroSnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (uiState.drafts.isNotEmpty()) {
                AppCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Saved drafts (${uiState.drafts.size})",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Open to choose a draft and resume editing",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        TextButton(onClick = { navController.navigate(Screen.CreateListing.route) }) {
                            Text("Resume")
                        }
                    }
                }
            }
            when {
                uiState.isLoading && publishedListings.isEmpty() -> ListSkeleton()
                publishedListings.isEmpty() -> Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    EmptyState(
                        icon = Lucide.Plus,
                        title = "No Listings Yet",
                        subtitle = "Create your first harvest listing to start selling"
                    )
                }
                else -> {
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                        Text(
                            text = "Your harvests",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${publishedListings.size} ${if (publishedListings.size == 1) "listing" else "listings"}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 2.dp, bottom = 12.dp)
                        )
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            label = { Text("Search crop or location") },
                            leadingIcon = { Icon(Lucide.Search, contentDescription = null) },
                            singleLine = true,
                            shape = MaterialTheme.shapes.medium,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(8.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            item {
                                FilterChip(
                                    selected = selectedStatus == null,
                                    onClick = { selectedStatus = null },
                                    label = { Text("All") }
                                )
                            }
                            items(ListingStatus.entries) { status ->
                                FilterChip(
                                    selected = selectedStatus == status,
                                    onClick = { selectedStatus = status },
                                    label = { Text(status.displayLabel()) }
                                )
                            }
                        }
                        Box {
                            TextButton(onClick = { sortMenuExpanded = true }) {
                                Text("Sort: ${selectedSort.label}")
                            }
                            DropdownMenu(
                                expanded = sortMenuExpanded,
                                onDismissRequest = { sortMenuExpanded = false }
                            ) {
                                ListingSort.entries.forEach { sort ->
                                    DropdownMenuItem(
                                        text = { Text(sort.label) },
                                        onClick = {
                                            selectedSort = sort
                                            sortMenuExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                    if (visibleListings.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                EmptyState(
                                    icon = Lucide.Search,
                                    title = "No matching listings",
                                    subtitle = "Try another crop, location, or status"
                                )
                                TextButton(onClick = {
                                    searchQuery = ""
                                    selectedStatus = null
                                }) {
                                    Text("Clear search and filters")
                                }
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(visibleListings, key = { it.listingId }) { listing ->
                                ListingCard(
                                    listing = listing,
                                    onCardClick = {
                                        navController.navigate(Screen.ViewBids.createRoute(listing.listingId))
                                    },
                                    onToggleBidding = { enabled ->
                                        viewModel.toggleBidding(listing.listingId, enabled)
                                    },
                                    onCloseClick = { listingPendingClosure = listing }
                                )
                            }
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
    onToggleBidding: (Boolean) -> Unit,
    onCloseClick: () -> Unit
) {
    AppCard(onClick = onCardClick) {
        ListingImage(imageUrl = listing.imageUrls.firstOrNull(), height = 176.dp)
        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = listing.cropName,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                ListingStatusChip(listing.status)
                if (listing.status == ListingStatus.OPEN && !listing.biddingEnabled) {
                    StatusChip(label = "Paused", tone = ChipTone.Warning)
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        listing.category?.let { category ->
            StatusChip(label = "${category.displayName} (farmer-provided)", tone = ChipTone.Secondary)
            Spacer(Modifier.height(8.dp))
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            StockValue("Original", listing.quantity, listing.quantityUnit.name)
            StockValue("Available", listing.availableStock(), listing.quantityUnit.name)
            StockValue("Sold / reserved", listing.soldQuantity, listing.quantityUnit.name)
        }
        Text(
            text = "Reserve: Rs. ${listing.reservePrice} / ${listing.quantityUnit.name}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(top = 8.dp)
        )
        listing.expectedHarvestAt?.let { harvestAt ->
            Text(
                text = "Expected harvest ${formatDate(harvestAt)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 6.dp)
            )
        }
        listing.qualityGrade?.takeIf(String::isNotBlank)?.let { grade ->
            Text(
                text = "Quality: $grade",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
        listing.description.takeIf(String::isNotBlank)?.let { notes ->
            Text(
                text = notes,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
        if (listing.location.isNotBlank()) {
            Row(
                modifier = Modifier.padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Location",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = listing.location,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        }
        TextButton(
            onClick = onCardClick,
            modifier = Modifier.align(Alignment.End)
        ) {
            Icon(Lucide.Gavel, contentDescription = null)
            Spacer(Modifier.width(6.dp))
            Text("View details & bids")
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
                    text = if (listing.biddingEnabled) "Bidding on" else "Paused",
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(end = 8.dp)
                )
                Switch(
                    checked = listing.biddingEnabled,
                    onCheckedChange = onToggleBidding,
                    enabled = listing.status == ListingStatus.OPEN
                )
            }
        }
        if (listing.status == ListingStatus.OPEN) {
            TextButton(
                onClick = onCloseClick,
                modifier = Modifier.align(Alignment.End)
            ) {
                Text("Close listing")
            }
        }
    }
}

@Composable
private fun StockValue(label: String, quantity: Double, unit: String) {
    Column {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text("$quantity $unit", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
    }
}

private fun ListingStatus.displayLabel(): String = when (this) {
    ListingStatus.OPEN -> "Open"
    ListingStatus.CLOSED -> "Closed"
    ListingStatus.FULFILLED -> "Fulfilled"
}

private fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
