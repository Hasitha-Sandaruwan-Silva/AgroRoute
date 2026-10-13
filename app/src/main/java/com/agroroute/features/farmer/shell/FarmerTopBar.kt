package com.agroroute.features.farmer.shell

import android.app.Activity
import android.os.Build
import android.view.WindowManager
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.agroroute.core.runtime.appViewModel as hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.agroroute.R
import com.agroroute.core.navigation.Screen
import com.agroroute.features.settings.NotificationBell
import com.agroroute.core.components.UserProfileAvatar
import com.agroroute.features.auth.AuthViewModel
import com.github.thelacspace.icons.Lucide

@Composable
fun FarmerTopBar(
    currentRoute: String,
    navController: NavController,
    modifier: Modifier = Modifier
) {
    val authViewModel: AuthViewModel = hiltViewModel()
    val authState by authViewModel.uiState.collectAsStateWithLifecycle()
    val isTab = currentRoute in farmerTabRoutes
    val title = farmerTitle(currentRoute)
    val subtitle = farmerSubtitle(currentRoute)

    // WIFI/BATTERY/TIME 100% VISIBLE - LIGHT MODE FIX - BASED ON SURFACE LUMINANCE
    val view = LocalView.current
    val isDarkTheme = isSystemInDarkTheme()
    val surfaceColor = MaterialTheme.colorScheme.surface
    // Use luminance to decide icon color - light surface = dark icons, dark surface = light icons
    val isSurfaceLight = surfaceColor.luminance() > 0.5f
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = isSurfaceLight
                isAppearanceLightNavigationBars = isSurfaceLight
            }
        }
    }

    var showAvatarMenu by remember { mutableStateOf(false) }

    Surface(modifier = modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.surface, shadowElevation = 0.dp) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Spacer(Modifier.windowInsetsTopHeight(WindowInsets.statusBars).background(MaterialTheme.colorScheme.surface))
            Row(
                modifier = Modifier.fillMaxWidth().height(64.dp).padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    if (!isTab) {
                        IconButton(onClick = { navController.popBackStack() }, modifier = Modifier.size(48.dp)) {
                            Icon(imageVector = Lucide.ArrowLeft, contentDescription = "Back", tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(22.dp))
                        }
                        Spacer(Modifier.width(4.dp))
                    } else {
                        Image(painter = painterResource(id = R.drawable.logo), contentDescription = "Logo", modifier = Modifier.size(32.dp))
                        Spacer(Modifier.width(10.dp))
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        if (isTab) {
                            Text("AgroRoute", fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, letterSpacing = (-0.3).sp, color = MaterialTheme.colorScheme.primary)
                            Text("FARMER", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        } else {
                            Text(title, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            if (subtitle != null) {
                                Text(subtitle, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    NotificationBell(navController = navController)

                    Box {
                        Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                            UserProfileAvatar(authState.user?.displayName ?: "Farmer", authState.user?.profileImageUrl,
                                size = 40.dp, onClick = { showAvatarMenu = true })
                        }
                        DropdownMenu(expanded = showAvatarMenu, onDismissRequest = { showAvatarMenu = false }) {
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(imageVector = Lucide.User, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                        Spacer(Modifier.width(12.dp))
                                        Text("Profile", fontWeight = FontWeight.SemiBold)
                                    }
                                },
                                onClick = { showAvatarMenu = false; navController.navigate(Screen.FarmerProfile.route) }
                            )
                            HorizontalDivider(Modifier.padding(horizontal = 12.dp, vertical = 4.dp))
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(imageVector = Lucide.Logout, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
                                        Spacer(Modifier.width(12.dp))
                                        Text("Logout", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                                    }
                                },
                                onClick = {
                                    showAvatarMenu = false
                                    authViewModel.logout { navController.navigate(Screen.Welcome.route) { popUpTo(0) { inclusive = true } } }
                                }
                            )
                        }
                    }
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), thickness = 1.dp)
        }
    }

    authState.error?.let { error ->
        AlertDialog(
            onDismissRequest = authViewModel::clearError,
            title = { Text("Could not sign out") },
            text = { Text(error) },
            confirmButton = { TextButton(onClick = authViewModel::clearError) { Text("OK") } }
        )
    }
}

internal val farmerTabRoutes = setOf(Screen.FarmerHome.route, Screen.MyListings.route, Screen.FarmerOrders.route, Screen.FarmerProfile.route)
internal val farmerRoutes = setOf(
    *farmerTabRoutes.toTypedArray(), Screen.FarmerBids.route, Screen.HarvestCalendar.route,
    Screen.CreateListing.route, Screen.EditListing.route, Screen.ViewBids.route,
    Screen.FarmerOrderDetail.route, Screen.FarmerOrderRequests.route, Screen.FarmerLeaderboard.route,
    Screen.SubmitReview.route, Screen.HelpSupport.route, Screen.About.route, Screen.Privacy.route
)
internal fun farmerTitle(route: String): String = when {
    route == Screen.FarmerHome.route -> "Home"
    route == Screen.MyListings.route -> "My Listings"
    route == Screen.FarmerBids.route -> "Bids"
    route == Screen.HarvestCalendar.route -> "Harvest Calendar"
    route == Screen.FarmerOrders.route -> "Orders"
    route == Screen.FarmerOrderRequests.route -> "Order Requests"
    route == Screen.FarmerProfile.route -> "Profile"
    route == Screen.FarmerLeaderboard.route -> "Farmer Leaderboard"
    route == Screen.CreateListing.route -> "Create Listing"
    route == Screen.HelpSupport.route -> "Help & Support"
    route == Screen.About.route -> "About"
    route == Screen.Privacy.route -> "Privacy & Terms"
    route.startsWith(Screen.EditListing.route.substringBefore("{")) -> "Edit Listing"
    route.startsWith(Screen.ViewBids.route.substringBefore("{")) -> "Bids Received"
    route.startsWith(Screen.FarmerOrderDetail.route.substringBefore("{")) -> "Order Details"
    route.startsWith(Screen.SubmitReview.route.substringBefore("{")) -> "Submit Review"
    else -> "Farmer"
}
private fun farmerSubtitle(route: String): String? = when {
    route == Screen.FarmerHome.route -> "Farm command center"
    route == Screen.MyListings.route -> "Harvest posts & bids"
    route == Screen.FarmerBids.route -> "Incoming supplier offers"
    route == Screen.HarvestCalendar.route -> "Upcoming harvest dates"
    route == Screen.FarmerOrders.route -> "Pickups & deliveries"
    route == Screen.FarmerOrderRequests.route -> "Direct order requests"
    route == Screen.FarmerProfile.route -> "Account settings"
    route == Screen.FarmerLeaderboard.route -> "Top ranked farmers by points"
    route == Screen.CreateListing.route -> "Publish produce"
    route.startsWith(Screen.EditListing.route.substringBefore("{")) -> "Update listing details"
    route.startsWith(Screen.ViewBids.route.substringBefore("{")) -> "Best offer"
    route.startsWith(Screen.FarmerOrderDetail.route.substringBefore("{")) -> "Order progress"
    route.startsWith(Screen.SubmitReview.route.substringBefore("{")) -> "Rate transaction"
    else -> null
}
