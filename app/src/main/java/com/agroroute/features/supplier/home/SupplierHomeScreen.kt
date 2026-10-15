package com.agroroute.features.supplier.home

import androidx.compose.foundation.layout.*
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.agroroute.core.components.AgroSnackbarHost
import com.agroroute.core.components.ErrorSnackbarEffect
import com.agroroute.core.components.listingPresentationTime
import com.agroroute.core.navigation.Screen
import com.agroroute.core.runtime.appViewModel
import com.agroroute.features.auth.AuthViewModel
import com.agroroute.features.settings.NotificationBell
import com.agroroute.features.supplier.bids.SupplierBidsViewModel
import com.agroroute.features.supplier.marketplace.MarketplaceViewModel
import com.agroroute.features.supplier.orders.SupplierOrderViewModel
import androidx.compose.ui.graphics.Color

@Composable
fun SupplierHomeScreen(
    navController: NavController,
    marketplaceViewModel: MarketplaceViewModel = appViewModel(),
    orderViewModel: SupplierOrderViewModel = appViewModel(),
    authViewModel: AuthViewModel = appViewModel(),
    bidsViewModel: SupplierBidsViewModel = appViewModel()
) {
    val market by marketplaceViewModel.uiState.collectAsStateWithLifecycle()
    val orders by orderViewModel.uiState.collectAsStateWithLifecycle()
    val auth by authViewModel.uiState.collectAsStateWithLifecycle()
    val bids by bidsViewModel.uiState.collectAsStateWithLifecycle()
    val now = listingPresentationTime()
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(auth.user?.userId) {
        val id = orderViewModel.currentSupplierId
        if (id.isNotBlank()) {
            orderViewModel.loadOrdersForSupplier(id)
            bidsViewModel.loadBids(id)
        }
    }
    ErrorSnackbarEffect(market.error, snackbar) { }
    ErrorSnackbarEffect(orders.error, snackbar) { orderViewModel.clearError() }
    ErrorSnackbarEffect(bids.error, snackbar) { bidsViewModel.clearError() }
    Box(Modifier.fillMaxSize()) {
        SupplierHomeContent(
            user = auth.user, listings = market.listings, bids = bids.bids, heroStatusBar = true,
            orders = orders.orders, loading = market.isLoading && market.listings.isEmpty(), now = now,
            onMarketplace = { navController.navigate(Screen.SupplierMarketplace.route) },
            onOrders = { navController.navigate(Screen.SupplierOrders.route) },
            onBids = { navController.navigate(Screen.SupplierBids.route) },
            onProfile = { navController.navigate(Screen.SupplierProfile.route) },
            onCommunity = { navController.navigate(Screen.SupplierLeaderboard.route) },
            onListing = { listing ->
                navController.navigate(if (listing.biddingEnabled) Screen.PlaceBid.createRoute(listing.listingId)
                    else Screen.PlaceOrderRequest.createRoute(listing.listingId))
            },
            notification = { NotificationBell(navController, tint = Color.White) },
            onSearch = {
                navController.currentBackStackEntry?.savedStateHandle?.set("supplier_home_search", true)
                navController.navigate(Screen.SupplierMarketplace.route)
            }
        )
        AgroSnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter))
    }
}
