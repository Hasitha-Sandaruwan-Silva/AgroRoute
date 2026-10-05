package com.agroroute.features.supplier.home

import androidx.compose.foundation.background
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
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.agroroute.core.navigation.Screen
import com.agroroute.core.theme.ForestDeep
import com.agroroute.core.theme.ForestMid
import com.agroroute.core.theme.ForestSoft
import com.agroroute.core.theme.LeafAccent
import com.agroroute.core.widgets.AgroSnackbarHost
import com.agroroute.core.widgets.AppCard
import com.agroroute.core.widgets.ErrorSnackbarEffect
import com.agroroute.core.widgets.SectionTitle
import com.agroroute.core.widgets.StatCardsSkeleton
import com.agroroute.features.supplier.marketplace.MarketplaceViewModel
import com.agroroute.features.supplier.orders.SupplierOrderViewModel
import com.github.thelacspace.icons.Lucide

@Composable
fun SupplierHomeScreen(
    navController: NavController,
    marketplaceViewModel: MarketplaceViewModel = hiltViewModel(),
    orderViewModel: SupplierOrderViewModel = hiltViewModel()
) {
    val marketState by marketplaceViewModel.uiState.collectAsStateWithLifecycle()
    val orderState by orderViewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        orderViewModel.loadOrdersForSupplier(orderViewModel.currentSupplierId)
    }

    ErrorSnackbarEffect(marketState.error, snackbarHostState) { /* auto-clears on next emission */ }
    ErrorSnackbarEffect(orderState.error, snackbarHostState) { orderViewModel.clearError() }

    val biddingOpen = marketState.listings.count { it.biddingEnabled }
    val isLoading = (marketState.isLoading && marketState.listings.isEmpty()) ||
        (orderState.isLoading && orderState.orders.isEmpty())

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 18.dp)
        ) {
            // ── Forest gradient hero: greeting + key stats ──────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(28.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(ForestDeep, ForestMid, ForestSoft)
                        )
                    )
                    .padding(22.dp)
            ) {
                // Decorative leaf watermark
                Icon(
                    imageVector = Lucide.Leaf,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.08f),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(96.dp)
                )
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = LeafAccent.copy(alpha = 0.22f),
                            modifier = Modifier.size(46.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Lucide.Store,
                                    contentDescription = null,
                                    tint = LeafAccent,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Good day, Supplier",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "Find active harvests and keep track of your orders.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.78f),
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                    }

                    Spacer(Modifier.height(20.dp))
                    HorizontalDivider(color = Color.White.copy(alpha = 0.12f), thickness = 1.dp)
                    Spacer(Modifier.height(14.dp))

                    Text(
                        text = "Overview",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = LeafAccent
                    )
                    Spacer(Modifier.height(10.dp))
                    if (isLoading) {
                        StatCardsSkeleton()
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            HeroStatItem(
                                label = "Open listings",
                                value = marketState.listings.size.toString(),
                                icon = Lucide.ShoppingBag
                            )
                            HeroStatItem(
                                label = "My orders",
                                value = orderState.orders.size.toString(),
                                icon = Lucide.Receipt
                            )
                            HeroStatItem(
                                label = "Bidding open",
                                value = biddingOpen.toString(),
                                icon = Lucide.ShoppingCart,
                                highlight = true
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
            SectionTitle(title = "Marketplace")

            // ── Shortcut tiles ───────────────────────────────────────────────
            ShortcutTile(
                icon = Lucide.ShoppingBag,
                title = "Find your next harvest",
                subtitle = "Browse listings available for bids",
                actionLabel = "Browse Marketplace",
                onClick = { navController.navigate(Screen.SupplierMarketplace.route) }
            )
            Spacer(Modifier.height(12.dp))
            ShortcutTile(
                icon = Lucide.Receipt,
                title = "View My Orders",
                subtitle = null,
                actionLabel = null,
                onClick = { navController.navigate(Screen.SupplierOrders.route) }
            )
            Spacer(Modifier.height(12.dp))
            ShortcutTile(
                icon = Lucide.ShoppingCart,
                title = "View My Bids",
                subtitle = null,
                actionLabel = null,
                onClick = { navController.navigate(Screen.SupplierBids.route) }
            )
            Spacer(Modifier.height(20.dp))
        }
        AgroSnackbarHost(
            snackbarHostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@Composable
private fun HeroStatItem(
    label: String,
    value: String,
    icon: ImageVector,
    highlight: Boolean = false
) {
    Column(modifier = Modifier.weight(1f)) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (highlight) LeafAccent else Color.White.copy(alpha = 0.7f),
            modifier = Modifier.size(18.dp)
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = value,
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            color = if (highlight) LeafAccent else Color.White
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = 0.72f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun ShortcutTile(
    icon: ImageVector,
    title: String,
    subtitle: String?,
    actionLabel: String?,
    onClick: () -> Unit
) {
    AppCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
                if (actionLabel != null) {
                    Text(
                        text = actionLabel,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }
            }
            Spacer(Modifier.width(8.dp))
            Icon(
                imageVector = Lucide.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
