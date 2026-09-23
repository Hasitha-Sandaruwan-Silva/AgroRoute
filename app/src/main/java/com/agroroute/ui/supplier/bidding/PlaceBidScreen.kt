package com.agroroute.ui.supplier.bidding

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavController

/**
 * Screen for a supplier to place a bid on a [HarvestListing] — full implementation to be added.
 */
@Composable
fun PlaceBidScreen(
    listingId: String,
    navController: NavController
) {
    // TODO: Show listing summary, PriceInputField, message field, and submit via BiddingViewModel
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text("Place Bid for $listingId — Implementation Pending")
    }
}
