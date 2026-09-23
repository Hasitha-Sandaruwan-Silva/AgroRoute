package com.agroroute.ui.orders

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavController

/**
 * Screen showing the current status and timeline of an [Order] — full implementation to be added.
 */
@Composable
fun OrderStatusScreen(
    navController: NavController
) {
    // TODO: Show order status timeline stepper (Awaiting Pickup → In Transit → Delivered)
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text("Order Status — Implementation Pending")
    }
}
