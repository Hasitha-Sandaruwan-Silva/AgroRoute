package com.agroroute.ui.farmer.listing

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavController

/**
 * Screen showing all listings created by the currently signed-in farmer — full implementation to be added.
 */
@Composable
fun MyListingsScreen(
    navController: NavController
) {
    // TODO: Observe ListingViewModel.uiState, show list with toggle bidding and navigate-to-bids actions
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text("My Listings — Implementation Pending")
    }
}
