package com.agroroute.features.farmer.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.agroroute.core.model.ListingStatus
import com.agroroute.core.model.acceptsNewBids
import com.agroroute.core.navigation.Screen
import com.agroroute.core.widgets.AgroSnackbarHost
import com.agroroute.core.widgets.ErrorSnackbarEffect
import com.agroroute.features.farmer.listing.ListingViewModel
import com.agroroute.features.farmer.orders.FarmerOrderViewModel

// Palette matching the TopBar clean theme
private val AgroGreen = Color(0xFF2E7D32)
private val AgroGreenDark = Color(0xFF1B5E20)
private val AgroGreenSoft = Color(0xFFE8F5E9)
private val AgroGreenMid = Color(0xFF66BB6A)
private val AgroLeaf = Color(0xFF43A047)
private val AgroBg = Color(0xFFF6FAF6)
private val AgroText = Color(0xFF1B1B1B)
private val AgroMuted = Color(0xFF5F6B5F)

@Composable
fun FarmerHomeScreen(
    navController: NavController,
    listingViewModel: ListingViewModel = hiltViewModel(),
    orderViewModel: FarmerOrderViewModel = hiltViewModel()
) {
    val listingState by listingViewModel.uiState.collectAsStateWithLifecycle()
    val orderState by orderViewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    // ✅ LOGIC EXACTLY UNCHANGED
    LaunchedEffect(Unit) {
        listingViewModel.loadFarmerListings("current_farmer_id")
        orderViewModel.loadOrdersForFarmer("current_farmer_id")
    }

    ErrorSnackbarEffect(listingState.error, snackbarHostState) { listingViewModel.clearError() }
    ErrorSnackbarEffect(orderState.error, snackbarHostState) { orderViewModel.clearError() }

    val publishedListings = listingState.listings.filterNot { it.isDraft }
    val openListings = publishedListings.count { it.acceptsNewBids() }
    val totalListings = publishedListings.size
    val totalOrders = orderState.orders.size
    val isLoading = listingState.isLoading && listingState.listings.isEmpty()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AgroBg)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 16.dp)
        ) {
            // ── Clean Welcome Greeting (Without duplicate branding) ────
            Text(
                text = "Good day, Farmer 👋",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = AgroText
            )
            Text(
                text = "Here's what's happening on your farm today.",
                style = MaterialTheme.typography.bodyMedium,
                color = AgroMuted
            )

            Spacer(Modifier.height(18.dp))

            // ── Overview Section ──────────────────────────────────────
            SectionLabel("Overview")

            if (isLoading) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    repeat(2) {
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .height(100.dp),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(1.dp)
                        ) {}
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    HomeStatCard(
                        label = "My Listings",
                        value = totalListings.toString(),
                        icon = Icons.Default.Description,
                        accent = AgroGreen,
                        soft = AgroGreenSoft,
                        modifier = Modifier.weight(1f)
                    )
                    HomeStatCard(
                        label = "Orders",
                        value = totalOrders.toString(),
                        icon = Icons.Default.Receipt,
                        accent = AgroLeaf,
                        soft = Color(0xFFE6F4EA),
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(Modifier.height(12.dp))

                HomeStatCard(
                    label = "Open for Bids",
                    value = openListings.toString(),
                    icon = Icons.Default.Gavel,
                    accent = AgroGreenDark,
                    soft = Color(0xFFFFF8E1),
                    modifier = Modifier.fillMaxWidth(),
                    wide = true
                )
            }

            Spacer(Modifier.height(24.dp))

            // ── Quick Action Card ─────────────────────────────────────
            SectionLabel("Quick Action")

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(4.dp, RoundedCornerShape(22.dp)),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                Box(
                    modifier = Modifier
                        .background(
                            Brush.linearGradient(
                                listOf(AgroLeaf, AgroGreen, AgroGreenDark)
                            )
                        )
                        .padding(20.dp)
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.2f))
                                    .border(1.dp, Color.White.copy(alpha = 0.35f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                            Spacer(Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Create New Listing",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = "Post fresh harvest & get competitive bids",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.9f)
                                )
                            }
                        }
                        Spacer(Modifier.height(16.dp))

                        Surface(
                            onClick = { navController.navigate(Screen.CreateListing.route) },
                            shape = RoundedCornerShape(14.dp),
                            color = Color.White,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "New Listing",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = AgroGreenDark
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            // ── Navigation Shortcuts ──────────────────────────────────
            SectionLabel("Go to")

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ShortcutCard(
                    title = "My Listings",
                    subtitle = "View & manage",
                    icon = Icons.Default.Description,
                    onClick = { navController.navigate(Screen.MyListings.route) },
                    modifier = Modifier.weight(1f)
                )
                ShortcutCard(
                    title = "Orders",
                    subtitle = "Track status",
                    icon = Icons.Default.Receipt,
                    onClick = { navController.navigate(Screen.FarmerOrders.route) },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(Modifier.height(12.dp))
            Spacer(Modifier.height(12.dp))

            ShortcutCard(
                title = "Harvest Calendar",
                subtitle = "Upcoming dates and reminders",
                icon = Icons.Default.TrendingUp,
                onClick = { navController.navigate(Screen.HarvestCalendar.route) },
                modifier = Modifier.fillMaxWidth()
            )


            ShortcutCard(
                title = "Marketplace",
                subtitle = "Browse active harvest listings",
                icon = Icons.Default.Storefront,
                onClick = { navController.navigate(Screen.SupplierMarketplace.route) },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(24.dp))

            // ── Tip Banner ────────────────────────────────────────────
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = Color.White,
                shadowElevation = 1.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, AgroGreenSoft, RoundedCornerShape(18.dp))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(AgroGreenSoft),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.TrendingUp,
                            contentDescription = null,
                            tint = AgroGreen,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Pro tip",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = AgroGreenDark
                        )
                        Text(
                            text = "Open bidding on listings to attract more suppliers and better prices.",
                            style = MaterialTheme.typography.bodySmall,
                            color = AgroMuted
                        )
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
        }

        AgroSnackbarHost(
            snackbarHostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = AgroText,
        modifier = Modifier.padding(bottom = 10.dp)
    )
}

@Composable
private fun HomeStatCard(
    label: String,
    value: String,
    icon: ImageVector,
    accent: Color,
    soft: Color,
    modifier: Modifier = Modifier,
    wide: Boolean = false
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(if (wide) 44.dp else 40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(soft),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text(
                    text = value,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = AgroText
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodySmall,
                    color = AgroMuted
                )
            }
        }
    }
}

@Composable
private fun ShortcutCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .clickable(onClick = onClick)
            .border(1.dp, AgroGreenSoft, RoundedCornerShape(18.dp)),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(AgroGreenSoft),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = AgroGreen
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = AgroText
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = AgroMuted
                )
            }
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = AgroGreenMid
            )
        }
    }
}