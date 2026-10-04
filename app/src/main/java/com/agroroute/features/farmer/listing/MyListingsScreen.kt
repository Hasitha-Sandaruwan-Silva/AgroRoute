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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import com.github.thelacspace.icons.Lucide
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.agroroute.core.model.HarvestListing
import com.agroroute.core.model.HarvestStatus
import com.agroroute.core.model.ListingStatus
import com.agroroute.core.model.availableStock
import com.agroroute.core.model.estimatedAvailable
import com.agroroute.core.navigation.Screen
import com.agroroute.core.theme.GreenDeep
import com.agroroute.core.theme.MintContainer
import com.agroroute.core.util.HarvestAvailabilityDate
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

internal fun farmerListingDetailsRoute(listingId: String): String =
    Screen.ViewBids.createRoute(listingId)

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
    var listingPendingReady by remember { mutableStateOf<HarvestListing?>(null) }
    var listingPendingFailure by remember { mutableStateOf<HarvestListing?>(null) }

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
        viewModel.loadFarmerListings(viewModel.currentFarmerId)
        viewModel.loadFarmerDrafts(viewModel.currentFarmerId)
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

    listingPendingReady?.let { listing ->
        MarkHarvestReadyDialog(
            listing = listing,
            onDismiss = { listingPendingReady = null },
            onConfirm = { actualQuantity ->
                listingPendingReady = null
                viewModel.markHarvestReady(listing.listingId, actualQuantity)
            }
        )
    }

    listingPendingFailure?.let { listing ->
        MarkHarvestFailedDialog(
            cropName = listing.cropName,
            onDismiss = { listingPendingFailure = null },
            onConfirm = { reason ->
                listingPendingFailure = null
                viewModel.markHarvestFailed(listing.listingId, reason)
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
                                    onEditClick = {
                                        navController.navigate(Screen.EditListing.createRoute(listing.listingId))
                                    },
                                    onCardClick = {
                                        navController.navigate(farmerListingDetailsRoute(listing.listingId))
                                    },
                                    onToggleBidding = { enabled ->
                                        viewModel.toggleBidding(listing.listingId, enabled)
                                    },
                                    isBiddingUpdating = listing.listingId in uiState.pendingBiddingListingIds,
                                    onCloseClick = { listingPendingClosure = listing },
                                    onMarkReadyClick = { listingPendingReady = listing },
                                    onMarkFailedClick = { listingPendingFailure = listing }
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
    onEditClick: () -> Unit,
    onCardClick: () -> Unit,
    onToggleBidding: (Boolean) -> Unit,
    isBiddingUpdating: Boolean,
    onCloseClick: () -> Unit,
    onMarkReadyClick: () -> Unit,
    onMarkFailedClick: () -> Unit
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
                HarvestStatusChip(listing.harvestStatus)
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
        if (listing.harvestStatus == HarvestStatus.EXPECTED) {
            StockValue("Estimated quantity (estimate)", listing.estimatedAvailable(), listing.quantityUnit.name)
            Text(
                text = "Estimates only — the actual quantity is confirmed when you mark the harvest ready.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 6.dp)
            )
        } else if (listing.harvestStatus == HarvestStatus.FAILED) {
            Text(
                text = buildString {
                    append("This harvest is unavailable")
                    listing.harvestFailureReason?.takeIf(String::isNotBlank)?.let { append(": $it") }
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StockValue("Original", listing.quantity, listing.quantityUnit.name)
                StockValue("Available", listing.availableStock(), listing.quantityUnit.name)
                StockValue("Sold / reserved", listing.soldQuantity, listing.quantityUnit.name)
            }
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
                text = if (listing.harvestStatus == HarvestStatus.EXPECTED) {
                    "Estimated harvest date: ${HarvestAvailabilityDate.format(harvestAt)} (estimate)"
                } else {
                    HarvestAvailabilityDate.label(harvestAt)
                },
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
        listing.harvestUpdatedAt?.let { updatedAt ->
            Text(
                text = "Harvest details last updated ${formatDate(updatedAt)}",
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
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onEditClick, modifier = Modifier.height(48.dp)) {
                Icon(Lucide.FileText, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text("Edit listing")
            }
            TextButton(onClick = onCardClick, modifier = Modifier.height(48.dp)) {
                Icon(Lucide.Gavel, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text("View details & bids")
            }
        }
        if (listing.harvestStatus == HarvestStatus.EXPECTED) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onMarkFailedClick) {
                    Text("Mark harvest failed", color = MaterialTheme.colorScheme.error)
                }
                Spacer(Modifier.width(4.dp))
                TextButton(onClick = onMarkReadyClick) {
                    Text("Mark ready")
                }
            }
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
                if (isBiddingUpdating) {
                    CircularProgressIndicator(
                        modifier = Modifier.width(24.dp).height(24.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Switch(
                        checked = listing.biddingEnabled,
                        onCheckedChange = onToggleBidding,
                        enabled = listing.status == ListingStatus.OPEN &&
                            (listing.biddingEnabled || listing.availableStock() > 0.0)
                    )
                }
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

@Composable
private fun HarvestStatusChip(harvestStatus: HarvestStatus) {
    when (harvestStatus) {
        HarvestStatus.EXPECTED -> HarvestStatusBadge(
            label = "Expected harvest",
            icon = Lucide.CalendarDays,
            container = MaterialTheme.colorScheme.tertiaryContainer,
            content = MaterialTheme.colorScheme.onTertiaryContainer
        )
        HarvestStatus.FAILED -> HarvestStatusBadge(
            label = "Harvest failed",
            icon = Lucide.AlertCircle,
            container = MaterialTheme.colorScheme.errorContainer,
            content = MaterialTheme.colorScheme.onErrorContainer
        )
        HarvestStatus.READY -> HarvestStatusBadge(
            label = "Ready",
            icon = Lucide.Sprout,
            container = MintContainer,
            content = GreenDeep
        )
    }
}

@Composable
private fun HarvestStatusBadge(
    label: String,
    icon: ImageVector,
    container: Color,
    content: Color
) {
    Surface(color = container, shape = MaterialTheme.shapes.small) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = content,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = content
            )
        }
    }
}

@Composable
private fun MarkHarvestReadyDialog(
    listing: HarvestListing,
    onDismiss: () -> Unit,
    onConfirm: (Double) -> Unit
) {
    var quantityText by remember {
        mutableStateOf(
            listing.estimatedAvailable().takeIf { it > 0.0 }?.let {
                if (it % 1.0 == 0.0) it.toLong().toString() else it.toString()
            }.orEmpty()
        )
    }
    val quantity = quantityText.toDoubleOrNull()
    val isValid = quantity != null && quantity > 0.0
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Mark harvest ready?") },
        text = {
            Column {
                Text(
                    text = "Enter the actual quantity available from the “${listing.cropName}” harvest. " +
                        "The listing will sell from this amount, replacing the earlier estimate."
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = quantityText,
                    onValueChange = {
                        if (it.isEmpty() || it.matches(Regex("^\\d*\\.?\\d*$"))) quantityText = it
                    },
                    label = { Text("Actual available quantity") },
                    supportingText = {
                        if (quantityText.isNotEmpty() && !isValid) {
                            Text("Enter a quantity greater than 0")
                        } else {
                            Text("In ${listing.quantityUnit.name}.")
                        }
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    isError = quantityText.isNotEmpty() && !isValid,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = isValid,
                onClick = { quantity?.let(onConfirm) }
            ) {
                Text("Mark ready")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun MarkHarvestFailedDialog(
    cropName: String,
    onDismiss: () -> Unit,
    onConfirm: (String?) -> Unit
) {
    var reason by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Mark harvest as failed?") },
        text = {
            Column {
                Text(
                    text = "Suppliers will see the “$cropName” harvest as unavailable and pending " +
                        "pre-harvest requests will be cancelled. No orders are created and no stock changes."
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    label = { Text("Reason (optional)") },
                    supportingText = { Text("e.g. crop destroyed by weather or pests.") },
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(reason.trim().ifBlank { null }) }) {
                Text("Mark failed", color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Preview(showBackground = true)
@Composable
private fun HarvestStatusBadgesPreview() {
    MaterialTheme {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            HarvestStatusBadge(
                label = "Expected harvest",
                icon = Lucide.CalendarDays,
                container = MaterialTheme.colorScheme.tertiaryContainer,
                content = MaterialTheme.colorScheme.onTertiaryContainer
            )
            HarvestStatusBadge(
                label = "Harvest failed",
                icon = Lucide.AlertCircle,
                container = MaterialTheme.colorScheme.errorContainer,
                content = MaterialTheme.colorScheme.onErrorContainer
            )
            HarvestStatusBadge(
                label = "Ready",
                icon = Lucide.Sprout,
                container = MintContainer,
                content = GreenDeep
            )
        }
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
