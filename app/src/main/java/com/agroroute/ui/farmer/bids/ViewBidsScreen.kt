package com.agroroute.ui.farmer.bids

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavController

/**
 * Screen showing all bids received for a specific listing — full implementation to be added.
 */
@Composable
fun ViewBidsScreen(
    listingId: String,
    navController: NavController
) {
    // TODO: Observe BidsViewModel, show bid cards with Accept/Reject actions
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text("View Bids for $listingId — Implementation Pending")
    }
}
