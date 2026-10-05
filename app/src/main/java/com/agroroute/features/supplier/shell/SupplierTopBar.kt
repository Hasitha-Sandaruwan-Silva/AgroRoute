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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import com.github.thelacspace.icons.Lucide
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.graphics.Brush
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
fun SupplierTopBar(
    currentRoute: String,
    navController: NavController,
    modifier: Modifier = Modifier
) {
    val authViewModel: AuthViewModel = hiltViewModel()
    val authState by authViewModel.uiState.collectAsStateWithLifecycle()
    val isTab = currentRoute in supplierTabRoutes

    // ─── STATUS BAR ICONS කළු පැහැයට හැරවීම ───
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
        color = MaterialTheme.colorScheme.surface,
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
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f))
                        ) {
                            Icon(
                                imageVector = Lucide.ArrowLeft,
                                contentDescription = "Back",
                                tint = MaterialTheme.colorScheme.primary,
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
                            text = if (isTab) "AgroRoute" else supplierTitle(currentRoute),
                            style = if (isTab) {
                                // Fresh Premium Gradient Text Style
                                TextStyle(
                                    brush = Brush.linearGradient(
                                        colors = listOf(
                                            MaterialTheme.colorScheme.primary,
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.72f)
                                        )
                                    ),
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 22.sp,
                                    letterSpacing = (-0.3).sp
                                )
                            } else {
                                MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.2.sp
                                )
                            },
                            color = if (isTab) androidx.compose.ui.graphics.Color.Unspecified
                            else MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (isTab) {
                            Text(
                                text = supplierTitle(currentRoute),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                // ─── Right Section: Profile Avatar + Dropdown ───
                Box {
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

internal val supplierTabRoutes = setOf(
    Screen.SupplierHome.route,
    Screen.SupplierMarketplace.route,
    Screen.SupplierOrders.route,
    Screen.SupplierBids.route,
    Screen.SupplierProfile.route
)

internal val supplierRoutes = setOf(
    *supplierTabRoutes.toTypedArray(),
    Screen.PlaceBid.route,
    Screen.PlaceOrderRequest.route,
    Screen.SupplierOrderDetail.route,
    Screen.HelpSupport.route,
    Screen.About.route,
    Screen.Privacy.route
)

internal fun supplierTitle(route: String): String = when {
    route == Screen.SupplierHome.route -> "Home"
    route == Screen.SupplierMarketplace.route -> "Marketplace"
    route == Screen.SupplierOrders.route -> "Orders"
    route == Screen.SupplierBids.route -> "My Bids"
    route == Screen.SupplierProfile.route -> "Profile"
    route == Screen.HelpSupport.route -> "Help & Support"
    route == Screen.About.route -> "About"
    route == Screen.Privacy.route -> "Privacy & Terms"
    route.startsWith(Screen.PlaceBid.route.substringBefore("{")) -> "Place Bid"
    route.startsWith(Screen.PlaceOrderRequest.route.substringBefore("{")) -> "Place Order Request"
    route.startsWith(Screen.SupplierOrderDetail.route.substringBefore("{")) -> "Order Details"
    else -> "Supplier"
}
