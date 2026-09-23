package com.agroroute.ui.supplier.marketplace

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavController

/**
 * Screen showing all open [HarvestListing]s available for bidding — full implementation to be added.
 */
@Composable
fun BrowseListingsScreen(
    navController: NavController
) {
    // TODO: Observe MarketplaceViewModel.uiState, render listing cards with Place Bid navigation
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text("Browse Marketplace — Implementation Pending")
    }
}
