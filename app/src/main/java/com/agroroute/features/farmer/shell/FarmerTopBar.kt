package com.agroroute.features.farmer.shell

import android.app.Activity
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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

    // ─── STATUS BAR ICONS DYNAMIC COLOR ───
    val view = LocalView.current
    val isDarkTheme = isSystemInDarkTheme()

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            // Light theme = dark icons, Dark theme = light icons
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !isDarkTheme
        }
    }

    var showAvatarMenu by remember { mutableStateOf(false) }

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface, // Automatically adapts to Dark/Light mode
        shadowElevation = if (isTab) 2.dp else 4.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(68.dp)
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // ─── Left Section: Logo + Title / Back Button ───
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    if (!isTab) {
                        IconButton(
                            onClick = { navController.popBackStack() },
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                    } else {
                        // App Logo
                        Image(
                            painter = painterResource(id = R.drawable.logo),
                            contentDescription = "AgroRoute Logo",
                            modifier = Modifier.size(38.dp)
                        )
                        Spacer(Modifier.width(12.dp))
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        if (isTab) {
                            // Brand Title
                            Text(
                                text = "AgroRoute",
                                style = TextStyle(
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 22.sp,
                                    letterSpacing = (-0.3).sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            )
                        } else {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.2.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (subtitle != null) {
                                Text(
                                    text = subtitle,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }

                // ─── Right Section: Beautiful Profile Icon ───
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box {
                        // Premium Profile Avatar
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer)
                                .clickable { showAvatarMenu = true }
                                .border(
                                    width = 2.dp,
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = "Profile",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        // Avatar Popup Dropdown Menu
                        DropdownMenu(
                            expanded = showAvatarMenu,
                            onDismissRequest = { showAvatarMenu = false },
                            modifier = Modifier
                                .background(
                                    color = MaterialTheme.colorScheme.surface,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .padding(vertical = 4.dp)
                        ) {
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Default.Person,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(Modifier.width(12.dp))
                                        Text(
                                            text = "Profile",
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                },
                                onClick = {
                                    showAvatarMenu = false
                                    navController.navigate(Screen.FarmerProfile.route)
                                }
                            )

                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                            )

                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Default.Logout,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(Modifier.width(12.dp))
                                        Text(
                                            text = "Logout",
                                            color = MaterialTheme.colorScheme.error,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                },
                                onClick = {
                                    showAvatarMenu = false
                                    authViewModel.logout {
                                        navController.navigate(Screen.Welcome.route) {
                                            popUpTo(0) { inclusive = true }
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            }

            // Subtle divider at the bottom
            HorizontalDivider(
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                thickness = 1.dp
            )
        }
    }

    authState.error?.let { error ->
        AlertDialog(
            onDismissRequest = authViewModel::clearError,
            title = { Text("Could not sign out") },
            text = { Text(error) },
            confirmButton = {
                TextButton(onClick = authViewModel::clearError) {
                    Text("OK")
                }
            }
        )
    }
}

// ─── Preserved internal route logic & functions (No Changes Here) ───

internal val farmerTabRoutes = setOf(
    Screen.FarmerHome.route,
    Screen.MyListings.route,
    Screen.FarmerOrders.route,
    Screen.FarmerProfile.route
)

internal val farmerRoutes = setOf(
    *farmerTabRoutes.toTypedArray(),
    Screen.FarmerBids.route,
    Screen.HarvestCalendar.route,
    Screen.CreateListing.route,
    Screen.EditListing.route,
    Screen.ViewBids.route,
    Screen.FarmerOrderDetail.route,
    Screen.FarmerOrderRequests.route,
    Screen.FarmerLeaderboard.route,
    Screen.SubmitReview.route,
    Screen.HelpSupport.route,
    Screen.About.route,
    Screen.Privacy.route,
    Screen.NotificationInbox.route,
    Screen.NotificationSettings.route
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
    route == Screen.NotificationInbox.route -> "Notifications"
    route == Screen.NotificationSettings.route -> "Notification Settings"
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