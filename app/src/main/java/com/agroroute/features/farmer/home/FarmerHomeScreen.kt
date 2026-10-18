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
import com.agroroute.core.runtime.appViewModel as hiltViewModel
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
import com.agroroute.features.farmer.components.*
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import com.agroroute.core.theme.*

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
    authViewModel: AuthViewModel = hiltViewModel(),
    requestsViewModel: com.agroroute.features.farmer.orderrequests.FarmerOrderRequestsViewModel = hiltViewModel()
) {
    val listingState by listingViewModel.uiState.collectAsStateWithLifecycle()
    val orderState by orderViewModel.uiState.collectAsStateWithLifecycle()
    val authState by authViewModel.uiState.collectAsStateWithLifecycle()
    val requestState by requestsViewModel.uiState.collectAsStateWithLifecycle()
    var requestToAccept by remember { mutableStateOf<com.agroroute.core.model.OrderRequest?>(null) }
    val now = com.agroroute.core.components.listingPresentationTime()
    val pendingRequests = remember(requestState.requests, requestState.listingsById, authState.user?.userId, now) {
        pendingHomeRequests(requestState, authState.user?.userId.orEmpty(), now)
    }
    androidx.lifecycle.compose.LifecycleResumeEffect(authState.user?.userId) {
        if (authState.user != null) requestsViewModel.loadRequests()
        onPauseOrDispose { requestsViewModel.stopObservingRequests() }
    }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        listingViewModel.loadFarmerListings(listingViewModel.currentFarmerId)
        orderViewModel.loadOrdersForFarmer(orderViewModel.currentFarmerId)
    }

    ErrorSnackbarEffect(requestState.error, snackbarHostState) { requestsViewModel.clearError() }
    ErrorSnackbarEffect(listingState.error, snackbarHostState) { listingViewModel.clearError() }
    ErrorSnackbarEffect(orderState.error, snackbarHostState) { orderViewModel.clearError() }

    val publishedListings = remember(listingState.listings) { listingState.listings.filterNot { it.isDraft } }
    val activeListings = remember(publishedListings) { publishedListings.count { it.status == ListingStatus.OPEN } }
    val recentOrders = remember(orderState.orders) { orderState.orders.sortedByDescending { it.createdAt }.take(3) }
    val totalListings = publishedListings.size
    val totalOrders = orderState.orders.size
    val isLoading = listingState.isLoading && listingState.listings.isEmpty()

    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)) {
            FarmerHeroReveal {
                FarmerArtworkHero {
                    Text("YOUR FARM, CONNECTED", style = MaterialTheme.typography.labelSmall, color = LeafAccent)
                    Text("Your harvest.\nMore opportunity.", style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold)
                    Text("A clearer view of your harvest and trade.", style = MaterialTheme.typography.bodySmall,
                        color = CreamInk.copy(alpha = .88f))
                    TextButton(onClick = { navController.navigate(FarmerHomeDestination.MY_LISTINGS.route) },
                        colors = ButtonDefaults.textButtonColors(contentColor = CreamInk), contentPadding = PaddingValues(0.dp)) {
                        Text("Explore your farm overview", style = MaterialTheme.typography.labelMedium)
                        Spacer(Modifier.width(8.dp)); Icon(Lucide.ArrowRight, null, Modifier.size(16.dp))
                    }
                }
            }
            FarmerSectionHeader("Farm essentials")
            BoxWithConstraints(Modifier.fillMaxWidth()) {
                val compact = maxWidth < 350.dp || LocalDensity.current.fontScale > 1.3f
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        FarmAction("New listing", "Share your harvest", Lucide.Plus, Modifier.weight(1f)) {
                            navController.navigate(FarmerHomeDestination.CREATE_LISTING.route)
                        }
                        FarmAction("My listings", "Manage your produce", Lucide.Sprout, Modifier.weight(1f), earth = true) {
                            navController.navigate(FarmerHomeDestination.MY_LISTINGS.route)
                        }
                        if (!compact) {
                            FarmAction("Orders", "Pickups and deliveries", Lucide.Truck, Modifier.weight(1f)) {
                                navController.navigate(FarmerHomeDestination.ORDERS.route)
                            }
                            FarmAction("Requests", "Supplier requests", Lucide.Inbox, Modifier.weight(1f), earth = true) {
                                navController.navigate(FarmerHomeDestination.ORDER_REQUESTS.route)
                            }
                        }
                    }
                    if (compact) Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        FarmAction("Orders", "Pickups and deliveries", Lucide.Truck, Modifier.weight(1f)) {
                            navController.navigate(FarmerHomeDestination.ORDERS.route)
                        }
                        FarmAction("Requests", "Supplier requests", Lucide.Inbox, Modifier.weight(1f), earth = true) {
                            navController.navigate(FarmerHomeDestination.ORDER_REQUESTS.route)
                        }
                    }
                }
            }
            if (isLoading) ShimmerBox(Modifier.fillMaxWidth().height(72.dp))
            else Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FarmMetric("Orders", "$totalOrders", Modifier.weight(1f))
                FarmMetric("Active listings", "$activeListings", Modifier.weight(1f))
                FarmMetric("Total listings", "$totalListings", Modifier.weight(1f))
            }
            IncomingOrderRequests(pendingRequests, requestState.listingsById,
                busy = requestState.isLoading || requestState.isSubmitting,
                onView = { navController.navigate(Screen.FarmerOrderRequests.route) },
                onAccept = { requestToAccept = it })
            FarmerPromo { navController.navigate(FarmerHomeDestination.CREATE_LISTING.route) }
            if (orderState.orders.isNotEmpty()) {
                FarmerSectionHeader("Recent orders", action = "View all",
                    onAction = { navController.navigate(FarmerHomeDestination.ORDERS.route) })
                recentOrders.forEach { order ->
                    FarmerRecentOrderCard(order) {
                        navController.navigate(Screen.FarmerOrderDetail.createRoute(order.orderId))
                    }
                }
            }
            DashboardAction("Harvest calendar", "Plan upcoming harvest dates", Lucide.Calendar) { navController.navigate(FarmerHomeDestination.HARVEST_CALENDAR.route) }
            AppCard(onClick = { navController.navigate(Screen.FarmerLeaderboard.route) }, containerColor = MaterialTheme.colorScheme.surface) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(Lucide.Award, null, tint = MaterialTheme.colorScheme.primary)
                    Column(Modifier.weight(1f)) {
                        Text("Your community points", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurface)
                        Text("${authState.user?.points ?: 0} pts", style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    }
                    Icon(Lucide.ChevronRight, null, tint = MaterialTheme.colorScheme.primary)
                }
            }
            if (com.agroroute.core.runtime.LocalMutationAllowed.current) com.agroroute.features.leaderboard.CommunityLeaderboards(
                onViewAll = { role ->
                        navController.navigate(Screen.FarmerLeaderboard.route)
                        navController.currentBackStackEntry?.savedStateHandle?.set("leaderboardRole", role)
                    },
                onParticipant = { id -> navController.navigate(com.agroroute.features.participants.ParticipantRoutes.profile(id)) }
            )
            else com.agroroute.features.leaderboard.HomeLeaderboard("FARMER", onViewAll = { navController.navigate(Screen.FarmerLeaderboard.route) })
            Spacer(Modifier.height(8.dp))
        }
        requestToAccept?.let { request ->
            com.agroroute.features.farmer.orderrequests.AcceptRequestConfirmationDialog(request,
                requestState.listingsById[request.listingId],
                onDismiss = { requestToAccept = null },
                onConfirm = { requestToAccept = null; requestsViewModel.acceptRequest(request) })
        }
        AgroSnackbarHost(snackbarHostState, Modifier.align(Alignment.BottomCenter))
    }
}

@Composable
private fun FarmMetric(label: String, value: String, modifier: Modifier) {
    AppCard(modifier, containerColor = MaterialTheme.colorScheme.surface) {

        Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun FarmAction(title: String, subtitle: String, icon: ImageVector, modifier: Modifier, earth: Boolean = false, onClick: () -> Unit) {
    AppCard(modifier.heightIn(min = 100.dp), onClick = onClick) {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)) {
            AgroIconBox(icon, boxSize = 34.dp, iconSize = 21.dp, corner = 12.dp,
                containerColor = if (earth) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.primary)
            Text(title, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }
}
