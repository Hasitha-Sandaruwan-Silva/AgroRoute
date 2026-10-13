package com.agroroute.features.supplier.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.agroroute.core.runtime.appViewModel as hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.agroroute.core.model.OrderRequestStatus
import com.agroroute.core.navigation.Screen
import com.agroroute.core.theme.ForestDeep
import com.agroroute.core.theme.ForestMid
import com.agroroute.core.theme.ForestSoft
import com.agroroute.core.theme.LeafAccent
import com.agroroute.core.components.AgroSnackbarHost
import com.agroroute.core.components.AppCard
import com.agroroute.core.components.ErrorSnackbarEffect
import com.agroroute.core.components.SectionTitle
import com.agroroute.core.components.StatCardsSkeleton
import com.agroroute.core.components.UserProfileAvatar
import com.agroroute.features.supplier.marketplace.MarketplaceViewModel
import com.agroroute.features.supplier.orders.SupplierOrderViewModel
import com.agroroute.features.settings.NotificationBell
import com.agroroute.features.auth.AuthViewModel
import com.github.thelacspace.icons.Lucide
import com.agroroute.core.components.*
import com.agroroute.core.theme.*
import com.agroroute.features.leaderboard.HomeLeaderboard

@Composable
fun SupplierHomeScreen(
    navController: NavController,
    marketplaceViewModel: MarketplaceViewModel = hiltViewModel(),
    orderViewModel: SupplierOrderViewModel = hiltViewModel(),
    authViewModel: AuthViewModel = hiltViewModel()
) {
    val marketState by marketplaceViewModel.uiState.collectAsStateWithLifecycle()
    val orderState by orderViewModel.uiState.collectAsStateWithLifecycle()
    val authState by authViewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        orderViewModel.loadOrdersForSupplier(orderViewModel.currentSupplierId)
    }

    ErrorSnackbarEffect(marketState.error, snackbarHostState) { /* auto-clears on next emission */ }
    ErrorSnackbarEffect(orderState.error, snackbarHostState) { orderViewModel.clearError() }

    val pendingRequests = orderState.requests.count { it.status == OrderRequestStatus.PENDING }
    val isLoading = (marketState.isLoading && marketState.listings.isEmpty()) ||
        (orderState.isLoading && orderState.orders.isEmpty())

    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)) {
            PremiumHero {
                Text("FROM HARVEST TO BUSINESS", style = MaterialTheme.typography.labelSmall, color = LeafAccent)
                Text("Good day, ${authState.user?.displayName?.takeIf(String::isNotBlank) ?: "Supplier"}",
                    style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text("Find fresh opportunities. Keep trade moving.", style = MaterialTheme.typography.bodyMedium,
                    color = CreamInk.copy(alpha = 0.85f))
                HorizontalDivider(color = CreamInk.copy(alpha = 0.18f))
                if (isLoading) ShimmerBox(Modifier.fillMaxWidth().height(72.dp))
                else Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    SupplierMetric("Open listings", "${marketState.listings.size}", Modifier.weight(1f))
                    SupplierMetric("My orders", "${orderState.orders.size}", Modifier.weight(1f))
                    SupplierMetric("Pending", "$pendingRequests", Modifier.weight(1f))
                }
            }
            HomeLeaderboard("SUPPLIER", onViewAll = { navController.navigate(Screen.SupplierLeaderboard.route) })
            Text("Your trading desk", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            PremiumHero(brush = AgroGradients.earth) {
                Icon(Lucide.ShoppingBag, null, Modifier.size(26.dp))
                Text("Discover the next harvest", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text("Request at the listed price; bidding is optional.", style = MaterialTheme.typography.bodyMedium)
                FilledTonalButton(onClick = { navController.navigate(Screen.SupplierMarketplace.route) },
                    colors = ButtonDefaults.filledTonalButtonColors(containerColor = CreamInk, contentColor = SoilBrown)) {
                    Text("Open Marketplace"); Spacer(Modifier.width(8.dp)); Icon(Lucide.ArrowRight, null, Modifier.size(18.dp))
                }
            }
            DashboardAction("My orders", "Track requests, pickups and deliveries", Lucide.Truck) { navController.navigate(Screen.SupplierOrders.route) }
            AppCard(onClick = { navController.navigate(Screen.SupplierLeaderboard.route) }, containerColor = MaterialTheme.colorScheme.secondaryContainer) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(Lucide.Award, null, tint = MaterialTheme.colorScheme.onSecondaryContainer)
                    Column(Modifier.weight(1f)) {
                        Text("Your community points", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSecondaryContainer)
                        Text("${authState.user?.points ?: 0} pts", style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSecondaryContainer)
                    }
                    Icon(Lucide.ChevronRight, null, tint = MaterialTheme.colorScheme.onSecondaryContainer)
                }
            }
            Spacer(Modifier.height(8.dp))
        }
        AgroSnackbarHost(snackbarHostState, Modifier.align(Alignment.BottomCenter))
    }
}

@Composable
private fun SupplierMetric(label: String, value: String, modifier: Modifier) {
    Column(modifier) {
        Text(value, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = CreamInk)
        Text(label, style = MaterialTheme.typography.labelSmall, color = CreamInk.copy(alpha = 0.8f))
    }
}
