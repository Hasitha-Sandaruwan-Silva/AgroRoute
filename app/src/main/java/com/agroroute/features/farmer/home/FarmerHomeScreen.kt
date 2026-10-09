package com.agroroute.features.farmer.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.agroroute.core.model.ListingStatus
import com.agroroute.core.navigation.Screen
import com.agroroute.core.theme.ForestDeep
import com.agroroute.core.theme.ForestMid
import com.agroroute.core.components.AgroSnackbarHost
import com.agroroute.core.components.ErrorSnackbarEffect
import com.agroroute.core.components.ShimmerBox
import com.agroroute.core.components.UserProfileAvatar
import com.agroroute.features.settings.NotificationBell
import com.agroroute.features.auth.AuthViewModel
import com.agroroute.features.farmer.listing.ListingViewModel
import com.agroroute.features.farmer.orders.FarmerOrderViewModel
import com.github.thelacspace.icons.Lucide
import com.agroroute.core.components.*
import com.agroroute.core.theme.*
import com.agroroute.features.leaderboard.HomeLeaderboard

internal enum class FarmerHomeDestination(val route: String) {
    CREATE_LISTING(Screen.CreateListing.route),
    MY_LISTINGS(Screen.MyListings.route),
    ORDERS(Screen.FarmerOrders.route),
    ORDER_REQUESTS(Screen.FarmerOrderRequests.route),
    HARVEST_CALENDAR(Screen.HarvestCalendar.route)
}

@Composable
fun FarmerHomeScreen(
    navController: NavController,
    listingViewModel: ListingViewModel = hiltViewModel(),
    orderViewModel: FarmerOrderViewModel = hiltViewModel(),
    authViewModel: AuthViewModel = hiltViewModel()
) {
    val listingState by listingViewModel.uiState.collectAsStateWithLifecycle()
    val orderState by orderViewModel.uiState.collectAsStateWithLifecycle()
    val authState by authViewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        listingViewModel.loadFarmerListings(listingViewModel.currentFarmerId)
        orderViewModel.loadOrdersForFarmer(orderViewModel.currentFarmerId)
    }

    ErrorSnackbarEffect(listingState.error, snackbarHostState) { listingViewModel.clearError() }
    ErrorSnackbarEffect(orderState.error, snackbarHostState) { orderViewModel.clearError() }

    val publishedListings = listingState.listings.filterNot { it.isDraft }
    val activeListings = publishedListings.count { it.status == ListingStatus.OPEN }
    val totalListings = publishedListings.size
    val totalOrders = orderState.orders.size
    val isLoading = listingState.isLoading && listingState.listings.isEmpty()

    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)) {
            PremiumHero {
                Text("YOUR FARM, CONNECTED", style = MaterialTheme.typography.labelSmall, color = LeafAccent)
                Text("Good day, ${authState.user?.displayName?.takeIf(String::isNotBlank) ?: "Farmer"}",
                    style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text("A clearer view of your harvest and trade.", style = MaterialTheme.typography.bodyMedium,
                    color = CreamInk.copy(alpha = 0.85f))
                HorizontalDivider(color = CreamInk.copy(alpha = 0.18f))
                if (isLoading) ShimmerBox(Modifier.fillMaxWidth().height(72.dp))
                else Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    FarmMetric("Orders", "$totalOrders", Modifier.weight(1f))
                    FarmMetric("Active listings", "$activeListings", Modifier.weight(1f))
                    FarmMetric("Total listings", "$totalListings", Modifier.weight(1f))
                }
                TextButton(onClick = { navController.navigate(FarmerHomeDestination.MY_LISTINGS.route) },
                    colors = ButtonDefaults.textButtonColors(contentColor = CreamInk), contentPadding = PaddingValues(0.dp)) {
                    Text("Explore your farm overview")
                    Spacer(Modifier.width(8.dp)); Icon(Lucide.ArrowRight, null, Modifier.size(18.dp))
                }
            }
            HomeLeaderboard("FARMER", onViewAll = { navController.navigate(Screen.FarmerLeaderboard.route) })
            Text("Farm essentials", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                FarmAction("New listing", "Share your harvest", Lucide.Plus, Modifier.weight(1f)) {
                    navController.navigate(FarmerHomeDestination.CREATE_LISTING.route)
                }
                FarmAction("My listings", "Manage your produce", Lucide.Sprout, Modifier.weight(1f), earth = true) {
                    navController.navigate(FarmerHomeDestination.MY_LISTINGS.route)
                }
            }
            DashboardAction("Orders", "Pickups and deliveries", Lucide.Truck) { navController.navigate(FarmerHomeDestination.ORDERS.route) }
            DashboardAction("Requests", "Review supplier requests", Lucide.Inbox, earth = true) { navController.navigate(FarmerHomeDestination.ORDER_REQUESTS.route) }
            DashboardAction("Harvest calendar", "Plan upcoming harvest dates", Lucide.Calendar) { navController.navigate(FarmerHomeDestination.HARVEST_CALENDAR.route) }
            AppCard(onClick = { navController.navigate(Screen.FarmerLeaderboard.route) }, containerColor = MaterialTheme.colorScheme.secondaryContainer) {
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
private fun FarmMetric(label: String, value: String, modifier: Modifier) {
    Column(modifier) {
        Text(value, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = CreamInk)
        Text(label, style = MaterialTheme.typography.labelSmall, color = CreamInk.copy(alpha = 0.8f))
    }
}

@Composable
private fun FarmAction(title: String, subtitle: String, icon: ImageVector, modifier: Modifier, earth: Boolean = false, onClick: () -> Unit) {
    val colors = LocalAgroExtendedColors.current
    AppCard(modifier, onClick = onClick) {
        AgroIconBox(icon, containerColor = if (earth) colors.accentIconContainer else colors.iconContainer,
            contentColor = if (earth) colors.accentIconOnContainer else colors.iconOnContainer)
        Spacer(Modifier.height(14.dp))
        Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
