package com.agroroute.features.supplier.shell

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.systemBars
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.runtime.Composable
import androidx.navigation.NavController

@Composable
fun SupplierScaffold(
    currentRoute: String,
    navController: NavController,
    onNavigate: (String) -> Unit,
    content: @Composable (PaddingValues) -> Unit
) {
    androidx.compose.runtime.CompositionLocalProvider(
        com.agroroute.features.supplier.components.LocalSupplierPresentation provides true
    ) {
        SupplierScaffoldContent(currentRoute, navController, onNavigate, content)
    }
}

@Composable
private fun SupplierScaffoldContent(
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
        // Home's photographic hero owns the status-bar area, just like the shared bar.
        contentWindowInsets = if (currentRoute == com.agroroute.core.navigation.Screen.SupplierHome.route)
            WindowInsets.systemBars.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom)
        else ScaffoldDefaults.contentWindowInsets,
        topBar = {
            // Remove immediately on Home; an exit animation would retain a second header.
            if (showTopBar) {
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
