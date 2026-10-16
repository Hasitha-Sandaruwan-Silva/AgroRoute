package com.agroroute.features.farmer.listing

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.agroroute.core.runtime.appViewModel as hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.agroroute.core.location.SelectedLocation
import com.agroroute.core.model.*
import com.agroroute.core.navigation.Screen
import com.agroroute.core.theme.ForestDeep
import com.agroroute.core.components.*
import com.agroroute.core.util.HarvestAvailabilityDate
import com.github.thelacspace.icons.Lucide
import java.text.SimpleDateFormat
import java.util.*

internal fun farmerListingDetailsRoute(listingId: String): String = Screen.ViewBids.createRoute(listingId)
private enum class ListingSort(val label: String) { NEWEST("Newest"), OLDEST("Oldest"), PRICE_LOW_TO_HIGH("Low Price"), PRICE_HIGH_TO_LOW("High Price") }

@Composable
fun MyListingsScreen(navController: NavController, viewModel: ListingViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var searchQuery by remember { mutableStateOf("") }
    var selectedStatus by remember { mutableStateOf<ListingStatus?>(null) }
    var selectedSort by remember { mutableStateOf(ListingSort.NEWEST) }
    var sortMenuExpanded by remember { mutableStateOf(false) }
    var listingPendingClosure by remember { mutableStateOf<HarvestListing?>(null) }
    var listingPendingReady by remember { mutableStateOf<HarvestListing?>(null) }
    var listingPickupRepair by remember { mutableStateOf<HarvestListing?>(null) }
    var listingPendingFailure by remember { mutableStateOf<HarvestListing?>(null) }
    val keyboardVisible = WindowInsets.ime.getBottom(androidx.compose.ui.platform.LocalDensity.current) > 0

    listingPickupRepair?.let { listing ->
        LocationPickerDialog(listing.location, onDismiss = { listingPickupRepair = null }, onConfirm = {
            viewModel.updateListing(listing.copy(location = it))
            listingPickupRepair = null
        })
    }
    val publishedListings = remember(uiState.listings) { uiState.listings.filterNot { it.isDraft } }
    val visibleListings = remember(publishedListings, searchQuery, selectedStatus, selectedSort) {
        publishedListings.filter { l ->
            (searchQuery.isBlank() || l.cropName.contains(searchQuery, true) || l.location.contains(searchQuery, true)) && (selectedStatus == null || l.status == selectedStatus)
        }.let {
            when (selectedSort) {
                ListingSort.NEWEST -> it.sortedByDescending { it.createdAt }
                ListingSort.OLDEST -> it.sortedBy { it.createdAt }
                ListingSort.PRICE_LOW_TO_HIGH -> it.sortedBy { it.reservePrice }
                ListingSort.PRICE_HIGH_TO_LOW -> it.sortedByDescending { it.reservePrice }
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.loadFarmerListings(viewModel.currentFarmerId)
        viewModel.loadFarmerDrafts(viewModel.currentFarmerId)
    }

    ErrorSnackbarEffect(uiState.error, snackbarHostState) { viewModel.clearError() }

    listingPendingClosure?.let { l ->
        AlertDialog(
            onDismissRequest = { listingPendingClosure = null },
            title = { Text("Close listing?", fontWeight = FontWeight.Bold) },
            text = { Text("Closing removes this listing from the active marketplace while preserving bids, requests, orders and reviews. Permanent deletion is unavailable because history must remain valid.") },
            confirmButton = {
                Button(onClick = { listingPendingClosure = null; viewModel.closeListing(l) }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)) {
                    Text("Close")
                }
            },
            dismissButton = { TextButton(onClick = { listingPendingClosure = null }) { Text("Cancel") } }
        )
    }
    listingPendingReady?.let { l -> MarkHarvestReadyDialog(listing = l, onDismiss = { listingPendingReady = null }, onConfirm = { q -> listingPendingReady = null; viewModel.markHarvestReady(l.listingId, q) }) }
    listingPendingFailure?.let { l -> MarkHarvestFailedDialog(cropName = l.cropName, onDismiss = { listingPendingFailure = null }, onConfirm = { r -> listingPendingFailure = null; viewModel.markHarvestFailed(l.listingId, r) }) }

    // SYSTEM THEME AWARE - uses MaterialTheme.colorScheme which auto switches Light/Dark via AgroRouteTheme
    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Scaffold(
            floatingActionButton = {
                if (!keyboardVisible) ExtendedFloatingActionButton(
                    onClick = { navController.navigate(Screen.CreateListing.route) },
                    icon = { Icon(Lucide.Plus, contentDescription = null) },
                    text = { Text("New Listing", fontWeight = FontWeight.Bold) },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.shadow(2.dp, RoundedCornerShape(16.dp))
                )
            },
            snackbarHost = { AgroSnackbarHost(snackbarHostState) },
            containerColor = Color.Transparent
        ) { paddingValues ->
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(bottom = paddingValues.calculateBottomPadding()),
                contentPadding = PaddingValues(bottom = 20.dp),
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                // COMPACT HEADER - THEME AWARE - NO GAP - 22sp
                item {
                    Surface(modifier = Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.surface, shadowElevation = 0.dp) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text("My Harvests", fontWeight = FontWeight.Black, fontSize = 22.sp, color = MaterialTheme.colorScheme.onSurface)
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(Color(0xFF4CAF50)))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            "${visibleListings.size} of ${publishedListings.size} • Live",
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Box(
                                        modifier = Modifier.size(40.dp).clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.primaryContainer)
                                            .clickable { sortMenuExpanded = true },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Lucide.Settings, contentDescription = "Sort listings", tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(18.dp))
                                    }
                                    Box(
                                        modifier = Modifier.size(40.dp).clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.primary)
                                            .clickable { navController.navigate(Screen.FarmerBids.route) },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Lucide.Gavel, contentDescription = "View bids", tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }

                            DropdownMenu(expanded = sortMenuExpanded, onDismissRequest = { sortMenuExpanded = false }) {
                                ListingSort.entries.forEach { sort ->
                                    DropdownMenuItem(text = { Text(sort.label) }, onClick = { selectedSort = sort; sortMenuExpanded = false })
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                placeholder = { Text("Search crop or location...", fontSize = 14.sp) },
                                leadingIcon = { Icon(Lucide.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
                                trailingIcon = {
                                    if (searchQuery.isNotEmpty()) {
                                        IconButton(onClick = { searchQuery = "" }) {
                                            Icon(Lucide.X, contentDescription = null, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                },
                                singleLine = true,
                                shape = RoundedCornerShape(28.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                ),
                                modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)
                            )

                            Spacer(modifier = Modifier.height(12.dp))
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                item {
                                    FilterChip(
                                        selected = selectedStatus == null,
                                        onClick = { selectedStatus = null },
                                        label = { Text("All", fontWeight = FontWeight.Bold) },
                                        shape = RoundedCornerShape(50.dp),
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                        )
                                    )
                                }
                                items(ListingStatus.entries) { status ->
                                    FilterChip(
                                        selected = selectedStatus == status,
                                        onClick = { selectedStatus = status },
                                        label = { Text(status.displayLabel()) },
                                        shape = RoundedCornerShape(50.dp),
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                        )
                                    )
                                }
                            }
                        }
                    }
                }

                if (uiState.drafts.isNotEmpty()) {
                    item {
                        Surface(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            shadowElevation = 2.dp
                        ) {
                            Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) { Icon(Lucide.FileText, contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSecondaryContainer) }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("${uiState.drafts.size} drafts", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSecondaryContainer)
                                    Text("Resume editing", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f))
                                }
                                FilledTonalButton(onClick = { navController.navigate(Screen.CreateListing.route) }, shape = RoundedCornerShape(10.dp)) {
                                    Text("Open")
                                }
                            }
                        }
                    }
                }

                when {
                    uiState.isLoading && publishedListings.isEmpty() -> item { Box(Modifier.fillMaxWidth().padding(top = 40.dp)) { ListSkeleton() } }
                    publishedListings.isEmpty() -> item {
                        Box(Modifier.fillParentMaxSize().padding(top = 100.dp), contentAlignment = Alignment.Center) {
                            EmptyState(icon = Lucide.Plus, title = "No Listings Yet", subtitle = "Create your first harvest listing to start selling")
                        }
                    }
                    visibleListings.isEmpty() -> item {
                        Box(Modifier.fillParentMaxSize().padding(top = 80.dp), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                EmptyState(icon = Lucide.Search, title = "No matching listings", subtitle = "Try another crop, location, or status")
                                Spacer(Modifier.height(8.dp))
                                Button(onClick = { searchQuery = ""; selectedStatus = null }, shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)) {
                                    Text("Clear filters")
                                }
                            }
                        }
                    }
                    else -> {
                        items(visibleListings, key = { it.listingId }) { listing ->
                            Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                                SuperbListingCard(
                                    listing = listing,
                                    onEditClick = {
                                        if (SelectedLocation.fromStoredText(listing.location) == null && listing.status == ListingStatus.OPEN && !listing.isExpired())
                                            listingPickupRepair = listing
                                        else navController.navigate(Screen.EditListing.createRoute(listing.listingId))
                                    },
                                    onCardClick = { navController.navigate(farmerListingDetailsRoute(listing.listingId)) },
                                    onToggleBidding = { viewModel.toggleBidding(listing.listingId, it) },
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
private fun SuperbListingCard(
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
    Surface(
        modifier = Modifier.fillMaxWidth().shadow(2.dp, RoundedCornerShape(22.dp)),
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surface,
        onClick = onCardClick
    ) {
        Column {
            Box(modifier = Modifier.fillMaxWidth().height(150.dp).clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))) {
                ListingImage(imageUrl = listing.imageUrls.firstOrNull(), height = 150.dp)
                Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.05f), Color.Black.copy(alpha = 0.45f)))))
                Row(modifier = Modifier.align(Alignment.TopStart).padding(12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (listing.isExpired( com.agroroute.core.components.listingPresentationTime())) {
                        com.agroroute.core.components.StatusChip(label = "Expired", tone = com.agroroute.core.components.ChipTone.Neutral)
                    } else if (listing.status == ListingStatus.FULFILLED) {
                        com.agroroute.core.components.StatusChip(label = "Sold Out", tone = com.agroroute.core.components.ChipTone.Neutral)
                    } else ListingStatusChip(listing.status)
                    if (listing.status == ListingStatus.OPEN) BiddingChip(listing.biddingEnabled)
                }
                Box(modifier = Modifier.align(Alignment.TopEnd).padding(12.dp)) { HarvestStatusBadge(listing.harvestStatus) }
                Surface(
                    modifier = Modifier.align(Alignment.BottomStart).padding(12.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = Color.Black.copy(alpha = 0.6f)
                ) {
                    Text(
                        text = "Rs. ${listing.reservePrice} / ${listing.quantityUnit.name}",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            }
            Column(modifier = Modifier.padding(14.dp).alpha(if (isFailed) 0.6f else 1f)) {
                Text(
                    text = listing.cropName,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold, fontSize = 17.sp, color = MaterialTheme.colorScheme.onSurface),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Lucide.MapPin, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = com.agroroute.core.components.profileLocationLabel(listing.location), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Spacer(modifier = Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text("Available", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("${listing.availableStock()} ${listing.quantityUnit.name}", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Category", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(listing.category?.displayName ?: "-", fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                    }
                }
                Text("Posted ${remember(listing.createdAt) { SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(listing.createdAt)) }}",
                    style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp))
                if (listing.status == ListingStatus.OPEN) {
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text("Allow bidding", modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                        if (isBiddingUpdating) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                        Switch(checked = listing.biddingEnabled, onCheckedChange = onToggleBidding, enabled = com.agroroute.core.runtime.LocalMutationAllowed.current && !isBiddingUpdating && !isFailed && !listing.isDraft &&
                            (listing.harvestStatus == HarvestStatus.EXPECTED || listing.availableStock() > 0))
                    }
                }
                Spacer(Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(enabled = com.agroroute.core.runtime.LocalMutationAllowed.current, onClick = onEditClick, modifier = Modifier.weight(1f)) {
                        Icon(Lucide.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(if (SelectedLocation.fromStoredText(listing.location) == null) "Add pickup pin" else "Edit listing")
                    }
                    if (listing.status == ListingStatus.OPEN) {
                        TextButton(onClick = onCloseClick, enabled = com.agroroute.core.runtime.LocalMutationAllowed.current) { Text("Remove", color = if (com.agroroute.core.runtime.LocalMutationAllowed.current) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)) }
                    }
                }
                if (listing.harvestStatus == HarvestStatus.EXPECTED) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            TextButton(onClick = onMarkFailedClick, enabled = com.agroroute.core.runtime.LocalMutationAllowed.current) { Text("Failed", color = MaterialTheme.colorScheme.error, fontSize = 13.sp) }
                            Button(
                                enabled = com.agroroute.core.runtime.LocalMutationAllowed.current, onClick = onMarkReadyClick,
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                modifier = Modifier.height(36.dp)
                            ) { Text("Ready", fontSize = 13.sp) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MarkHarvestReadyDialog(listing: HarvestListing, onDismiss: () -> Unit, onConfirm: (Double) -> Unit) {
    var quantityText by remember { mutableStateOf(listing.estimatedAvailable().takeIf { it > 0.0 }?.let { if (it % 1.0 == 0.0) it.toLong().toString() else it.toString() }.orEmpty()) }
    val quantity = quantityText.toDoubleOrNull()
    val isValid = quantity != null && quantity > 0.0
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Lucide.Sprout, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
        title = { Text("Mark harvest ready") },
        text = {
            Column {
                Text("Enter actual quantity for ${listing.cropName}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = quantityText,
                    onValueChange = { if (it.isEmpty() || it.matches(Regex("^\\d*\\.?\\d*$"))) quantityText = it },
                    label = { Text("Actual quantity") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = { Button(enabled = isValid, onClick = { quantity?.let(onConfirm) }, shape = RoundedCornerShape(10.dp)) { Text("Mark ready") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun MarkHarvestFailedDialog(cropName: String, onDismiss: () -> Unit, onConfirm: (String?) -> Unit) {
    var reason by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Lucide.AlertCircle, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
        title = { Text("Mark harvest as failed?") },
        text = {
            OutlinedTextField(
                value = reason,
                onValueChange = { reason = it },
                label = { Text("Reason (optional)") },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(onClick = { onConfirm(reason.trim().ifBlank { null }) }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) {
                Text("Failed")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

private fun ListingStatus.displayLabel(): String = when (this) {
    ListingStatus.OPEN -> "Open"
    ListingStatus.CLOSED -> "Closed"
    ListingStatus.FULFILLED -> "Fulfilled"
}
