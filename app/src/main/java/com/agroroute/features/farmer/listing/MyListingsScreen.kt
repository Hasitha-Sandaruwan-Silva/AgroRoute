package com.agroroute.features.farmer.listing

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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import com.github.thelacspace.icons.Lucide
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.agroroute.core.model.HarvestListing
import com.agroroute.core.model.HarvestStatus
import com.agroroute.core.model.ListingCategory
import com.agroroute.core.model.ListingStatus
import com.agroroute.core.model.availableStock
import com.agroroute.core.model.estimatedAvailable
import com.agroroute.core.navigation.Screen
import com.agroroute.core.theme.AgroRouteTheme
import com.agroroute.core.theme.GreenDeep
import com.agroroute.core.theme.MintContainer
import com.agroroute.core.util.HarvestAvailabilityDate
import com.agroroute.core.widgets.AgroSnackbarHost
import com.agroroute.core.widgets.AppCard
import com.agroroute.core.widgets.BiddingChip
import com.agroroute.core.widgets.ChipTone
import com.agroroute.core.widgets.EmptyState
import com.agroroute.core.widgets.ErrorSnackbarEffect
import com.agroroute.core.widgets.HarvestStatusBadge
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
                TextButton(
                    onClick = {
                        listingPendingClosure = null
                        viewModel.closeListing(listing)
                    },
                    modifier = Modifier.heightIn(min = 48.dp)
                ) {
                    Text("Close listing")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { listingPendingClosure = null },
                    modifier = Modifier.heightIn(min = 48.dp)
                ) {
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
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Lucide.FileText,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(Modifier.width(12.dp))
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
                        TextButton(
                            onClick = { navController.navigate(Screen.CreateListing.route) },
                            modifier = Modifier.heightIn(min = 48.dp)
                        ) {
                            Text("Resume", fontWeight = FontWeight.SemiBold)
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
                        TextButton(
                            onClick = { navController.navigate(Screen.FarmerBids.route) },
                            modifier = Modifier.heightIn(min = 40.dp)
                        ) {
                            Icon(Lucide.Gavel, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Optional bidding · Incoming offers")
                        }
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            label = { Text("Search crop or location") },
                            leadingIcon = { Icon(Lucide.Search, contentDescription = null) },
                            singleLine = true,
                            shape = MaterialTheme.shapes.medium,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                                focusedContainerColor = MaterialTheme.colorScheme.surface,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surface
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(8.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            item {
                                FilterChip(
                                    selected = selectedStatus == null,
                                    onClick = { selectedStatus = null },
                                    label = { Text("All") },
                                    modifier = Modifier.heightIn(min = 48.dp)
                                )
                            }
                            items(ListingStatus.entries) { status ->
                                FilterChip(
                                    selected = selectedStatus == status,
                                    onClick = { selectedStatus = status },
                                    label = { Text(status.displayLabel()) },
                                    modifier = Modifier.heightIn(min = 48.dp)
                                )
                            }
                        }
                        Box {
                            TextButton(
                                onClick = { sortMenuExpanded = true },
                                modifier = Modifier.heightIn(min = 48.dp)
                            ) {
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
                                TextButton(
                                    onClick = {
                                        searchQuery = ""
                                        selectedStatus = null
                                    },
                                    modifier = Modifier.heightIn(min = 48.dp)
                                ) {
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
    val isFailed = listing.harvestStatus == HarvestStatus.FAILED
    AppCard(onClick = onCardClick) {
        // ── Information block — visibly muted once the harvest has failed ──
        Column(modifier = Modifier.alpha(if (isFailed) 0.55f else 1f)) {
            ListingImage(imageUrl = listing.imageUrls.firstOrNull(), height = 176.dp)
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = listing.cropName,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                HarvestStatusBadge(listing.harvestStatus)
            }
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ListingStatusChip(listing.status)
                if (listing.status == ListingStatus.OPEN) {
                    BiddingChip(listing.biddingEnabled)
                }
            }
            listing.category?.let { category ->
                Spacer(Modifier.height(8.dp))
                StatusChip(label = "${category.displayName} (farmer-provided)", tone = ChipTone.Secondary)
            }
            when (listing.harvestStatus) {
                HarvestStatus.EXPECTED -> {
                    Spacer(Modifier.height(12.dp))
                    StockValue(
                        label = "Estimated quantity (estimate)",
                        quantity = listing.estimatedAvailable(),
                        unit = listing.quantityUnit.name
                    )
                    Text(
                        text = "Estimates only — the actual quantity is confirmed when you mark the harvest ready.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
                HarvestStatus.FAILED -> {
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = buildString {
                            append("This harvest is unavailable")
                            listing.harvestFailureReason?.takeIf(String::isNotBlank)?.let { append(": $it") }
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                HarvestStatus.READY -> {
                    Spacer(Modifier.height(12.dp))
                    Surface(
                        shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            StockValue("Original", listing.quantity, listing.quantityUnit.name)
                            StockValue("Available", listing.availableStock(), listing.quantityUnit.name)
                            StockValue("Sold / reserved", listing.soldQuantity, listing.quantityUnit.name)
                        }
                    }
                }
            }
            Text(
                text = "Reserve: Rs. ${listing.reservePrice} / ${listing.quantityUnit.name}",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 10.dp)
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
                    modifier = Modifier.padding(top = 4.dp)
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
                    overflow = TextOverflow.Ellipsis,
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
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .padding(start = 8.dp)
                            .weight(1f, fill = false)
                    )
                }
            }
        }

        // ── Harvest actions — only offered where the app already offers them ──
        if (listing.harvestStatus == HarvestStatus.EXPECTED) {
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = onMarkFailedClick,
                    modifier = Modifier.heightIn(min = 48.dp)
                ) {
                    Text("Mark harvest failed", color = MaterialTheme.colorScheme.error)
                }
                Button(
                    onClick = onMarkReadyClick,
                    modifier = Modifier.heightIn(min = 48.dp),
                    shape = MaterialTheme.shapes.large
                ) {
                    Text("Mark ready")
                }
            }
        }

        HorizontalDivider(
            modifier = Modifier.padding(top = 12.dp),
            thickness = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.End),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(
                onClick = onEditClick,
                modifier = Modifier.heightIn(min = 48.dp)
            ) {
                Icon(
                    Lucide.FileText,
                    contentDescription = "Edit listing",
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text("Edit listing")
            }
            TextButton(
                onClick = onCardClick,
                modifier = Modifier.heightIn(min = 48.dp)
            ) {
                Icon(
                    Lucide.FileText,
                    contentDescription = "View listing details",
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text("View listing details")
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp)
                .heightIn(min = 48.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Created ${formatDate(listing.createdAt)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            // A failed harvest can no longer accept bids, so no toggle is shown.
            if (!isFailed) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Optional bidding",
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    if (isBiddingUpdating) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .width(24.dp)
                                .height(24.dp),
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
        }
        if (listing.status == ListingStatus.OPEN) {
            TextButton(
                onClick = onCloseClick,
                modifier = Modifier
                    .align(Alignment.End)
                    .heightIn(min = 48.dp)
            ) {
                Text("Close listing")
            }
        }
    }
}

@Composable
private fun StockValue(label: String, quantity: Double, unit: String) {
    Column {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1
        )
        Text(
            "$quantity $unit",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1
        )
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
    val hasError = quantityText.isNotEmpty() && !isValid
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Lucide.Sprout,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        title = { Text("Mark harvest ready") },
        text = {
            Column {
                Text(
                    text = "Enter the actual quantity available from the “${listing.cropName}” harvest. " +
                        "The listing will sell from this amount, replacing the earlier estimate.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(
                    value = quantityText,
                    onValueChange = {
                        if (it.isEmpty() || it.matches(Regex("^\\d*\\.?\\d*$"))) quantityText = it
                    },
                    label = { Text("Actual quantity") },
                    suffix = {
                        Text(
                            text = listing.quantityUnit.name,
                            style = MaterialTheme.typography.labelLarge,
                            color = if (hasError) MaterialTheme.colorScheme.error
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    supportingText = {
                        Text(
                            text = if (hasError) {
                                "Enter a quantity greater than zero."
                            } else {
                                "Measured in ${listing.quantityUnit.name}."
                            },
                            color = if (hasError) MaterialTheme.colorScheme.error
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    isError = hasError,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                enabled = isValid,
                onClick = { quantity?.let(onConfirm) },
                modifier = Modifier.heightIn(min = 48.dp),
                shape = MaterialTheme.shapes.large
            ) {
                Text("Mark ready")
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier.heightIn(min = 48.dp)
            ) {
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
        icon = {
            Icon(
                imageVector = Lucide.AlertCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error
            )
        },
        title = { Text("Mark harvest as failed?") },
        text = {
            Column {
                Text(
                    text = "Suppliers will see the “$cropName” harvest as unavailable and pending " +
                        "pre-harvest requests will be cancelled. No orders are created and no stock changes.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(16.dp))
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
            Button(
                onClick = { onConfirm(reason.trim().ifBlank { null }) },
                modifier = Modifier.heightIn(min = 48.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError
                ),
                shape = MaterialTheme.shapes.large
            ) {
                Text("Mark failed")
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier.heightIn(min = 48.dp)
            ) {
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
            HarvestStatusBadge(HarvestStatus.EXPECTED)
            HarvestStatusBadge(HarvestStatus.FAILED)
            HarvestStatusBadge(HarvestStatus.READY)
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

private fun previewListing(harvestStatus: HarvestStatus) = HarvestListing(
    listingId = "preview-$harvestStatus",
    farmerId = "farmer-preview",
    farmerName = "Preview Farmer",
    cropName = "Tomatoes — vine ripened, farm grade A",
    description = "Picked daily, cool storage, ready for bulk orders.",
    quantity = 250.0,
    reservePrice = 180.0,
    imageUrls = emptyList(),
    location = "Kandy Central Farm",
    status = ListingStatus.OPEN,
    biddingEnabled = harvestStatus != HarvestStatus.FAILED,
    category = ListingCategory.ORGANIC,
    expectedHarvestAt = 1_796_000_000_000L,
    harvestStatus = harvestStatus,
    estimatedQuantity = 200.0,
    harvestFailureReason = if (harvestStatus == HarvestStatus.FAILED) "Destroyed by late blight" else null,
    harvestUpdatedAt = 1_795_000_000_000L
)

@Preview(name = "Light", showBackground = true, backgroundColor = 0xFFF4FBF6, heightDp = 1600)
@Preview(name = "Dark", showBackground = true, backgroundColor = 0xFF0F1F16, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES, heightDp = 1600)
@Composable
private fun ListingCardsPreview() {
    AgroRouteTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            HarvestStatus.entries.forEach { status ->
                ListingCard(
                    listing = previewListing(status),
                    onEditClick = {},
                    onCardClick = {},
                    onToggleBidding = {},
                    isBiddingUpdating = false,
                    onCloseClick = {},
                    onMarkReadyClick = {},
                    onMarkFailedClick = {}
                )
            }
        }
    }
}
