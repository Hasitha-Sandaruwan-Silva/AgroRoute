package com.agroroute.features.farmer.orderrequests

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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
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
import com.agroroute.core.model.OrderRequest
import com.agroroute.core.model.OrderRequestStatus
import com.agroroute.core.widgets.AgroSnackbarHost
import com.agroroute.core.widgets.AppCard
import com.agroroute.core.widgets.EmptyState
import com.agroroute.core.widgets.ErrorSnackbarEffect
import com.agroroute.core.widgets.ListSkeleton
import com.agroroute.core.widgets.LoadingButton
import com.agroroute.core.widgets.OrderRequestStatusChip
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

    LaunchedEffect(Unit) {
        viewModel.loadRequests()
    }
    ErrorSnackbarEffect(uiState.error, snackbarHostState) { viewModel.clearError() }

    val pendingCount = uiState.requests.count { it.status == OrderRequestStatus.PENDING }
    val visibleRequests = uiState.requests
        .filter { it.status == selectedStatus }
        .sortedByDescending { it.createdAt }

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
                    style = MaterialTheme.typography.titleLarge,
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
                            label = { Text("${status.label()} ($count)") }
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
                            isLoading = uiState.isLoading,
                            onAccept = { requestToAccept = request },
                            onReject = { viewModel.rejectRequest(request) }
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
private fun FarmerOrderRequestCard(
    request: OrderRequest,
    listing: HarvestListing?,
    isLoading: Boolean,
    onAccept: () -> Unit,
    onReject: () -> Unit
) {
    AppCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    request.cropName.ifBlank { listing?.cropName.orEmpty() }.ifBlank { "Crop" },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Supplier: ${request.supplierName.ifBlank { "Name unavailable" }}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            OrderRequestStatusChip(request.status)
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Quantity: ${request.requestedQuantity} ${request.quantityUnit.name}",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = "Listed price: Rs. ${request.listedPrice} per ${request.quantityUnit.name}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = "Requested: ${formatRequestDate(request.createdAt)}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (request.status == OrderRequestStatus.PENDING) {
            Row(
                modifier = Modifier.padding(top = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                LoadingButton(
                    text = "Accept",
                    isLoading = isLoading,
                    onClick = onAccept,
                    modifier = Modifier.weight(1f)
                )
                TextButton(onClick = onReject, enabled = !isLoading) {
                    Text("Reject")
                }
            }
        }
    }
}

@Composable
private fun AcceptRequestConfirmationDialog(
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
            TextButton(onClick = onConfirm) {
                Text("Accept and create order")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

private fun OrderRequestStatus.label(): String = name.lowercase().replaceFirstChar { it.uppercase() }

private fun formatRequestDate(timestamp: Long): String =
    SimpleDateFormat("MMM dd, yyyy · hh:mm a", Locale.getDefault()).format(Date(timestamp))
