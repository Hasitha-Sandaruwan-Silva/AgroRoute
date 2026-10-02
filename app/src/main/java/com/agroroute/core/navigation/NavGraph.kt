package com.agroroute.core.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.agroroute.features.auth.LoginScreen
import com.agroroute.features.auth.RegisterScreen
import com.agroroute.features.welcome.WelcomeScreen
import com.agroroute.core.model.UserRole
import com.agroroute.core.util.Constants
import com.agroroute.features.farmer.bids.ViewBidsScreen
import com.agroroute.features.farmer.home.FarmerHomeScreen
import com.agroroute.features.farmer.calendar.HarvestCalendarScreen
import com.agroroute.features.farmer.listing.CreateListingScreen
import com.agroroute.features.farmer.listing.MyListingsScreen
import com.agroroute.features.farmer.orders.FarmerOrderDetailScreen
import com.agroroute.features.farmer.orders.FarmerOrdersScreen
import com.agroroute.features.farmer.profile.FarmerProfileScreen
import com.agroroute.features.farmer.shell.FarmerScaffold
import com.agroroute.features.farmer.shell.farmerRoutes
import com.agroroute.features.supplier.home.SupplierHomeScreen
import com.agroroute.features.supplier.bidding.PlaceBidScreen
import com.agroroute.features.supplier.marketplace.BrowseListingsScreen
import com.agroroute.features.supplier.orders.SupplierOrderDetailScreen
import com.agroroute.features.supplier.orders.SupplierOrdersScreen
import com.agroroute.features.supplier.profile.SupplierProfileScreen
import com.agroroute.features.supplier.shell.SupplierScaffold
import com.agroroute.features.supplier.shell.supplierRoutes

private val authRoutes = setOf(
    Screen.Welcome.route,
    Screen.Login.route,
    Screen.Register.route
)

@Composable
fun NavGraph(
    navController: NavHostController = rememberNavController()
) {
    var currentUserRole by remember { mutableStateOf<UserRole?>(null) }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val hostContent: @Composable (PaddingValues) -> Unit = { paddingValues ->
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            NavHost(
                navController = navController,
                startDestination = Screen.Welcome.route, // 👈 නිවැරදිව Welcome එකට යොමු කර ඇත (කොමාව සහිතව)
                modifier = Modifier.padding(paddingValues)
            ) {

                composable(Screen.Welcome.route) {
                    WelcomeScreen(navController = navController)
                }

                composable(Screen.Login.route) {
                    LoginScreen(
                        navController = navController,
                        onLoginSuccess = { role -> currentUserRole = role }
                    )
                }

                composable(Screen.Register.route) {
                    RegisterScreen(
                        navController = navController,
                        onRegisterSuccess = { role -> currentUserRole = role }
                    )
                }

                composable(Screen.FarmerHome.route) {
                    FarmerHomeScreen(navController = navController)
                }

                composable(Screen.SupplierHome.route) {
                    SupplierHomeScreen(navController = navController)
                }

                composable(Screen.FarmerProfile.route) {
                    FarmerProfileScreen(
                        navController = navController,
                        userRole = currentUserRole
                    )
                }

                composable(Screen.SupplierProfile.route) {
                    SupplierProfileScreen(
                        navController = navController,
                        userRole = currentUserRole
                    )
                }

                composable(Screen.CreateListing.route) {
                    CreateListingScreen(navController = navController)
                }

                composable(Screen.MyListings.route) {
                    MyListingsScreen(navController = navController)
                }

                composable(Screen.HarvestCalendar.route) {
                    HarvestCalendarScreen(navController = navController)
                }

                composable(
                    route = Screen.ViewBids.route,
                    arguments = listOf(navArgument(Constants.ARG_LISTING_ID) { type = NavType.StringType })
                ) { backStackEntry ->
                    val listingId = backStackEntry.arguments?.getString(Constants.ARG_LISTING_ID) ?: return@composable
                    ViewBidsScreen(listingId = listingId, navController = navController)
                }

                composable(Screen.FarmerOrders.route) {
                    FarmerOrdersScreen(navController = navController)
                }

                composable(
                    route = Screen.FarmerOrderDetail.route,
                    arguments = listOf(navArgument(Constants.ARG_ORDER_ID) { type = NavType.StringType })
                ) { backStackEntry ->
                    val orderId = backStackEntry.arguments?.getString(Constants.ARG_ORDER_ID) ?: return@composable
                    FarmerOrderDetailScreen(orderId = orderId, navController = navController)
                }

                composable(Screen.SupplierMarketplace.route) {
                    BrowseListingsScreen(navController = navController)
                }

                composable(
                    route = Screen.PlaceBid.route,
                    arguments = listOf(navArgument(Constants.ARG_LISTING_ID) { type = NavType.StringType })
                ) { backStackEntry ->
                    val listingId = backStackEntry.arguments?.getString(Constants.ARG_LISTING_ID) ?: return@composable
                    PlaceBidScreen(listingId = listingId, navController = navController)
                }

                composable(Screen.SupplierOrders.route) {
                    SupplierOrdersScreen(navController = navController)
                }

                composable(
                    route = Screen.SupplierOrderDetail.route,
                    arguments = listOf(navArgument(Constants.ARG_ORDER_ID) { type = NavType.StringType })
                ) { backStackEntry ->
                    val orderId = backStackEntry.arguments?.getString(Constants.ARG_ORDER_ID) ?: return@composable
                    SupplierOrderDetailScreen(orderId = orderId, navController = navController)
                }
            }
        }
    }

    when {
        currentRoute in farmerRoutes -> FarmerScaffold(
            currentRoute = currentRoute.orEmpty(),
            navController = navController,
            onNavigate = { route ->
                navController.navigate(route) {
                    popUpTo(Screen.FarmerHome.route) { saveState = true }
                    launchSingleTop = true
                    restoreState = true
                }
            },
            content = hostContent
        )
        currentRoute in supplierRoutes -> SupplierScaffold(
            currentRoute = currentRoute.orEmpty(),
            navController = navController,
            onNavigate = { route ->
                navController.navigate(route) {
                    popUpTo(Screen.SupplierHome.route) { saveState = true }
                    launchSingleTop = true
                    restoreState = true
                }
            },
            content = hostContent
        )
        else -> hostContent(PaddingValues())
    }
}