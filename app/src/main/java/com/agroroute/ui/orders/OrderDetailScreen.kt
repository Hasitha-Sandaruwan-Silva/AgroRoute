package com.agroroute.ui.orders

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavController

/**
 * Screen showing full details of a single [Order] — full implementation to be added.
 */
@Composable
fun OrderDetailScreen(
    orderId: String,
    navController: NavController
) {
    // TODO: Observe OrderViewModel, display order details and status update actions
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text("Order Detail: $orderId — Implementation Pending")
    }
}
