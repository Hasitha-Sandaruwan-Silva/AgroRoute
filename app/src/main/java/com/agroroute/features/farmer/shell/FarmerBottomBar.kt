package com.agroroute.features.farmer.shell

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.agroroute.core.navigation.Screen
import com.github.thelacspace.icons.Lucide

private data class FarmerTab(val route: String, val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector)

private val farmerTabs = listOf(
    FarmerTab(Screen.FarmerHome.route, "Home", Lucide.Home),
    FarmerTab(Screen.MyListings.route, "Listings", Lucide.FileText),
    FarmerTab(Screen.FarmerOrders.route, "Orders", Lucide.Receipt),
    FarmerTab(Screen.FarmerProfile.route, "Profile", Lucide.User)
)

@Composable
fun FarmerBottomBar(
    selectedRoute: String,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    com.agroroute.core.components.AnimatedBottomBar(
        items = farmerTabs.map { com.agroroute.core.components.BottomNavItem(it.route, it.label, it.icon) },
        selectedRoute = selectedRoute,
        onItemSelected = { onNavigate(it.route) },
        modifier = modifier
    )
}
