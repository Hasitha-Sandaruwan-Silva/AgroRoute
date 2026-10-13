package com.agroroute.features.supplier.shell

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.navigation.NavController

@Composable
fun SupplierScaffold(
    currentRoute: String,
    navController: NavController,
    onNavigate: (String) -> Unit,
    content: @Composable (PaddingValues) -> Unit
) {
    // History owns its app bar and insets for both roles; avoid nested shell padding.
    if (currentRoute == com.agroroute.core.navigation.Screen.NotificationInbox.route) {
        content(PaddingValues())
        return
    }
    // Home owns its photographic branding/profile/notification header.
    val showTopBar = currentRoute in supplierRoutes &&
        currentRoute != com.agroroute.core.navigation.Screen.SupplierHome.route
    val showBottomBar = currentRoute in supplierTabRoutes

    Scaffold(
        topBar = {
            AnimatedVisibility(visible = showTopBar, enter = fadeIn(), exit = fadeOut()) {
                SupplierTopBar(currentRoute = currentRoute, navController = navController)
            }
        },
        bottomBar = {
            AnimatedVisibility(visible = showBottomBar, enter = fadeIn(), exit = fadeOut()) {
                SupplierBottomBar(selectedRoute = currentRoute, onNavigate = onNavigate)
            }
        },
        content = content
    )
}
