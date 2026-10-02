package com.agroroute.features.farmer.shell

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.hilt.navigation.compose.hiltViewModel
import com.agroroute.R
import com.agroroute.core.navigation.Screen
import com.agroroute.features.auth.AuthViewModel

// Premium Fresh Green Theme Palette
private val PrimaryGreen = Color(0xFF2E7D32)
private val DarkGreenText = Color(0xFF14532D)
private val AgroLeaf = Color(0xFF43A047)
private val SoftMint = Color(0xFFE8F5E9)
private val OrangeBadge = Color(0xFFF97316)
private val BorderColor = Color(0xFFE2E8F0)
private val TextSecondary = Color(0xFF64748B)

private data class TopBarNotification(
    val id: String,
    val title: String,
    val message: String,
    val time: String,
    val icon: ImageVector,
    val iconColor: Color,
    var isUnread: Boolean = true
)

@OptIn(ExperimentalMaterial3Api::class)
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

    var showNotificationSheet by remember { mutableStateOf(false) }
    var showAvatarMenu by remember { mutableStateOf(false) }

    // Mock Notifications List
    val notifications = remember {
        mutableStateListOf(
            TopBarNotification(
                id = "1",
                title = "New bid received",
                message = "Supplier A placed a bid on your Tomato harvest.",
                time = "2m ago",
                icon = Icons.Default.Gavel,
                iconColor = PrimaryGreen,
                isUnread = true
            ),
            TopBarNotification(
                id = "2",
                title = "Order accepted",
                message = "Your Carrot order is confirmed for pickup.",
                time = "1h ago",
                icon = Icons.Default.CheckCircle,
                iconColor = AgroLeaf,
                isUnread = true
            ),
            TopBarNotification(
                id = "3",
                title = "Pickup scheduled",
                message = "Driver assigned for order #1029.",
                time = "3h ago",
                icon = Icons.Default.Receipt,
                iconColor = OrangeBadge,
                isUnread = true
            )
        )
    }

    val unreadCount = notifications.count { it.isUnread }

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = Color.White,
        shadowElevation = 4.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
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
                // ─── Left Section: Logo + Green Title / Back Button ───
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    if (!isTab) {
                        IconButton(
                            onClick = { navController.popBackStack() },
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(SoftMint)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = PrimaryGreen,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                    } else {
                        // App Logo Branding
                        Image(
                            painter = painterResource(id = R.drawable.logo),
                            contentDescription = "AgroRoute Logo",
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(Modifier.width(10.dp))
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        if (isTab) {
                            // Fresh Green Brand Title Text
                            Text(
                                text = "AgroRoute",
                                style = TextStyle(
                                    brush = Brush.linearGradient(
                                        colors = listOf(
                                            DarkGreenText,
                                            PrimaryGreen,
                                            AgroLeaf
                                        )
                                    ),
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 22.sp,
                                    letterSpacing = (-0.3).sp
                                )
                            )
                        } else {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.2.sp
                                ),
                                color = DarkGreenText,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (subtitle != null) {
                                Text(
                                    text = subtitle,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }

                // ─── Right Section: Notifications + Profile Icon ───
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Notification Bell Button with Orange Badge
                    AnimatedIconButton(
                        onClick = { showNotificationSheet = true }
                    ) {
                        Box(contentAlignment = Alignment.TopEnd) {
                            Icon(
                                imageVector = Icons.Default.NotificationsNone,
                                contentDescription = "Notifications",
                                tint = DarkGreenText,
                                modifier = Modifier.size(25.dp)
                            )
                            if (unreadCount > 0) {
                                Box(
                                    modifier = Modifier
                                        .offset(x = 4.dp, y = (-4).dp)
                                        .background(OrangeBadge, CircleShape)
                                        .border(1.5.dp, Color.White, CircleShape)
                                        .padding(horizontal = 4.dp, vertical = 1.dp)
                                ) {
                                    Text(
                                        text = if (unreadCount > 9) "9+" else "$unreadCount",
                                        color = Color.White,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    // Profile Icon Avatar Menu Button
                    Box {
                        Surface(
                            onClick = { showAvatarMenu = true },
                            shape = CircleShape,
                            modifier = Modifier
                                .size(40.dp)
                                .shadow(2.dp, CircleShape),
                            color = Color.White,
                            border = androidx.compose.foundation.BorderStroke(
                                1.5.dp,
                                Brush.linearGradient(
                                    listOf(
                                        PrimaryGreen,
                                        AgroLeaf
                                    )
                                )
                            )
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(SoftMint)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = "Profile",
                                    tint = PrimaryGreen,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }

                        // Avatar Popup Dropdown Menu
                        DropdownMenu(
                            expanded = showAvatarMenu,
                            onDismissRequest = { showAvatarMenu = false },
                            modifier = Modifier
                                .background(Color.White, RoundedCornerShape(16.dp))
                                .border(1.dp, BorderColor, RoundedCornerShape(16.dp))
                        ) {
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Person, contentDescription = null, tint = PrimaryGreen, modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.width(12.dp))
                                        Text("Profile", fontWeight = FontWeight.SemiBold)
                                    }
                                },
                                onClick = {
                                    showAvatarMenu = false
                                    navController.navigate(Screen.FarmerProfile.route)
                                }
                            )
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Settings, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.width(12.dp))
                                        Text("Settings", fontWeight = FontWeight.SemiBold)
                                    }
                                },
                                onClick = { showAvatarMenu = false }
                            )
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.HelpOutline, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.width(12.dp))
                                        Text("Help & Support", fontWeight = FontWeight.SemiBold)
                                    }
                                },
                                onClick = { showAvatarMenu = false }
                            )
                            HorizontalDivider(color = BorderColor.copy(alpha = 0.5f))
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Logout, contentDescription = null, tint = Color.Red, modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.width(12.dp))
                                        Text("Logout", color = Color.Red, fontWeight = FontWeight.Bold)
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

            HorizontalDivider(color = BorderColor.copy(alpha = 0.5f), thickness = 0.8.dp)
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

    // ─── Notification Bottom Sheet ───
    if (showNotificationSheet) {
        ModalBottomSheet(
            onDismissRequest = { showNotificationSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = Color.White,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(SoftMint, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = null,
                            tint = PrimaryGreen,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Notifications",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = DarkGreenText
                        )
                        Text(
                            text = if (unreadCount > 0) "$unreadCount unread" else "All caught up",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                    if (unreadCount > 0) {
                        TextButton(onClick = {
                            notifications.forEach { it.isUnread = false }
                        }) {
                            Text(
                                text = "Mark all read",
                                color = PrimaryGreen,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                HorizontalDivider(color = BorderColor.copy(alpha = 0.5f))

                if (notifications.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No notifications yet", color = TextSecondary)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(280.dp)
                    ) {
                        items(notifications) { item ->
                            NotificationTile(
                                item = item,
                                onClick = { item.isUnread = false }
                            )
                            HorizontalDivider(color = BorderColor.copy(alpha = 0.3f), modifier = Modifier.padding(start = 68.dp))
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                Box(modifier = Modifier.padding(horizontal = 20.dp)) {
                    TextButton(
                        onClick = { showNotificationSheet = false },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Close", color = TextSecondary, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

// ─── Single Notification Row Component ───
@Composable
private fun NotificationTile(
    item: TopBarNotification,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        color = if (item.isUnread) SoftMint.copy(alpha = 0.35f) else Color.White
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .background(item.iconColor.copy(alpha = 0.12f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = item.icon,
                    contentDescription = null,
                    tint = item.iconColor,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = if (item.isUnread) FontWeight.Bold else FontWeight.SemiBold
                        ),
                        color = Color(0xFF0F172A)
                    )
                    if (item.isUnread) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(OrangeBadge, CircleShape)
                        )
                    }
                }
                Spacer(Modifier.height(2.dp))
                Text(
                    text = item.message,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = item.time,
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary.copy(alpha = 0.7f),
                    fontSize = 10.sp
                )
            }
        }
    }
}

// ─── Animated Pressable TopBar Button ───
@Composable
private fun AnimatedIconButton(
    onClick: () -> Unit,
    content: @Composable () -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.88f else 1f,
        animationSpec = tween(120),
        label = "btnScale"
    )

    Box(
        modifier = Modifier
            .scale(scale)
            .clip(CircleShape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(6.dp),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

// Preserved internal route logic & functions
internal val farmerTabRoutes = setOf(
    Screen.FarmerHome.route,
    Screen.MyListings.route,
    Screen.FarmerBids.route,
    Screen.FarmerOrders.route,
    Screen.FarmerProfile.route
)

internal val farmerRoutes = setOf(
    *farmerTabRoutes.toTypedArray(),
    Screen.HarvestCalendar.route,
    Screen.CreateListing.route,
    Screen.ViewBids.route,
    Screen.FarmerOrderDetail.route
)

internal fun farmerTitle(route: String): String = when {
    route == Screen.FarmerHome.route -> "Home"
    route == Screen.MyListings.route -> "My Listings"
    route == Screen.FarmerBids.route -> "Bids"
    route == Screen.HarvestCalendar.route -> "Harvest Calendar"
    route == Screen.FarmerOrders.route -> "Orders"
    route == Screen.FarmerProfile.route -> "Profile"
    route == Screen.CreateListing.route -> "Create Listing"
    route.startsWith(Screen.ViewBids.route.substringBefore("{")) -> "Bids Received"
    route.startsWith(Screen.FarmerOrderDetail.route.substringBefore("{")) -> "Order Details"
    else -> "Farmer"
}

private fun farmerSubtitle(route: String): String? = when {
    route == Screen.FarmerHome.route -> "Farm command center"
    route == Screen.MyListings.route -> "Harvest posts & bids"
    route == Screen.FarmerBids.route -> "Incoming supplier offers"
    route == Screen.HarvestCalendar.route -> "Upcoming harvest dates"
    route == Screen.FarmerOrders.route -> "Pickups & deliveries"
    route == Screen.FarmerProfile.route -> "Account settings"
    route == Screen.CreateListing.route -> "Publish produce"
    route.startsWith(Screen.ViewBids.route.substringBefore("{")) -> "Best offer"
    route.startsWith(Screen.FarmerOrderDetail.route.substringBefore("{")) -> "Order progress"
    else -> null
}