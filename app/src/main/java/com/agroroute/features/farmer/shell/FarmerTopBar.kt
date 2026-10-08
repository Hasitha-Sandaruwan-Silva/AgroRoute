package com.agroroute.features.farmer.shell

import android.app.Activity
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.agroroute.R
import com.agroroute.core.navigation.Screen
import com.agroroute.features.auth.AuthViewModel
import com.github.thelacspace.icons.Lucide

private data class FarmerNotification(val id: String, val title: String, val subtitle: String, val time: String, val isRead: Boolean = false)

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

    // STATUS BAR ICONS - FIX WIFI/TIME/BATTERY NOT VISIBLE - 100% FIXED
    val view = LocalView.current
    val isDarkTheme = isSystemInDarkTheme()
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            // Make status bar transparent so icons are visible over app content
            window.statusBarColor = android.graphics.Color.TRANSPARENT
            // Light surface = dark icons (visible on white), Dark surface = light icons (visible on dark)
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !isDarkTheme
                isAppearanceLightNavigationBars = !isDarkTheme
            }
        }
    }

    var showAvatarMenu by remember { mutableStateOf(false) }
    var showNotifications by remember { mutableStateOf(false) }
    var notifications by remember {
        mutableStateOf(
            listOf(
                FarmerNotification("1", "New bid received!", "Supplier offered Rs. 450 for Tomatoes", "2m ago"),
                FarmerNotification("2", "Order confirmed", "Your Basmati Rice order #1234 confirmed", "1h ago"),
                FarmerNotification("3", "Harvest reminder", "Tomatoes harvest in 2 days", "3h ago", true)
            )
        )
    }
    val unreadCount = notifications.count { !it.isRead }

    Surface(modifier = modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.surface, shadowElevation = 0.dp) {
        Column(modifier = Modifier.fillMaxWidth().statusBarsPadding()) {
            Row(
                modifier = Modifier.fillMaxWidth().height(64.dp).padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    if (!isTab) {
                        IconButton(onClick = { navController.popBackStack() }, modifier = Modifier.size(40.dp)) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(22.dp))
                        }
                        Spacer(Modifier.width(4.dp))
                    } else {
                        Image(painter = painterResource(id = R.drawable.logo), contentDescription = "Logo", modifier = Modifier.size(32.dp))
                        Spacer(Modifier.width(10.dp))
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        if (isTab) {
                            Text("AgroRoute", fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, letterSpacing = (-0.3).sp, color = MaterialTheme.colorScheme.primary)
                        } else {
                            Text(title, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            if (subtitle != null) {
                                Text(subtitle, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    // NOTIFICATION - NO BG, NO CUT, NO FREEZE - FIXED WITH DIALOG
                    IconButton(onClick = { showNotifications = true }, modifier = Modifier.size(44.dp)) {
                        BadgedBox(
                            badge = {
                                if (unreadCount > 0) {
                                    Badge(containerColor = Color(0xFFD32F2F), contentColor = Color.White) {
                                        Text("$unreadCount", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        ) {
                            Icon(Lucide.Bell, contentDescription = "Notifications", tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(22.dp))
                        }
                    }

                    // PROFILE - NO BG
                    Box {
                        IconButton(onClick = { showAvatarMenu = true }, modifier = Modifier.size(44.dp)) {
                            Icon(Icons.Default.Person, "Profile", tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(24.dp))
                        }
                        DropdownMenu(expanded = showAvatarMenu, onDismissRequest = { showAvatarMenu = false }) {
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Person, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
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
                                        Icon(Icons.Default.Logout, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
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

    // NOTIFICATION DIALOG - NO FREEZE, NO CUT - FIXED
    if (showNotifications) {
        AlertDialog(
            onDismissRequest = { showNotifications = false },
            modifier = Modifier.widthIn(max = 380.dp),
            title = {
                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                    Text("Notifications", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
                    if (unreadCount > 0) {
                        TextButton(onClick = { notifications = notifications.map { it.copy(isRead = true) } }) {
                            Text("Mark all read", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            },
            text = {
                Column(Modifier.fillMaxWidth().heightIn(max = 400.dp).verticalScroll(rememberScrollState())) {
                    if (notifications.isEmpty()) {
                        Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Lucide.Bell, null, Modifier.size(32.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(Modifier.height(8.dp))
                                Text("No notifications", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    } else {
                        notifications.forEach { n ->
                            Row(
                                Modifier.fillMaxWidth().clickable { notifications = notifications.map { if (it.id == n.id) it.copy(isRead = true) else it } }
                                    .padding(vertical = 12.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Icon(
                                    when {
                                        n.title.contains("bid", true) -> Lucide.Gavel
                                        n.title.contains("order", true) -> Lucide.Receipt
                                        else -> Lucide.Sprout
                                    },
                                    null,
                                    tint = if (!n.isRead) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp).padding(top = 2.dp)
                                )
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                                        Text(n.title, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1, modifier = Modifier.weight(1f))
                                        Text(n.time, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    Spacer(Modifier.height(2.dp))
                                    Text(n.subtitle, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                }
                                if (!n.isRead) {
                                    Spacer(Modifier.width(8.dp))
                                    Box(Modifier.size(8.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary).align(Alignment.CenterVertically))
                                }
                            }
                            HorizontalDivider(Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showNotifications = false }) { Text("Close", fontWeight = FontWeight.Bold) } }
        )
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
