package com.agroroute.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.agroroute.ui.auth.LoginScreen
import com.agroroute.ui.auth.RegisterScreen
import com.agroroute.ui.farmer.bids.ViewBidsScreen
import com.agroroute.ui.farmer.listing.CreateListingScreen
import com.agroroute.ui.farmer.listing.MyListingsScreen
import com.agroroute.ui.orders.OrderDetailScreen
import com.agroroute.ui.orders.OrderStatusScreen
import com.agroroute.ui.supplier.bidding.PlaceBidScreen
import com.agroroute.ui.supplier.marketplace.BrowseListingsScreen
import com.agroroute.util.Constants

/**
 * Root navigation graph that wires all [Screen] destinations to their Composable screens.
 * The start destination is [Screen.Login]; deep-linking and role-based routing is handled
 * within [LoginScreen] / [RegisterScreen] after successful authentication.
 */
@Composable
fun NavGraph(
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController   = navController,
        startDestination = Screen.Login.route
    ) {

        // ── Auth ───────────────────────────────────────────────────────────────
        composable(Screen.Login.route) {
            LoginScreen(navController = navController)
        }

        composable(Screen.Register.route) {
            RegisterScreen(navController = navController)
        }

        composable(Screen.RoleSelection.route) {
            // Placeholder — role selection screen to be implemented
            RoleSelectionPlaceholder(navController)
        }

        // ── Farmer ─────────────────────────────────────────────────────────────
        composable(Screen.FarmerHome.route) {
            FarmerHomePlaceholder(navController)
        }

        composable(Screen.CreateListing.route) {
            CreateListingScreen(navController = navController)
        }

        composable(Screen.MyListings.route) {
            MyListingsScreen(navController = navController)
        }

        composable(
            route     = Screen.ViewBids.route,
            arguments = listOf(navArgument(Constants.ARG_LISTING_ID) { type = NavType.StringType })
        ) { backStackEntry ->
            val listingId = backStackEntry.arguments?.getString(Constants.ARG_LISTING_ID) ?: return@composable
            ViewBidsScreen(listingId = listingId, navController = navController)
        }

        // ── Supplier ───────────────────────────────────────────────────────────
        composable(Screen.SupplierMarketplace.route) {
            BrowseListingsScreen(navController = navController)
        }

        composable(
            route     = Screen.PlaceBid.route,
            arguments = listOf(navArgument(Constants.ARG_LISTING_ID) { type = NavType.StringType })
        ) { backStackEntry ->
            val listingId = backStackEntry.arguments?.getString(Constants.ARG_LISTING_ID) ?: return@composable
            PlaceBidScreen(listingId = listingId, navController = navController)
        }

        // ── Orders ─────────────────────────────────────────────────────────────
        composable(
            route     = Screen.OrderDetail.route,
            arguments = listOf(navArgument(Constants.ARG_ORDER_ID) { type = NavType.StringType })
        ) { backStackEntry ->
            val orderId = backStackEntry.arguments?.getString(Constants.ARG_ORDER_ID) ?: return@composable
            OrderDetailScreen(orderId = orderId, navController = navController)
        }

        composable(Screen.OrderStatus.route) {
            OrderStatusScreen(navController = navController)
        }
    }
}

// ── Placeholder composables ────────────────────────────────────────────────────
// These will be replaced with real screen implementations as features are built.

@Composable
@PublishedApi
internal fun RoleSelectionPlaceholder(navController: NavHostController) {
    PlaceholderScreen(label = "Role Selection")
}

@Composable
@PublishedApi
internal fun FarmerHomePlaceholder(navController: NavHostController) {
    PlaceholderScreen(label = "Farmer Home")
}

@Composable
@PublishedApi
internal fun PlaceholderScreen(label: String) {
    androidx.compose.foundation.layout.Box(
        modifier = androidx.compose.ui.Modifier.fillMaxSize(),
        contentAlignment = androidx.compose.ui.Alignment.Center
    ) {
        androidx.compose.material3.Text(text = "$label — Coming Soon")
    }
}
