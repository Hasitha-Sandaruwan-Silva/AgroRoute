package com.agroroute.features.farmer.orderrequests

import androidx.compose.foundation.clickable
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
import com.github.thelacspace.icons.Lucide
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.unit.dp
import com.agroroute.core.runtime.appViewModel as hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.agroroute.core.model.HarvestListing
import com.agroroute.core.model.HarvestStatus
import com.agroroute.core.model.OrderRequest
import com.agroroute.core.model.OrderRequestStatus
import com.agroroute.core.components.AgroSnackbarHost
import com.agroroute.core.components.AppCard
import com.agroroute.core.components.ChipTone
import com.agroroute.core.components.EmptyState
import com.agroroute.core.components.ErrorSnackbarEffect
import com.agroroute.core.components.ListSkeleton
import com.agroroute.core.components.LoadingButton
import com.agroroute.core.components.OrderRequestStatusChip
import com.agroroute.core.components.StatusChip
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val farmerRequestStatuses = listOf(
    OrderRequestStatus.PENDING,
    OrderRequestStatus.ACCEPTED,
    OrderRequestStatus.REJECTED,
    OrderRequestStatus.CANCELLED
)

@Composable
fun FarmerOrderRequestsScreen(
    navController: NavController,
    viewModel: FarmerOrderRequestsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var selectedStatus by remember { mutableStateOf(OrderRequestStatus.PENDING) }
    var requestToAccept by remember { mutableStateOf<OrderRequest?>(null) }

    androidx.lifecycle.compose.LifecycleResumeEffect(Unit) {
        viewModel.loadRequests()
        onPauseOrDispose { viewModel.stopObservingRequests() }
    }
    ErrorSnackbarEffect(uiState.error, snackbarHostState) { viewModel.clearError() }

    val pendingCount = remember(uiState.requests) { uiState.requests.count { it.status == OrderRequestStatus.PENDING } }
    val visibleRequests = remember(uiState.requests, selectedStatus) {
        uiState.requests.filter { it.status == selectedStatus }.sortedByDescending { it.createdAt }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Order requests",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "$pendingCount pending ${if (pendingCount == 1) "request" else "requests"}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(farmerRequestStatuses) { status ->
                        val count = uiState.requests.count { it.status == status }
                        FilterChip(
                            selected = selectedStatus == status,
                            onClick = { selectedStatus = status },
                            label = { Text("${status.label()} ($count)") },
                            modifier = Modifier.heightIn(min = 48.dp)
                        )
                    }
                }
            }

            when {
                uiState.isLoading && uiState.requests.isEmpty() -> ListSkeleton()
                uiState.listingsById.isEmpty() && uiState.requests.isEmpty() -> EmptyState(
                    icon = com.github.thelacspace.icons.Lucide.ShoppingCart,
                    title = "No listings yet",
                    subtitle = "Direct order requests from Suppliers will appear here when they request your fixed-price listings"
                )
                visibleRequests.isEmpty() -> EmptyState(
                    icon = com.github.thelacspace.icons.Lucide.ShoppingCart,
                    title = when (selectedStatus) {
                        OrderRequestStatus.PENDING -> "No pending requests"
                        OrderRequestStatus.ACCEPTED -> "No accepted requests"
                        OrderRequestStatus.REJECTED -> "No rejected requests"
                        OrderRequestStatus.CANCELLED -> "No cancelled requests"
                    },
                    subtitle = if (selectedStatus == OrderRequestStatus.PENDING) {
                        "There are no direct order requests waiting for your decision."
                    } else {
                        "Order requests in this section will appear here."
                    }
                )
                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(visibleRequests, key = { it.requestId }) { request ->
                        val listing = uiState.listingsById[request.listingId]
                        FarmerOrderRequestCard(
                            request = request,
                            listing = listing,
                            isLoading = uiState.isLoading || uiState.isSubmitting,
                            onProfile = { navController.navigate(com.agroroute.features.participants.ParticipantRoutes.profile(request.supplierId)) },
                            onAccept = { requestToAccept = request },
                            onReject = { viewModel.rejectRequest(request) },
                            onOpenOrder = { navController.navigate(com.agroroute.core.navigation.Screen.FarmerOrderDetail.createRoute(request.requestId)) }
                        )
                    }
                }
            }
        }

        requestToAccept?.let { request ->
            AcceptRequestConfirmationDialog(
                request = request,
                listing = uiState.listingsById[request.listingId],
                onDismiss = { requestToAccept = null },
                onConfirm = {
                    requestToAccept = null
                    viewModel.acceptRequest(request)
                }
            )
        }

        AgroSnackbarHost(
            snackbarHostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@Composable
internal fun FarmerOrderRequestCard(
    request: OrderRequest,
    listing: HarvestListing?,
    isLoading: Boolean,
    onAccept: () -> Unit,
    onReject: () -> Unit,
    onOpenOrder: () -> Unit = {},
    onProfile: () -> Unit = {}
) {
    // The accept flow blocks requests whose harvest is not READY yet, so the
    // button is shown disabled with a short hint instead of failing on tap.
    val harvestStatus = listing?.harvestStatus
    val acceptBlockedReason = com.agroroute.core.components.listingAcceptanceBlockedReason(
        listing, forBid = false, quantity = request.requestedQuantity, deliveryLocation = request.deliveryLocation)
    val isPreHarvest = request.isPreHarvest || harvestStatus == HarvestStatus.EXPECTED

    AppCard {
        listing?.let {
            com.agroroute.core.components.ListingImage(it.imageUrls.firstOrNull(), height = 96.dp)
            Spacer(Modifier.height(12.dp))
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    request.cropName.ifBlank { listing?.cropName.orEmpty() }.ifBlank { "Crop" },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )

            }
            Spacer(Modifier.width(8.dp))
            OrderRequestStatusChip(request.status)
        }
        com.agroroute.features.participants.ParticipantIdentityRow(request.supplierName, "Supplier", onProfile)
        if (isPreHarvest) {
            Spacer(Modifier.height(8.dp))
            StatusChip(label = "Pre-harvest", tone = ChipTone.Tertiary)
        }
        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = "Quantity",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${request.requestedQuantity} ${request.quantityUnit.name}",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                Text(
                    text = "Listed price",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Rs. ${request.listedPrice} / ${request.quantityUnit.name}",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
        HorizontalDivider(
            modifier = Modifier.padding(vertical = 12.dp),
            thickness = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        )
        if (request.deliveryLocation.isNotBlank()) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Lucide.MapPin, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                Text(com.agroroute.core.components.profileLocationLabel(request.deliveryLocation),
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f))
            }
            Spacer(Modifier.height(8.dp))
        }
        Text(
            text = "Requested: ${formatRequestDate(request.createdAt)}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (request.status == OrderRequestStatus.ACCEPTED) {
            TextButton(onClick = onOpenOrder) { Text("View order") }
        }
        if (request.status == OrderRequestStatus.PENDING) {
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                LoadingButton(
                    text = "Accept",
                    isLoading = isLoading,
                    onClick = onAccept,
                    enabled = com.agroroute.core.runtime.LocalMutationAllowed.current && (acceptBlockedReason == null),
                    modifier = Modifier.weight(1f)
                )
                TextButton(
                    onClick = onReject,
                    enabled = com.agroroute.core.runtime.LocalMutationAllowed.current && (!isLoading && listing != null && !listing.isDraft),
                    modifier = Modifier.heightIn(min = 48.dp)
                ) {
                    Text(
                        text = "Reject",
                        color = if (!com.agroroute.core.runtime.LocalMutationAllowed.current || isLoading) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                        else MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
            if (acceptBlockedReason != null) {
                Row(
                    modifier = Modifier.padding(top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Lucide.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = acceptBlockedReason,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
internal fun AcceptRequestConfirmationDialog(
    request: OrderRequest,
    listing: HarvestListing?,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Accept this order request?") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Supplier: ${request.supplierName.ifBlank { "Name unavailable" }}")
                Text("Crop: ${request.cropName.ifBlank { listing?.cropName.orEmpty() }}")
                Text("Quantity: ${request.requestedQuantity} ${request.quantityUnit.name}")
                Text("Price: Rs. ${request.listedPrice} per ${request.quantityUnit.name}")
            }
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                enabled = com.agroroute.core.runtime.LocalMutationAllowed.current && com.agroroute.core.components.listingAcceptanceBlockedReason(listing, false, request.requestedQuantity, request.deliveryLocation) == null,
                modifier = Modifier.heightIn(min = 48.dp)
            ) {
                Text("Accept and create order")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.heightIn(min = 48.dp)
            ) {
                Text("Cancel")
            }
        }
    )
}

private fun OrderRequestStatus.label(): String = name.lowercase().replaceFirstChar { it.uppercase() }

private fun formatRequestDate(timestamp: Long): String =
    SimpleDateFormat("MMM dd, yyyy · hh:mm a", Locale.getDefault()).format(Date(timestamp))
