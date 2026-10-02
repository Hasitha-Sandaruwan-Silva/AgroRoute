package com.agroroute.features.supplier.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import com.github.thelacspace.icons.Lucide
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.agroroute.core.navigation.Screen
import com.agroroute.core.widgets.AgroSnackbarHost
import com.agroroute.core.widgets.ErrorSnackbarEffect
import com.agroroute.core.widgets.GradientHeader
import com.agroroute.core.widgets.LoadingButton
import com.agroroute.core.widgets.SectionTitle
import com.agroroute.core.widgets.StatCard
import com.agroroute.core.widgets.StatCardsSkeleton
import com.agroroute.features.supplier.marketplace.MarketplaceViewModel
import com.agroroute.features.supplier.orders.SupplierOrderViewModel

@Composable
fun SupplierHomeScreen(
    navController: NavController,
    marketplaceViewModel: MarketplaceViewModel = hiltViewModel(),
    orderViewModel: SupplierOrderViewModel = hiltViewModel()
) {
    val marketState by marketplaceViewModel.uiState.collectAsStateWithLifecycle()
    val orderState by orderViewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        orderViewModel.loadOrdersForSupplier(orderViewModel.currentSupplierId)
    }

    ErrorSnackbarEffect(marketState.error, snackbarHostState) { /* auto-clears on next emission */ }
    ErrorSnackbarEffect(orderState.error, snackbarHostState) { orderViewModel.clearError() }

    val biddingOpen = marketState.listings.count { it.biddingEnabled }

    Column(modifier = Modifier.fillMaxSize()) {
        GradientHeader(
            title = "Welcome, Supplier 👋",
            subtitle = "Discover harvests and grow your supply chain"
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            SectionTitle(title = "Overview")
            if (marketState.isLoading && marketState.listings.isEmpty()) {
                StatCardsSkeleton()
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatCard(
                        label = "Market Listings",
                        value = marketState.listings.size.toString(),
                        icon = Lucide.ShoppingBag,
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        label = "My Orders",
                        value = orderState.orders.size.toString(),
                        icon = Lucide.Receipt,
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(Modifier.height(12.dp))
                StatCard(
                    label = "Bidding Open",
                    value = biddingOpen.toString(),
                    icon = Lucide.ShoppingCart,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(Modifier.height(24.dp))
            SectionTitle(title = "Quick action")
            LoadingButton(
                text = "Browse Market",
                isLoading = false,
                onClick = { navController.navigate(Screen.SupplierMarketplace.route) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Find open harvest listings and place your best bid.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        AgroSnackbarHost(snackbarHostState)
    }
}
