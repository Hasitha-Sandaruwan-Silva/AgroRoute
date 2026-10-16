package com.agroroute.features.farmer.home

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.agroroute.core.components.*
import com.agroroute.core.model.*
import com.agroroute.features.farmer.orderrequests.FarmerOrderRequestsUiState
import com.github.thelacspace.icons.Lucide
import java.util.Locale

/** Presentation only: acceptance is always revalidated by the existing use case/transaction. */
internal fun pendingHomeRequests(state: FarmerOrderRequestsUiState, farmerId: String, now: Long): List<OrderRequest> =
    state.requests.filter { request ->
        val listing = state.listingsById[request.listingId]
        request.status == OrderRequestStatus.PENDING && request.farmerId == farmerId &&
            listing != null && listing.farmerId == farmerId && !listing.isDraft && !listing.isExpired(now) &&
            listing.status == ListingStatus.OPEN && listing.harvestStatus != HarvestStatus.FAILED
    }.sortedWith(compareByDescending<OrderRequest> { it.createdAt }.thenBy { it.requestId })

@Composable
internal fun IncomingOrderRequests(requests: List<OrderRequest>, listings: Map<String, HarvestListing>,
    busy: Boolean, onView: () -> Unit, onAccept: (OrderRequest) -> Unit) {
    if (requests.isEmpty()) return
    val mutationAllowed = com.agroroute.core.runtime.LocalMutationAllowed.current
    AppCard(containerColor = MaterialTheme.colorScheme.surface) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Lucide.Inbox, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text("Incoming Order Requests", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("${requests.size} pending", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            }
            TextButton(onClick = onView) { Text("View All") }
        }
        requests.take(3).forEachIndexed { index, request ->
            if (index > 0) HorizontalDivider(Modifier.padding(vertical = 8.dp))
            val listing = listings[request.listingId]
            val blocked = listingAcceptanceBlockedReason(listing, false, request.requestedQuantity, request.deliveryLocation)
            Row(Modifier.fillMaxWidth().padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                UserProfileAvatar(request.supplierName.ifBlank { "Supplier" }, null, size = 40.dp)
                Column(Modifier.weight(1f)) {
                    Text(request.supplierName.ifBlank { "Supplier" }, style = MaterialTheme.typography.titleSmall,
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(request.cropName.ifBlank { listing?.cropName.orEmpty() }, style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("${request.requestedQuantity} ${request.quantityUnit.name} | Rs. ${String.format(Locale.getDefault(), "%,.2f", request.requestedQuantity * request.listedPrice)}",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            if (blocked != null) Text(blocked, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onView, modifier = Modifier.weight(1f)) { Text("View") }
                Button(onClick = { onAccept(request) }, enabled = !busy && mutationAllowed && blocked == null,
                    modifier = Modifier.weight(1f)) { Text("Accept") }
            }
        }
    }
}
