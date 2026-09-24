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
    val showTopBar = currentRoute in supplierRoutes
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