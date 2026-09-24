package com.agroroute.common.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.agroroute.auth.LoginScreen
import com.agroroute.auth.RegisterScreen
import com.agroroute.common.model.UserRole
import com.agroroute.common.util.Constants
import com.agroroute.farmer.CreateListingScreen
import com.agroroute.farmer.FarmerOrderDetailScreen
import com.agroroute.farmer.FarmerOrdersScreen
import com.agroroute.farmer.MyListingsScreen
import com.agroroute.farmer.ViewBidsScreen
import com.agroroute.supplier.BrowseListingsScreen
import com.agroroute.supplier.PlaceBidScreen
import com.agroroute.supplier.SupplierOrderDetailScreen
import com.agroroute.supplier.SupplierOrdersScreen

/**
 * Root navigation graph that wires all [Screen] destinations to their Composable screens.
 * The start destination is [Screen.Login]; deep-linking and role-based routing is handled
 * within [LoginScreen] / [RegisterScreen] after successful authentication.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NavGraph(
    navController: NavHostController = rememberNavController()
) {
    // Track current user role for conditional navigation
    var currentUserRole by remember { mutableStateOf<UserRole?>(null) }
    
    // Track current route for conditional UI
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Determine if we should show bottom nav (hide on auth screens)
    val showBottomNav = currentRoute != null && 
                       currentRoute != Screen.Login.route && 
                       currentRoute != Screen.Register.route

    // Determine if we should show top app bar (hide on auth screens)
    val showTopBar = currentRoute != null && 
                    currentRoute != Screen.Login.route && 
                    currentRoute != Screen.Register.route

    // Get screen title and subtitle based on current route
    val screenTitle = when {
        currentRoute?.startsWith(Screen.MyListings.route) == true -> "My Listings"
        currentRoute?.startsWith(Screen.CreateListing.route) == true -> "Create Listing"
        currentRoute?.startsWith(Screen.SupplierMarketplace.route) == true -> "Marketplace"
        currentRoute?.startsWith(Screen.FarmerOrders.route) == true || currentRoute?.startsWith(Screen.SupplierOrders.route) == true -> "My Orders"
        currentRoute?.startsWith(Screen.FarmerOrderDetail.route) == true || currentRoute?.startsWith(Screen.SupplierOrderDetail.route) == true -> "Order Details"
        currentRoute?.startsWith(Screen.ViewBids.route) == true -> "Bids Received"
        currentRoute?.startsWith(Screen.PlaceBid.route) == true -> "Place Bid"
        else -> "AgroRoute"
    }

    Scaffold(
        topBar = {
            if (showTopBar) {
                TopAppBar(
                    title = { Text(screenTitle) },
                    navigationIcon = {
                        if (navController.previousBackStackEntry != null) {
                            IconButton(onClick = { navController.popBackStack() }) {
                                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                            }
                        }
                    }
                )
            }
        },
        bottomBar = {
            if (showBottomNav && currentUserRole != null) {
                NavigationBar {
                    when (currentUserRole) {
                        UserRole.FARMER -> {
                            NavigationBarItem(
                                icon = { Icon(Icons.Default.Description, contentDescription = null) },
                                label = { Text("My Listings") },
                                selected = currentRoute?.startsWith(Screen.MyListings.route) == true,
                                onClick = {
                                    navController.navigate(Screen.MyListings.route) {
                                        popUpTo(Screen.MyListings.route) { inclusive = true }
                                    }
                                }
                            )
                            NavigationBarItem(
                                icon = { Icon(Icons.Default.Receipt, contentDescription = null) },
                                label = { Text("Orders") },
                                selected = currentRoute?.startsWith(Screen.FarmerOrders.route) == true,
                                onClick = {
                                    navController.navigate(Screen.FarmerOrders.route) {
                                        popUpTo(Screen.FarmerOrders.route) { inclusive = true }
                                    }
                                }
                            )
                        }
                        UserRole.SUPPLIER -> {
                            NavigationBarItem(
                                icon = { Icon(Icons.Default.ShoppingCart, contentDescription = null) },
                                label = { Text("Marketplace") },
                                selected = currentRoute?.startsWith(Screen.SupplierMarketplace.route) == true,
                                onClick = {
                                    navController.navigate(Screen.SupplierMarketplace.route) {
                                        popUpTo(Screen.SupplierMarketplace.route) { inclusive = true }
                                    }
                                }
                            )
                            NavigationBarItem(
                                icon = { Icon(Icons.Default.Receipt, contentDescription = null) },
                                label = { Text("Orders") },
                                selected = currentRoute?.startsWith(Screen.SupplierOrders.route) == true,
                                onClick = {
                                    navController.navigate(Screen.SupplierOrders.route) {
                                        popUpTo(Screen.SupplierOrders.route) { inclusive = true }
                                    }
                                }
                            )
                        }
                        null -> {}
                    }
                }
            }
        }
    ) { paddingValues ->
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            NavHost(
                navController   = navController,
                startDestination = Screen.Login.route,
                modifier = Modifier.padding(paddingValues)
            ) {

                // ── Auth ───────────────────────────────────────────────────────────────
                composable(Screen.Login.route) {
                    LoginScreen(
                        navController = navController,
                        onLoginSuccess = { role ->
                            currentUserRole = role
                        }
                    )
                }

                composable(Screen.Register.route) {
                    RegisterScreen(
                        navController = navController,
                        onRegisterSuccess = { role ->
                            currentUserRole = role
                        }
                    )
                }

                // ── Farmer ─────────────────────────────────────────────────────────────
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

                composable(Screen.FarmerOrders.route) {
                    FarmerOrdersScreen(navController = navController)
                }

                composable(
                    route     = Screen.FarmerOrderDetail.route,
                    arguments = listOf(navArgument(Constants.ARG_ORDER_ID) { type = NavType.StringType })
                ) { backStackEntry ->
                    val orderId = backStackEntry.arguments?.getString(Constants.ARG_ORDER_ID) ?: return@composable
                    FarmerOrderDetailScreen(orderId = orderId, navController = navController)
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

                composable(Screen.SupplierOrders.route) {
                    SupplierOrdersScreen(navController = navController)
                }

                composable(
                    route     = Screen.SupplierOrderDetail.route,
                    arguments = listOf(navArgument(Constants.ARG_ORDER_ID) { type = NavType.StringType })
                ) { backStackEntry ->
                    val orderId = backStackEntry.arguments?.getString(Constants.ARG_ORDER_ID) ?: return@composable
                    SupplierOrderDetailScreen(orderId = orderId, navController = navController)
                }
            }
        }
    }
}
