package com.agroroute.features.supplier.shell

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import com.github.thelacspace.icons.Lucide
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp
import com.agroroute.core.navigation.Screen
import com.agroroute.core.runtime.appViewModel as hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.agroroute.features.auth.AuthViewModel

private data class SupplierTab(
    val route: String,
    val label: String,
    val icon: ImageVector
)

private val supplierTabs = listOf(
    SupplierTab(Screen.SupplierHome.route, "Home", Lucide.Home),
    SupplierTab(Screen.SupplierMarketplace.route, "Marketplace", Lucide.ShoppingBag),
    SupplierTab(Screen.SupplierOrders.route, "Orders", Lucide.Receipt),
    SupplierTab(Screen.SupplierProfile.route, "Profile", Lucide.User)
)

@Composable
fun SupplierBottomBar(
    selectedRoute: String,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (selectedRoute == Screen.SupplierHome.route) {
        com.agroroute.features.supplier.home.SupplierHomeBottomNavigation(onNavigate, modifier)
        return
    }
    val authViewModel: AuthViewModel = hiltViewModel()
    val authState by authViewModel.uiState.collectAsStateWithLifecycle()
    com.agroroute.core.components.AnimatedBottomBar(
        items = supplierTabs.map {
            com.agroroute.core.components.BottomNavItem(it.route, it.label, it.icon,
                profileName = if (it.route == Screen.SupplierProfile.route) authState.user?.displayName ?: "Supplier" else null,
                profileImageUrl = if (it.route == Screen.SupplierProfile.route) authState.user?.profileImageUrl else null)
        },
        selectedRoute = selectedRoute,
        onItemSelected = { onNavigate(it.route) },
        modifier = modifier
    )
}
