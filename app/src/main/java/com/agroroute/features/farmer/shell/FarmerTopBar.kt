package com.agroroute.features.farmer.shell

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Agriculture
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.agroroute.core.navigation.Screen

@Composable
fun FarmerTopBar(
    currentRoute: String,
    navController: NavController,
    modifier: Modifier = Modifier,
    hasUnreadNotifications: Boolean = true,
    onNotificationClick: () -> Unit = {}
) {
    val isTab = currentRoute in farmerTabRoutes
    val title = farmerTitle(currentRoute)
    val subtitle = farmerSubtitle(currentRoute)

    val primaryColor = MaterialTheme.colorScheme.primary
    val surfaceColor = MaterialTheme.colorScheme.surface
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
    val outlineVariant = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .shadow(elevation = 2.dp, spotColor = primaryColor.copy(alpha = 0.1f)),
        color = surfaceColor,
        tonalElevation = 1.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
        ) {
            // Main Top Bar Content
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp)
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left Action: Back button OR App Emblem
                if (!isTab) {
                    Surface(
                        onClick = { navController.popBackStack() },
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier
                            .size(38.dp)
                            .border(1.dp, outlineVariant, CircleShape)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                contentDescription = "Back",
                                tint = onSurfaceColor,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                Brush.linearGradient(
                                    listOf(
                                        primaryColor,
                                        MaterialTheme.colorScheme.tertiary.takeOrElse { primaryColor.copy(alpha = 0.8f) }
                                    )
                                )
                            )
                            .border(1.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Agriculture,
                            contentDescription = "Farm Logo",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Title & Subtitle with smooth transition animation
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.Center
                ) {
                    AnimatedContent(
                        targetState = title,
                        transitionSpec = {
                            (slideInVertically { height -> height / 2 } + fadeIn()) togetherWith
                                    (slideOutVertically { height -> -height / 2 } + fadeOut())
                        },
                        label = "TitleAnimation"
                    ) { animatedTitle ->
                        Text(
                            text = animatedTitle,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.15.sp
                            ),
                            color = onSurfaceColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    if (subtitle != null) {
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            ),
                            color = onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Right Action: Notification Button (Only on Main Tabs)
                if (isTab) {
                    Surface(
                        onClick = onNotificationClick,
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier
                            .size(38.dp)
                            .border(1.dp, outlineVariant, RoundedCornerShape(12.dp))
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Rounded.Notifications,
                                contentDescription = "Notifications",
                                tint = onSurfaceColor.copy(alpha = 0.8f),
                                modifier = Modifier.size(20.dp)
                            )

                            // Unread Notification Dot Badge
                            if (hasUnreadNotifications) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .align(Alignment.TopEnd)
                                        .padding(top = 2.dp, end = 2.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.error)
                                        .border(1.5.dp, surfaceColor, CircleShape)
                                )
                            }
                        }
                    }
                }
            }

            // Sleek bottom border divider
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(outlineVariant)
            )
        }
    }
}

// Helper to fallback if tertiary isn't explicitly defined
private inline fun Color.takeOrElse(block: () -> Color): Color =
    if (this != Color.Unspecified) this else block()

internal val farmerTabRoutes = setOf(
    Screen.FarmerHome.route,
    Screen.MyListings.route,
    Screen.FarmerOrders.route,
    Screen.FarmerProfile.route
)

internal val farmerRoutes = setOf(
    *farmerTabRoutes.toTypedArray(),
    Screen.CreateListing.route,
    Screen.ViewBids.route,
    Screen.FarmerOrderDetail.route
)

internal fun farmerTitle(route: String): String = when {
    route == Screen.FarmerHome.route -> "Home"
    route == Screen.MyListings.route -> "My Listings"
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
    route == Screen.FarmerOrders.route -> "Pickups & deliveries"
    route == Screen.FarmerProfile.route -> "Account settings"
    route == Screen.CreateListing.route -> "Publish fresh produce"
    route.startsWith(Screen.ViewBids.route.substringBefore("{")) -> "Review buyer offers"
    route.startsWith(Screen.FarmerOrderDetail.route.substringBefore("{")) -> "Track order status"
    else -> null
}