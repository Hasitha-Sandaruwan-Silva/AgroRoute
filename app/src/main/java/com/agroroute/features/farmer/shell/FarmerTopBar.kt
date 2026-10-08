package com.agroroute.features.farmer.shell

import android.app.Activity
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
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
import kotlinx.coroutines.delay

@Composable
fun FarmerTopBar(
    currentRoute: String,
    navController: NavController,
    modifier: Modifier = Modifier,
    hasNewNotification: Boolean = true
) {
    val authViewModel: AuthViewModel = hiltViewModel()
    val authState by authViewModel.uiState.collectAsStateWithLifecycle()
    val isTab = currentRoute in farmerTabRoutes
    val title = farmerTitle(currentRoute)
    val subtitle = farmerSubtitle(currentRoute)

    val view = LocalView.current
    val isDarkTheme = isSystemInDarkTheme()
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !isDarkTheme
        }
    }

    var showAvatarMenu by remember { mutableStateOf(false) }

    // noti එකක් ආවම විතරක් shake
    var triggerShake by remember { mutableStateOf(0) }
    LaunchedEffect(hasNewNotification) { if (hasNewNotification) triggerShake++ }
    val shakeAnim = remember { Animatable(0f) }
    LaunchedEffect(triggerShake) {
        if (hasNewNotification && triggerShake > 0) {
            repeat(3) {
                shakeAnim.animateTo(12f, tween(80))
                shakeAnim.animateTo(-12f, tween(80))
            }
            shakeAnim.animateTo(0f, tween(80))
        }
    }

    Surface(modifier = modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.surface, shadowElevation = 1.dp) {
        Column(Modifier.fillMaxWidth().statusBarsPadding()) {
            Row(
                Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    if (!isTab) {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", modifier = Modifier.size(26.dp))
                        }
                    } else {
                        Image(
                            painter = painterResource(id = R.drawable.agroroute),
                            contentDescription = "Logo",
                            modifier = Modifier.size(52.dp).clip(CircleShape).clickable {
                                navController.navigate(Screen.FarmerHome.route) { popUpTo(Screen.FarmerHome.route) { inclusive = false }; launchSingleTop = true }
                            }
                        )
                        Spacer(Modifier.width(10.dp))
                    }
                    Column(Modifier.weight(1f)) {
                        if (isTab) {
                            Text("AgroRoute", style = TextStyle(fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, color = MaterialTheme.colorScheme.primary))
                        } else {
                            Text(title, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), maxLines = 1, overflow = TextOverflow.Ellipsis)
                            if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                        }
                    }
                }

                // ICONS ONLY - BG නෑ, ලොකුයි
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box {
                        IconButton(onClick = { /* noti screen */ }, modifier = Modifier.size(48.dp)) {
                            Icon(
                                Icons.Filled.Notifications,
                                "Notifications",
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(28.dp).rotate(if (hasNewNotification) shakeAnim.value else 0f)
                            )
                        }
                        if (hasNewNotification) {
                            Box(Modifier.align(Alignment.TopEnd).offset(x = (-6).dp, y = 6.dp).size(10.dp).clip(CircleShape).background(Color.Red))
                        }
                    }

                    Box {
                        IconButton(onClick = { showAvatarMenu = true }, modifier = Modifier.size(48.dp)) {
                            Icon(Icons.Filled.Person, "Profile", tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(28.dp))
                        }
                        DropdownMenu(expanded = showAvatarMenu, onDismissRequest = { showAvatarMenu = false }) {
                            DropdownMenuItem(text = { Text("Profile") }, onClick = { showAvatarMenu = false; navController.navigate(Screen.FarmerProfile.route) })
                            DropdownMenuItem(text = { Text("Logout", color = MaterialTheme.colorScheme.error) }, onClick = { showAvatarMenu = false; authViewModel.logout { navController.navigate(Screen.Welcome.route) { popUpTo(0) { inclusive = true } } } })
                        }
                    }
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(0.08f), thickness = 1.dp)
        }
    }

    authState.error?.let { error ->
        AlertDialog(onDismissRequest = authViewModel::clearError, title = { Text("Could not sign out") }, text = { Text(error) }, confirmButton = { TextButton(onClick = authViewModel::clearError) { Text("OK") } })
    }
}

internal val farmerTabRoutes = setOf(Screen.FarmerHome.route, Screen.MyListings.route, Screen.FarmerOrders.route, Screen.FarmerProfile.route)
internal val farmerRoutes = setOf(*farmerTabRoutes.toTypedArray(), Screen.FarmerBids.route, Screen.HarvestCalendar.route, Screen.CreateListing.route, Screen.EditListing.route, Screen.ViewBids.route, Screen.FarmerOrderDetail.route, Screen.FarmerOrderRequests.route, Screen.FarmerLeaderboard.route, Screen.SubmitReview.route, Screen.HelpSupport.route, Screen.About.route, Screen.Privacy.route)
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