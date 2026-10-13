package com.agroroute.features.farmer.shell

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.agroroute.core.runtime.appViewModel as hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.agroroute.core.components.AnimatedBottomBar
import com.agroroute.core.components.BottomNavItem
import com.agroroute.core.navigation.Screen
import com.agroroute.features.auth.AuthViewModel
import com.github.thelacspace.icons.Lucide

private data class FarmerTab(
    val route: String,
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)

private val farmerTabs = listOf(
    FarmerTab(
        route = Screen.FarmerHome.route,
        label = "Home",
        icon = Lucide.Home
    ),
    FarmerTab(
        route = Screen.MyListings.route,
        label = "Listings",
        icon = Lucide.FileText
    ),
    FarmerTab(
        route = Screen.FarmerOrders.route,
        label = "Orders",
        icon = Lucide.Receipt
    ),
    FarmerTab(
        route = Screen.FarmerProfile.route,
        label = "Profile",
        icon = Lucide.User
    )
)

@Composable
fun FarmerBottomBar(
    selectedRoute: String,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val authViewModel: AuthViewModel = hiltViewModel()

    val authState by authViewModel.uiState.collectAsStateWithLifecycle()

    AnimatedBottomBar(
        items = farmerTabs.map { tab ->

            BottomNavItem(
                route = tab.route,
                label = tab.label,
                icon = tab.icon,

                profileName = if (
                    tab.route == Screen.FarmerProfile.route
                ) {
                    authState.user?.displayName ?: "Farmer"
                } else {
                    null
                },

                profileImageUrl = if (
                    tab.route == Screen.FarmerProfile.route
                ) {
                    authState.user?.profileImageUrl
                } else {
                    null
                }
            )
        },

        selectedRoute = selectedRoute,

        onItemSelected = { item ->
            onNavigate(item.route)
        },

        modifier = modifier
    )
}