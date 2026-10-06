package com.agroroute.features.farmer.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.agroroute.core.model.ListingStatus
import com.agroroute.core.navigation.Screen
import com.agroroute.core.theme.ForestDeep
import com.agroroute.core.theme.ForestMid
import com.agroroute.core.theme.ForestSoft
import com.agroroute.core.theme.GreenDeep
import com.agroroute.core.theme.GreenEmerald
import com.agroroute.core.theme.PureWhite
import com.agroroute.core.theme.SoftMintBg
import com.agroroute.core.theme.TextSecondary
import com.agroroute.core.widgets.AppCard
import com.agroroute.core.widgets.AgroSnackbarHost
import com.agroroute.core.widgets.ErrorSnackbarEffect
import com.agroroute.core.widgets.ShimmerBox
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

// Custom Colors matching the UI design
private val DarkGreenBg = ForestDeep
private val CardGreen = ForestMid
private val AccentGreen = GreenEmerald
private val LightBg = SoftMintBg
private val TextGray = TextSecondary

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

    // ✅ LOGIC EXACTLY UNCHANGED
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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(LightBg)
    ) {
        // Top Dark Green Background Shape
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)
                .clip(RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp))
                .background(DarkGreenBg)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // ── Header Section ─────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 24.dp)
                    .padding(top = 16.dp), // Extra top padding for status bar if needed
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Dummy Profile Picture
                Surface(
                    shape = CircleShape,
                    color = AccentGreen,
                    modifier = Modifier.size(48.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Lucide.User, contentDescription = null, tint = Color.White)
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Hi, Farmer",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                    Text(
                        text = "Welcome back!",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                // Notification Bell
                Surface(
                    shape = CircleShape,
                    color = CardGreen,
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Lucide.Bell,
                            contentDescription = "Notifications",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // ── Hero Overview Card (Weather Style) ─────────────────────────────────────────────
            Box(modifier = Modifier.padding(horizontal = 24.dp)) {
                if (isLoading) {
                    ShimmerBox(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp),
                        cornerRadius = 24.dp
                    )
                } else {
                    HeroStatCard(
                        activeListings = activeListings.toString(),
                        totalListings = totalListings.toString(),
                        totalOrders = totalOrders.toString()
                    )
                }
            }

            Spacer(Modifier.height(12.dp))
            Box(modifier = Modifier.padding(horizontal = 24.dp)) {
                AppCard(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { navController.navigate(Screen.FarmerLeaderboard.route) }
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Lucide.Trophy,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Your points", style = MaterialTheme.typography.labelMedium)
                            Text(
                                "${authState.user?.points ?: 0} pts",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text("Farmer leaderboard", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }

            Spacer(Modifier.height(32.dp))

            // ── Navigation Shortcuts (Pill Filter Style) ─────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Sell and fulfill",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = GreenDeep
                )
            }

            Spacer(Modifier.height(16.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ChipItem("Orders", isActive = true) { navController.navigate(FarmerHomeDestination.ORDERS.route) }
                ChipItem("Requests", isActive = false) { navController.navigate(FarmerHomeDestination.ORDER_REQUESTS.route) }
                ChipItem("My Listings", isActive = false) { navController.navigate(FarmerHomeDestination.MY_LISTINGS.route) }
                ChipItem("Calendar", isActive = false) { navController.navigate(FarmerHomeDestination.HARVEST_CALENDAR.route) }
            }

            Spacer(Modifier.height(24.dp))

            // ── Action Cards (Large Card Style) ─────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Card 1: Create Listing
                ActionLargeCard(
                    modifier = Modifier.weight(1f),
                    title = "New Listing",
                    subtitle = "List harvest for buyers",
                    icon = Lucide.Plus,
                    iconBgColor = AccentGreen,
                    onClick = { navController.navigate(FarmerHomeDestination.CREATE_LISTING.route) }
                )

                ActionLargeCard(
                    modifier = Modifier.weight(1f),
                    title = "Manage Orders",
                    subtitle = "Review requests and fulfillment",
                    icon = Lucide.Receipt,
                    iconBgColor = ForestSoft,
                    onClick = { navController.navigate(FarmerHomeDestination.ORDERS.route) }
                )
            }

            Spacer(Modifier.height(40.dp))
        }

        AgroSnackbarHost(
            snackbarHostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

// ──────────────────────────────────────────────────────────────────────────
// Custom UI Components matching the design
// ──────────────────────────────────────────────────────────────────────────

@Composable
private fun HeroStatCard(
    activeListings: String,
    totalListings: String,
    totalOrders: String
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = CardGreen),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(24.dp)
        ) {
            // Top Row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Lucide.MapPin,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Order Overview",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White
                    )
                }
                Icon(
                    imageVector = Lucide.Leaf, // Changed from Sprout to Leaf to prevent errors
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.5f),
                    modifier = Modifier.size(32.dp)
                )
            }

            Spacer(Modifier.height(16.dp))

            // Middle Row (Big Stat)
            Row(
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    text = totalOrders,
                    fontSize = 56.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.alignByBaseline()
                )
                Spacer(Modifier.width(8.dp))
                Column(modifier = Modifier.alignByBaseline()) {
                    Text(
                        text = "Orders",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                    Text(
                        text = "To fulfill",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White
                    )
                }
            }

            Spacer(Modifier.height(24.dp))
            Divider(color = Color.White.copy(alpha = 0.1f), thickness = 1.dp)
            Spacer(Modifier.height(16.dp))

            // Bottom Row (Small Stats)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StatItem(label = "Active listings", value = activeListings)
                StatItem(label = "All listings", value = totalListings)
            }
        }
    }
}

@Composable
private fun StatItem(label: String, value: String) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = TextGray
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
    }
}

@Composable
private fun ChipItem(
    text: String,
    isActive: Boolean,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(50),
        color = if (isActive) AccentGreen else PureWhite,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .clickable(onClick = onClick)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
            color = if (isActive) PureWhite else GreenDeep,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp)
        )
    }
}

@Composable
private fun ActionLargeCard(
    modifier: Modifier = Modifier,
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconBgColor: Color,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = PureWhite),
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .clickable(onClick = onClick)
    ) {
        Column {
            // Image/Graphic placeholder area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp)
                    .background(LightBg), // Acts as the placeholder for the image
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    shape = CircleShape,
                    color = iconBgColor,
                    modifier = Modifier.size(48.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = Color.White
                        )
                    }
                }

                // Little icon in top right to match reference
                Surface(
                    shape = CircleShape,
                    color = Color.Black.copy(alpha = 0.3f),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp)
                        .size(28.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Lucide.Heart, // Changed from Bookmark to Heart
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }

            // Text Area
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = GreenDeep
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextGray,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}