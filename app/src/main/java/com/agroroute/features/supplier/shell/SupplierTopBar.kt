package com.agroroute.features.supplier.shell

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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import com.github.thelacspace.icons.Lucide
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
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

@Composable
fun SupplierTopBar(
    currentRoute: String,
    navController: NavController,
    modifier: Modifier = Modifier
) {
    val authViewModel: AuthViewModel = hiltViewModel()
    val authState by authViewModel.uiState.collectAsStateWithLifecycle()
    val isTab = currentRoute in supplierTabRoutes

    var showAvatarMenu by remember { mutableStateOf(false) }

    Surface(
        modifier = modifier.fillMaxWidth().testTag("supplier-shared-top-bar"),
        color = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        shadowElevation = 0.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 64.dp)
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
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
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                        ) {
                            Icon(
                                imageVector = Lucide.ArrowLeft,
                                contentDescription = "Back",
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                    } else {
                        Image(
                            painter = painterResource(id = R.drawable.logo),
                            contentDescription = "AgroRoute logo",
                            modifier = Modifier.size(38.dp)
                        )
                        Spacer(Modifier.width(12.dp))
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = supplierTitle(currentRoute),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (isTab) {
                            Text(
                                text = "AgroRoute · Supplier",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                NotificationBell(navController = navController, tint = MaterialTheme.colorScheme.onSurface)

                // ─── Right Section: Profile Avatar + Dropdown ───
                Box {
                    Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                        UserProfileAvatar(authState.user?.displayName ?: "Supplier", authState.user?.profileImageUrl,
                            size = 40.dp, onClick = { showAvatarMenu = true })
                    }

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
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.height(24.dp)
                                ) {
                                    Icon(
                                        Lucide.User,
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
                                navController.navigate(Screen.SupplierProfile.route)
                            }
                        )

                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                        )

                        DropdownMenuItem(
                            text = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.height(24.dp)
                                ) {
                                    Icon(
                                        Lucide.Logout,
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

            // Subtle divider at the bottom
            HorizontalDivider(
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
                thickness = 1.dp
            )
        }
    }

    authState.error?.let { error ->
        AlertDialog(
            onDismissRequest = authViewModel::clearError,
            title = { Text("Could not sign out") },
            text = { Text(com.agroroute.core.components.presentationError(error)) },
            confirmButton = {
                TextButton(onClick = authViewModel::clearError) {
                    Text("OK")
                }
            }
        )
    }
}

internal val supplierTabRoutes = setOf(
    Screen.SupplierHome.route,
    Screen.SupplierMarketplace.route,
    Screen.SupplierOrders.route,
    Screen.SupplierProfile.route
)

internal val supplierRoutes = setOf(
    *supplierTabRoutes.toTypedArray(),
    Screen.SupplierBids.route,
    Screen.PlaceBid.route,
    Screen.PlaceOrderRequest.route,
    Screen.SupplierOrderDetail.route,
    Screen.SupplierLeaderboard.route,
    Screen.SubmitReview.route,
    Screen.HelpSupport.route,
    Screen.About.route,
    Screen.Privacy.route,
    Screen.NotificationInbox.route,
    Screen.NotificationSettings.route
)

internal fun supplierTitle(route: String): String = when {
    route == Screen.SupplierHome.route -> "Home"
    route == Screen.SupplierMarketplace.route -> "Marketplace"
    route == Screen.SupplierOrders.route -> "Orders"
    route == Screen.SupplierBids.route -> "My Bids"
    route == Screen.SupplierProfile.route -> "Profile"
    route == Screen.SupplierLeaderboard.route -> "Leaderboard"
    route == Screen.HelpSupport.route -> "Help & Support"
    route == Screen.About.route -> "About"
    route == Screen.Privacy.route -> "Privacy & Terms"
    route == Screen.NotificationInbox.route -> "Notifications"
    route == Screen.NotificationSettings.route -> "Notification Settings"
    route.startsWith(Screen.PlaceBid.route.substringBefore("{")) -> "Place Bid"
    route.startsWith(Screen.PlaceOrderRequest.route.substringBefore("{")) -> "Place Order Request"
    route.startsWith(Screen.SupplierOrderDetail.route.substringBefore("{")) -> "Order Details"
    route.startsWith(Screen.SubmitReview.route.substringBefore("{")) -> "Submit Review"
    else -> "Supplier"
}
