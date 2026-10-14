package com.agroroute.features.farmer.shell

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavController

@Composable
fun FarmerScaffold(
    currentRoute: String,
    navController: NavController,
    onNavigate: (String) -> Unit,
    content: @Composable (PaddingValues) -> Unit
) {
    com.agroroute.features.farmer.components.FarmerPresentation {
        FarmerScaffoldContent(currentRoute, navController, onNavigate, content)
    }
}

@Composable
private fun FarmerScaffoldContent(
    currentRoute: String,
    navController: NavController,
    onNavigate: (String) -> Unit,
    content: @Composable (PaddingValues) -> Unit
) {
    if (currentRoute == com.agroroute.core.navigation.Screen.NotificationInbox.route) {
        content(PaddingValues())
        return
    }
    val showTopBar = currentRoute in farmerRoutes
    val showBottomBar = currentRoute in farmerTabRoutes

    Scaffold(
        topBar = {
            AnimatedVisibility(visible = showTopBar, enter = fadeIn(), exit = fadeOut()) {
                FarmerTopBar(currentRoute = currentRoute, navController = navController)
            }
        },
        bottomBar = {
            AnimatedVisibility(visible = showBottomBar, enter = fadeIn(), exit = fadeOut()) {
                FarmerBottomBar(selectedRoute = currentRoute, onNavigate = onNavigate)
            }
        },
        content = content
    )
}