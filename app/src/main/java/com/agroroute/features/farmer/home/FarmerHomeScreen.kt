package com.agroroute.features.farmer.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.List
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
import com.agroroute.features.auth.AuthViewModel
import com.agroroute.features.farmer.listing.ListingViewModel
import com.agroroute.features.farmer.orders.FarmerOrderViewModel
import com.github.thelacspace.icons.Lucide

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

    // THEME AWARE - uses MaterialTheme.colorScheme which auto switches via AgroRouteTheme
    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Box(
            modifier = Modifier.fillMaxWidth().height(220.dp)
                .background(Brush.verticalGradient(listOf(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), Color.Transparent)))
        )

        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).padding(top = 12.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            // HEADER - NO NOTIFICATION ICON - REMOVED AS REQUESTED
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primary, modifier = Modifier.size(48.dp).shadow(6.dp, CircleShape)) {
                        Box(contentAlignment = Alignment.Center) { Icon(Lucide.User, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(22.dp)) }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Hi, Farmer 👋", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("Welcome back!", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold, fontSize = 22.sp), color = MaterialTheme.colorScheme.onBackground, maxLines = 1)
                    }
                }
                // Profile avatar only - notification removed
                Surface(
                    shape = CircleShape, color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.size(44.dp).clickable { navController.navigate(Screen.FarmerProfile.route) }
                ) {
                    Box(contentAlignment = Alignment.Center) { Icon(Lucide.User, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(22.dp)) }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            if (isLoading) {
                ShimmerBox(modifier = Modifier.fillMaxWidth().height(190.dp), cornerRadius = 28.dp)
            } else {
                // FARM OVERVIEW - LINKED TO MY LISTINGS
                Surface(
                    modifier = Modifier.fillMaxWidth().shadow(16.dp, RoundedCornerShape(28.dp)).clickable { navController.navigate(FarmerHomeDestination.MY_LISTINGS.route) },
                    shape = RoundedCornerShape(28.dp), color = ForestMid
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(36.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.15f)), contentAlignment = Alignment.Center) {
                                    Icon(Lucide.MapPin, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Text("Farm Overview", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
                            }
                            Icon(Lucide.Leaf, contentDescription = null, tint = Color.White.copy(alpha = 0.2f), modifier = Modifier.size(36.dp))
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text("$totalOrders", fontSize = 52.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.padding(bottom = 8.dp)) {
                                Text("Orders", color = Color.White.copy(alpha = 0.7f), fontSize = 14.sp)
                                Text("To fulfill", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Divider(color = Color.White.copy(alpha = 0.12f))
                        Spacer(modifier = Modifier.height(14.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column { Text("Active", fontSize = 12.sp, color = Color.White.copy(alpha = 0.6f)); Text("$activeListings listings", fontWeight = FontWeight.Bold, color = Color.White) }
                            Column { Text("Total", fontSize = 12.sp, color = Color.White.copy(alpha = 0.6f)); Text("$totalListings listings", fontWeight = FontWeight.Bold, color = Color.White) }
                            Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.primary) {
                                Text("Live", modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // POINTS - LINKED TO LEADERBOARD
            Surface(
                modifier = Modifier.fillMaxWidth().shadow(8.dp, RoundedCornerShape(20.dp)).clickable { navController.navigate(Screen.FarmerLeaderboard.route) },
                shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surface
            ) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(44.dp).clip(CircleShape).background(MaterialTheme.colorScheme.secondaryContainer), contentAlignment = Alignment.Center) {
                        Icon(Lucide.Trophy, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.size(22.dp))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Your points", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("${authState.user?.points ?: 0} pts", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurface)
                    }
                    Text("Leaderboard →", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            Text("Quick Actions", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = MaterialTheme.colorScheme.onBackground)
            Spacer(modifier = Modifier.height(12.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                ActionCard(modifier = Modifier.weight(1f), title = "New\nListing", icon = Lucide.Plus, bg = MaterialTheme.colorScheme.primary) { navController.navigate(FarmerHomeDestination.CREATE_LISTING.route) }
                ActionCard(modifier = Modifier.weight(1f), title = "My\nListings", icon = Icons.Filled.List, bg = ForestMid) { navController.navigate(FarmerHomeDestination.MY_LISTINGS.route) }
            }
            Spacer(modifier = Modifier.height(14.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                ActionCard(modifier = Modifier.weight(1f), title = "Orders", icon = Lucide.Receipt, bg = Color(0xFF2E7D32)) { navController.navigate(FarmerHomeDestination.ORDERS.route) }
                ActionCard(modifier = Modifier.weight(1f), title = "Requests", icon = Lucide.Mail, bg = Color(0xFF6A1B9A)) { navController.navigate(FarmerHomeDestination.ORDER_REQUESTS.route) }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Surface(
                modifier = Modifier.fillMaxWidth().clickable { navController.navigate(FarmerHomeDestination.HARVEST_CALENDAR.route) },
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            ) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(48.dp).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.primaryContainer), contentAlignment = Alignment.Center) {
                        Icon(Icons.Filled.AccessTime, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(24.dp))
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Harvest Calendar", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        Text("Check upcoming harvest dates", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Icon(Lucide.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Spacer(modifier = Modifier.height(40.dp))
        }

        AgroSnackbarHost(snackbarHostState = snackbarHostState, modifier = Modifier.align(Alignment.BottomCenter))
    }
}

@Composable
private fun ActionCard(modifier: Modifier, title: String, icon: ImageVector, bg: Color, onClick: () -> Unit) {
    Surface(
        modifier = modifier.height(110.dp).shadow(6.dp, RoundedCornerShape(20.dp)).clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Box(modifier = Modifier.size(38.dp).clip(CircleShape).background(bg.copy(alpha = 0.12f)), contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = bg, modifier = Modifier.size(20.dp))
            }
            Text(title, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface, lineHeight = 18.sp)
        }
    }
}
