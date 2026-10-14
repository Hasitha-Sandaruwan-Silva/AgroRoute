package com.agroroute.features.supplier.marketplace

import androidx.compose.ui.focus.focusRequester
import androidx.compose.runtime.LaunchedEffect

import androidx.compose.foundation.background
import androidx.compose.ui.draw.clip
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
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.agroroute.core.runtime.appViewModel as hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.agroroute.core.model.HarvestListing
import com.agroroute.core.model.HarvestStatus
import com.agroroute.core.model.acceptsDirectOrders
import com.agroroute.core.model.acceptsNewBids
import com.agroroute.core.model.availabilityLabel
import com.agroroute.core.model.availableStock
import com.agroroute.core.model.estimatedAvailable
import com.agroroute.core.navigation.Screen
import com.agroroute.core.util.HarvestAvailabilityDate
import com.agroroute.core.components.AgroSnackbarHost
import com.agroroute.features.supplier.components.SupplierCard as AppCard
import com.agroroute.core.components.ChipTone
import com.agroroute.features.supplier.components.SupplierEmptyState as EmptyState
import com.agroroute.core.components.ErrorSnackbarEffect
import com.agroroute.core.components.ListSkeleton
import com.agroroute.features.supplier.components.SupplierListingImage as ListingImage
import com.agroroute.core.components.ProfilePhoneContact
import com.agroroute.core.components.StatusChip
import com.agroroute.core.location.SelectedLocation
import com.github.thelacspace.icons.Lucide
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
    val homeSearchFocus = remember { androidx.compose.ui.focus.FocusRequester() }
    val focusFromHome = remember {
        navController.previousBackStackEntry?.savedStateHandle?.remove<Boolean>("supplier_home_search") == true
    }
    LaunchedEffect(focusFromHome) {
        if (focusFromHome) homeSearchFocus.requestFocus()
    }
    var filter by remember { mutableStateOf(MarketFilter.ALL) }

    ErrorSnackbarEffect(uiState.error, snackbarHostState) { /* flows from VM */ }

    val filtered = uiState.listings.filter { listing ->
        if (!isSupplierMarketplaceEligible(listing)) return@filter false
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

    val hasActiveFilters = searchQuery.isNotBlank() || filter != MarketFilter.ALL

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp)) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search crops or location", style = MaterialTheme.typography.bodyMedium) },
                leadingIcon = { Icon(Lucide.Search, contentDescription = null, modifier = Modifier.size(20.dp)) },
                textStyle = MaterialTheme.typography.bodyMedium,
                singleLine = true,
                shape = RoundedCornerShape(28.dp),
                colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = MaterialTheme.colorScheme.surface, unfocusedContainerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp).then(
                    Modifier.focusRequester(homeSearchFocus)
                )
            )
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                item {
                    FilterChip(
                        selected = filter == MarketFilter.ALL,
                        onClick = { filter = MarketFilter.ALL },
                        label = { Text("All", style = MaterialTheme.typography.labelSmall) },
                        shape = RoundedCornerShape(22.dp),
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = MaterialTheme.colorScheme.primary, selectedLabelColor = MaterialTheme.colorScheme.onPrimary)
                    )
                }
                item {
                    FilterChip(
                        selected = filter == MarketFilter.BIDDING_OPEN,
                        onClick = { filter = MarketFilter.BIDDING_OPEN },
                        label = { Text("Bidding open", style = MaterialTheme.typography.labelSmall) },
                        shape = RoundedCornerShape(22.dp),
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = MaterialTheme.colorScheme.primary, selectedLabelColor = MaterialTheme.colorScheme.onPrimary)
                    )
                }
                item {
                    FilterChip(
                        selected = filter == MarketFilter.NEAR_ME,
                        onClick = { filter = MarketFilter.NEAR_ME },
                        label = { Text("With location", style = MaterialTheme.typography.labelSmall) },
                        shape = RoundedCornerShape(22.dp),
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = MaterialTheme.colorScheme.primary, selectedLabelColor = MaterialTheme.colorScheme.onPrimary)
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (filtered.size == 1) "1 listing" else "${filtered.size} listings",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (filter == MarketFilter.BIDDING_OPEN) {
                        Text("Bidding open", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    }
                }
                TextButton(
                    onClick = { navController.navigate(Screen.SupplierBids.route) },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                ) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Optional bidding", style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("My bids >", style = MaterialTheme.typography.labelMedium)
                    }
                }
                IconButton(onClick = { viewModel.refresh() }) {
                    Icon(Lucide.RefreshCw, contentDescription = "Refresh", modifier = Modifier.size(20.dp))
                }
            }
        }

        Box(modifier = Modifier.fillMaxSize()) {
            when {
                uiState.isLoading && uiState.listings.isEmpty() -> ListSkeleton()
                uiState.error != null && uiState.listings.isEmpty() -> Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    EmptyState(
                        icon = Lucide.Search,
                        title = "Marketplace unavailable",
                        subtitle = "Check your connection and try again"
                    )
                }
                filtered.isEmpty() -> Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        EmptyState(
                            icon = Lucide.Search,
                            title = if (hasActiveFilters) "No matching listings" else "No active listings",
                            subtitle = if (hasActiveFilters) "Try another search or filter" else "New harvest listings will appear here"
                        )
                        if (hasActiveFilters) {
                            TextButton(onClick = {
                                searchQuery = ""
                                filter = MarketFilter.ALL
                            }) {
                                Text("Clear search and filters")
                            }
                        }
                    }
                }
                else -> LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filtered, key = { it.listingId }) { listing ->
                        MarketplaceListingCard(
                            listing = listing,
                            canPlaceBid = listing.acceptsNewBids(),
                            canRequestOrder = listing.acceptsDirectOrders(),
                            onCardClick = {
                                if (listing.acceptsNewBids()) {
                                    navController.navigate(Screen.PlaceBid.createRoute(listing.listingId))
                                } else if (listing.acceptsDirectOrders()) {
                                    navController.navigate(Screen.PlaceOrderRequest.createRoute(listing.listingId))
                                }
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

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
internal fun MarketplaceListingCard(
    listing: HarvestListing,
    canPlaceBid: Boolean,
    canRequestOrder: Boolean,
    onCardClick: () -> Unit
) {
    val isExpected = listing.harvestStatus == HarvestStatus.EXPECTED
    val pickup = remember(listing.location) { SelectedLocation.fromStoredText(listing.location) }
    AppCard(onClick = onCardClick.takeIf { canPlaceBid || canRequestOrder }, contentPadding = PaddingValues(0.dp)) {
        Box {
            ListingImage(imageUrl = listing.imageUrls.firstOrNull(), height = 180.dp)
            Box(Modifier.padding(12.dp)) {
                when {
                    canPlaceBid -> StatusChip(label = "Bidding open", tone = ChipTone.Success)
                    canRequestOrder -> StatusChip(label = "Fixed price", tone = ChipTone.Tertiary)
                    else -> StatusChip(label = listing.availabilityLabel(), tone = ChipTone.Neutral)
                }
            }
        }
        Column(Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = listing.cropName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(8.dp))

            }
            // Farmer + location metadata row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Lucide.User,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(Modifier.width(5.dp))
                Text(
                    text = "by ${listing.farmerName}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                if (listing.location.isNotBlank()) {
                    Spacer(Modifier.width(10.dp))
                    Icon(
                        imageVector = Lucide.MapPin,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = "Location: ${listing.location}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            ProfilePhoneContact(userId = listing.farmerId, personLabel = "Farmer")
            Spacer(Modifier.height(10.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (isExpected) {
                    StatusChip(label = "Expected harvest", tone = ChipTone.Tertiary)
                }
                listing.category?.let { category ->
                    StatusChip(label = category.displayName, tone = ChipTone.Secondary)
                }
                listing.qualityGrade?.takeIf(String::isNotBlank)?.let { grade ->
                    StatusChip(label = "Quality: $grade", tone = ChipTone.Tertiary)
                }
            }
            listing.expectedHarvestAt?.let { harvestAt ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    Icon(
                        imageVector = Lucide.CalendarDays,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.width(5.dp))
                    Text(
                        text = if (isExpected) {
                            "Expected harvest: ${HarvestAvailabilityDate.format(harvestAt)} (estimate)"
                        } else {
                            "Harvest: ${HarvestAvailabilityDate.label(harvestAt)}"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)).padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isExpected) "Estimated quantity" else "Available",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = if (isExpected) {
                            "${listing.estimatedAvailable()} ${listing.quantityUnit.name}"
                        } else {
                            "${listing.availableStock()} ${listing.quantityUnit.name}"
                        },
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text("Reserve price", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        "Rs. ${listing.reservePrice}/${listing.quantityUnit.name}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 2
                    )
                }
            }
            if (listing.description.isNotBlank()) {
                Text(
                    text = listing.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
            Text(
                text = "Posted ${formatDate(listing.createdAt)}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp)
            )
            pickup?.let { location ->
                MarketplaceLocationPreview(location = location, modifier = Modifier.padding(top = 10.dp))
            }
            if (canPlaceBid) {
                Spacer(Modifier.height(12.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = onCardClick,
                    shape = MaterialTheme.shapes.large,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                ) {
                    Icon(Lucide.Gavel, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("View / Bid", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                }
            } else if (canRequestOrder) {
                Spacer(Modifier.height(12.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = onCardClick,
                    shape = MaterialTheme.shapes.large,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                ) {
                    Text("View / Request", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

private fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
