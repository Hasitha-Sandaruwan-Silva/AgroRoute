package com.agroroute.ui.farmer

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.agroroute.ui.navigation.Screen

/**
 * Farmer Home Dashboard — central hub for farmers to manage listings, create new harvest listings, and view orders.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FarmerHomeScreen(
    navController: NavController
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("🌾 Farmer Dashboard") },
                actions = {
                    IconButton(
                        onClick = {
                            navController.navigate(Screen.Login.route) {
                                popUpTo(0) { inclusive = true }
                            }
                        }
                    ) {
                        Icon(Icons.Default.ExitToApp, contentDescription = "Sign Out")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Welcome, Farmer!",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "Manage your harvest listings, check incoming bids, and monitor order deliveries.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(16.dp))

            // ── My Listings Card ───────────────────────────────────────────────
            DashboardCard(
                title = "My Harvest Listings",
                description = "View, edit, or toggle bidding mode on your active listings.",
                icon = Icons.Default.List,
                onClick = { navController.navigate(Screen.MyListings.route) }
            )

            // ── Create Listing Card ────────────────────────────────────────────
            DashboardCard(
                title = "Create New Listing",
                description = "List a new harvest crop with quantity, price, and location.",
                icon = Icons.Default.Add,
                onClick = { navController.navigate(Screen.CreateListing.route) }
            )

            // ── Order Status Card ──────────────────────────────────────────────
            DashboardCard(
                title = "Order Status & Delivery",
                description = "Track fulfillment stages of accepted harvest orders.",
                icon = Icons.Default.LocalShipping,
                onClick = { navController.navigate(Screen.OrderStatus.route) }
            )
        }
    }
}

@Composable
private fun DashboardCard(
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(36.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
