package com.agroroute.features.supplier.home

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.agroroute.R
import com.agroroute.core.components.UserProfileAvatar
import com.agroroute.core.model.*
import com.github.thelacspace.icons.Lucide
import java.util.Locale

private val HomeGreen = Color(0xFF086D32)
internal val supplierHomeCategories = listOf("All", "Vegetables", "Fruits", "Grains", "Dairy", "Spices")

/** Display grouping only; ORGANIC/INORGANIC remains the authoritative listing category. */
internal fun supplierProduceGroup(crop: String): String {
    val name = crop.lowercase(Locale.ROOT)
    return when {
        listOf("banana", "mango", "papaya", "pineapple", "orange", "apple", "grape", "watermelon", "avocado").any(name::contains) -> "Fruits"
        listOf("rice", "paddy", "wheat", "corn", "maize", "millet", "oat").any(name::contains) -> "Grains"
        listOf("milk", "cheese", "yogurt", "curd", "butter").any(name::contains) -> "Dairy"
        listOf("cinnamon", "pepper", "turmeric", "cardamom", "clove", "ginger").any(name::contains) -> "Spices"
        else -> "Vegetables"
    }
}

internal fun homeActiveListings(listings: List<HarvestListing>, now: Long): List<HarvestListing> =
    listings.filter { !it.isExpired(now) && it.status == ListingStatus.OPEN && !it.isDraft &&
        (it.hasConfirmedStock() || it.harvestStatus == HarvestStatus.EXPECTED) }
        .sortedByDescending { it.createdAt }

internal fun homeQuantity(listing: HarvestListing): String =
    if (listing.harvestStatus == HarvestStatus.EXPECTED)
        "Est. ${homeNumber(listing.estimatedAvailable())} ${listing.quantityUnit.name.lowercase(Locale.ROOT)}"
    else "${homeNumber(listing.availableStock())} ${listing.quantityUnit.name.lowercase(Locale.ROOT)} available"

internal fun homeNumber(value: Double): String = if (!value.isFinite()) "Unavailable" else
    java.math.BigDecimal.valueOf(value).round(java.math.MathContext(12)).stripTrailingZeros().toPlainString()
private fun homePrice(listing: HarvestListing) = if (!listing.reservePrice.isFinite()) "Price unavailable" else
    "Rs. ${homeNumber(listing.reservePrice)} / ${listing.quantityUnit.name.lowercase(Locale.ROOT)}"
internal fun homeRelativeTime(created: Long, now: Long): String {
    val minutes = ((now - created).coerceAtLeast(0) / 60_000)
    return when { minutes < 1 -> "Just now"; minutes < 60 -> "${minutes}m ago"; minutes < 1440 -> "${minutes / 60}h ago"; else -> "${minutes / 1440}d ago" }
}

@Composable
internal fun SupplierHomeContent(
    user: UserProfile?, listings: List<HarvestListing>, bids: List<Bid>, orders: List<Order>,
    loading: Boolean = false, now: Long,
    onMarketplace: () -> Unit, onOrders: () -> Unit, onBids: () -> Unit, onProfile: () -> Unit,
    onCommunity: () -> Unit, onListing: (HarvestListing) -> Unit, notification: @Composable () -> Unit,
    onSearch: () -> Unit = onMarketplace,
    heroStatusBar: Boolean = false,
    communityLeaders: (@Composable () -> Unit)? = null,
    requests: List<OrderRequest> = emptyList(),
    onOrder: (Order) -> Unit = {}
) {
    val profileDiscoveryAllowed = com.agroroute.core.runtime.LocalMutationAllowed.current
    var category by rememberSaveable { mutableStateOf("All") }
    val active = remember(listings, now) { homeActiveListings(listings, now) }
    val selected = remember(active, category) { active.filter { category == "All" || supplierProduceGroup(it.cropName) == category } }
    val accepted = remember(orders, user?.userId) { homeAcceptedOrders(orders, user?.userId.orEmpty()) }
    val pendingBidCount = remember(bids) { bids.count { it.status == BidStatus.PENDING } }
    val transitOrderCount = remember(orders) { orders.count { it.status == OrderStatus.IN_TRANSIT } }
    val featured = remember(selected) { selected.take(8) }
    val latest = remember(selected) { selected.take(4) }
    LazyColumn(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).testTag("supplier-home"),
        contentPadding = PaddingValues(bottom = 20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item(key = "hero") {
            SupplierHeroHeader(user, category, { category = it }, onSearch, onProfile, notification, heroStatusBar)
        }
        item(key = "feature") { SupplierFeatureBanner(onMarketplace, Modifier.padding(horizontal = 16.dp)) }
        item(key = "statistics") {
            SupplierStatistics(active.size, pendingBidCount, orders.size,
                transitOrderCount, onMarketplace, onBids, onOrders)
        }
        item(key = "featured-heading") { HomeSectionHeading("Featured Produce", onMarketplace) }
        item(key = "featured") {
            when {
                loading -> Box(Modifier.fillMaxWidth().height(180.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(Modifier.size(28.dp)); Text("Loading produce", Modifier.align(Alignment.BottomCenter).padding(20.dp))
                }
                selected.isEmpty() -> Surface(Modifier.padding(horizontal = 16.dp).fillMaxWidth(), shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
                    Column(Modifier.padding(24.dp)) { Text("No listings available", fontWeight = FontWeight.Bold); Text("Try another category or check Marketplace for new harvests.", style = MaterialTheme.typography.bodySmall) }
                }
                else -> LazyRow(Modifier.testTag("featured-row"), contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(featured, key = { it.listingId }) { FeaturedProduceCard(it) { onListing(it) } }
                }
            }
        }
        if (accepted.isNotEmpty()) item(key = "accepted-orders") {
            SupplierAcceptedOrders(accepted, listings, requests, now, onOrders, onOrder)
        }
        item(key = "latest-heading") { HomeSectionHeading("Latest Listings", onMarketplace) }
        item(key = "latest-and-promo") {
            BoxWithConstraints(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                if (maxWidth >= 600.dp && LocalDensity.current.fontScale <= 1.3f) {
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Column(Modifier.weight(1.55f)) { LatestListings(latest, now, onListing) }
                        LocalFarmersPromo(onMarketplace, Modifier.weight(1f))
                    }
                } else Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    LatestListings(latest, now, onListing)
                    LocalFarmersPromo(onMarketplace, Modifier.fillMaxWidth())
                }
            }
        }
        item(key = "community") {
            TextButton(onCommunity, Modifier.padding(horizontal = 16.dp)) {
                Icon(Lucide.Award, null, Modifier.size(18.dp)); Spacer(Modifier.width(8.dp))
                Text("Your community points: ${user?.points ?: 0}")
            }
        }
        if (communityLeaders != null && profileDiscoveryAllowed) item(key = "community-leaders") { Column(Modifier.padding(horizontal = 16.dp)) { communityLeaders() } }
    }
}

@Composable
private fun SupplierHeroHeader(user: UserProfile?, selected: String, onCategory: (String) -> Unit,
    onSearch: () -> Unit, onProfile: () -> Unit, notification: @Composable () -> Unit, heroStatusBar: Boolean) {
    if (heroStatusBar) com.agroroute.core.theme.DarkHeaderStatusBar()
    Box(Modifier.fillMaxWidth().testTag("supplier-home-hero").clip(RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp))) {
        Image(painterResource(R.drawable.supplier_farm_hero), null, Modifier.matchParentSize(), contentScale = ContentScale.Crop)
        Box(Modifier.matchParentSize().background(Brush.verticalGradient(listOf(Color(0xB8003017), Color(0x4D003017), Color(0xBB064925)))))
        Column(Modifier.fillMaxWidth().then(if (heroStatusBar) Modifier.statusBarsPadding() else Modifier)
            .padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(painterResource(R.drawable.agroroute1), null, Modifier.size(30.dp))
                Spacer(Modifier.width(7.dp))
                Column(Modifier.weight(1f)) {
                    Text("AgroRoute", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 23.sp)
                    Text("Fresh Farms. Direct to You.", color = Color.White, fontSize = 10.sp)
                }
                notification()
                Row(Modifier.clip(RoundedCornerShape(24.dp)).clickable(onClick = onProfile).padding(4.dp), verticalAlignment = Alignment.CenterVertically) {
                    UserProfileAvatar(user?.displayName ?: "Supplier", user?.profileImageUrl, size = 32.dp)
                    Spacer(Modifier.width(5.dp)); Text("Supplier", color = Color.White, fontSize = 11.sp)
                    Icon(Lucide.ChevronRight, null, Modifier.size(12.dp).rotate(90f), tint = Color.White)
                }
            }
            Text("Good day, ${user?.displayName?.takeIf(String::isNotBlank) ?: "Supplier"}", color = Color.White, style = MaterialTheme.typography.labelSmall)
            SupplierSearchBar(onSearch)
            SupplierCategoryRow(selected, onCategory)
        }
    }
}

@Composable
private fun SupplierSearchBar(onSearch: () -> Unit) {
    Surface(onClick = onSearch, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp).testTag("supplier-search"),
        shape = RoundedCornerShape(30.dp), color = MaterialTheme.colorScheme.surface) {
        Row(Modifier.padding(horizontal = 14.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Lucide.Search, null, Modifier.size(22.dp)); Spacer(Modifier.width(10.dp))
            Text("Search vegetables, fruits, suppliers...", Modifier.weight(1f), fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.width(6.dp)); Icon(Lucide.Filter, "Search and filter Marketplace", Modifier.size(22.dp))
        }
    }
}

@Composable
private fun SupplierCategoryRow(selected: String, onSelect: (String) -> Unit) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(supplierHomeCategories, key = { it }) { category ->
            FilterChip(selected == category, { onSelect(category) }, label = { Text(category, fontSize = 12.sp) },
                shape = RoundedCornerShape(22.dp), border = null,
                colors = FilterChipDefaults.filterChipColors(containerColor = MaterialTheme.colorScheme.surface,
                    selectedContainerColor = HomeGreen, selectedLabelColor = Color.White))
        }
    }
}

@Composable
private fun SupplierFeatureBanner(onMap: () -> Unit, modifier: Modifier) {
    Surface(modifier, color = HomeGreen, contentColor = Color.White, shape = RoundedCornerShape(16.dp)) {
        Row(Modifier.padding(14.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Lucide.Truck, null, Modifier.size(30.dp))
            Column(Modifier.weight(1f)) {
                Text("Fresh Produce, Direct from Farmers", fontWeight = FontWeight.Bold, fontSize = 15.sp, lineHeight = 19.sp)
                Text("Compare prices • Place orders • Track delivery", fontSize = 10.sp, lineHeight = 14.sp)
            }
            Button(onMap, contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = HomeGreen)) {
                Icon(Lucide.MapPin, null, Modifier.size(15.dp)); Spacer(Modifier.width(4.dp)); Text("View Map", fontSize = 11.sp)
            }
        }
    }
}

@Composable
private fun SupplierStatistics(listings: Int, bids: Int, orders: Int, transit: Int,
    onMarketplace: () -> Unit, onBids: () -> Unit, onOrders: () -> Unit) {
    val labels = listOf("New Listings", "Active Bids", "Recent Orders", "On the Way")
    val values = listOf(listings, bids, orders, transit)
    val icons = listOf(Lucide.Leaf, Lucide.Gavel, Lucide.Package, Lucide.Truck)
    val accents = listOf(Color(0xFF3A8A39), Color(0xFFDD7808), Color(0xFF267CE0), Color(0xFF8847BE))
    val actions = listOf(onMarketplace, onBids, onOrders, onOrders)
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val scroll = maxWidth < 360.dp || LocalDensity.current.fontScale > 1.3f
        if (scroll) LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(4) { i -> SupplierStatCard(labels[i], values[i], icons[i], accents[i], actions[i], Modifier.width(108.dp)) }
        } else Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            repeat(4) { i -> SupplierStatCard(labels[i], values[i], icons[i], accents[i], actions[i], Modifier.weight(1f)) }
        }
    }
}

@Composable
private fun SupplierStatCard(label: String, value: Int, icon: ImageVector, accent: Color, onClick: () -> Unit, modifier: Modifier) {
    val dark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    Surface(onClick, modifier.testTag("stat-$label"), shape = RoundedCornerShape(13.dp),
        color = if (dark) MaterialTheme.colorScheme.surfaceVariant else accent.copy(alpha = 0.08f)) {
        Column(Modifier.padding(10.dp).heightIn(min = 80.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(icon, null, Modifier.size(22.dp), tint = accent)
                Text(value.toString(), fontSize = 22.sp, fontWeight = FontWeight.Bold)
            }
            Text(label, fontSize = 11.sp, lineHeight = 14.sp)
        }
    }
}

@Composable
private fun HomeSectionHeading(title: String, onAll: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(title, Modifier.weight(1f), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        TextButton(onAll) { Text("View All", color = MaterialTheme.colorScheme.primary); Icon(Lucide.ChevronRight, null, Modifier.size(16.dp)) }
    }
}

@Composable
private fun SupplierProduceImage(listing: HarvestListing, modifier: Modifier) {
    val url = listing.imageUrls.firstOrNull()
    var failed by remember(url) { mutableStateOf(false) }
    Box(modifier.background(MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) {
        Icon(Lucide.Sprout, if (url.isNullOrBlank() || failed) "${listing.cropName} photo unavailable" else null,
            Modifier.size(36.dp), tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.65f))
        if (!url.isNullOrBlank() && !failed) AsyncImage(url, listing.cropName, Modifier.matchParentSize(),
            contentScale = ContentScale.Crop, onError = { failed = true })
    }
}

@Composable
private fun FeaturedProduceCard(listing: HarvestListing, onOpen: () -> Unit) {
    Card(onOpen, Modifier.width(184.dp).testTag("featured-${listing.listingId}"), shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), elevation = CardDefaults.cardElevation(2.dp)) {
        SupplierProduceImage(listing, Modifier.fillMaxWidth().height(118.dp))
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(listing.cropName, fontWeight = FontWeight.Bold, fontSize = 16.sp, lineHeight = 20.sp, maxLines = 2)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Lucide.MapPin, null, Modifier.size(12.dp)); Spacer(Modifier.width(3.dp))
                Text(listing.location.lineSequence().firstOrNull().orEmpty().ifBlank { "Pickup location" },
                    fontSize = 10.sp, lineHeight = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Text(homePrice(listing), fontWeight = FontWeight.Bold, fontSize = 14.sp, lineHeight = 18.sp)
            Text(homeQuantity(listing), fontSize = 11.sp, lineHeight = 15.sp, color = MaterialTheme.colorScheme.primary, maxLines = 2)
            Button(onOpen, Modifier.fillMaxWidth(), contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = HomeGreen, contentColor = Color.White)) {
                Icon(if (listing.biddingEnabled) Lucide.Gavel else Lucide.ShoppingBag, null, Modifier.size(16.dp))
                Spacer(Modifier.width(5.dp)); Text(if (listing.biddingEnabled) "View / Bid" else "View / Request", fontSize = 11.sp)
            }
        }
    }
}

@Composable
private fun LatestListings(listings: List<HarvestListing>, now: Long, onOpen: (HarvestListing) -> Unit) {
    Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surface, shadowElevation = 1.dp) {
        Column(Modifier.fillMaxWidth()) {
            if (listings.isEmpty()) Text("New harvests will appear here.", Modifier.padding(20.dp), style = MaterialTheme.typography.bodyMedium)
            listings.forEachIndexed { index, listing ->
                LatestListingRow(listing, now) { onOpen(listing) }
                if (index < listings.lastIndex) HorizontalDivider(Modifier.padding(horizontal = 12.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
            }
        }
    }
}

@Composable
private fun LatestListingRow(listing: HarvestListing, now: Long, onOpen: () -> Unit) {
    BoxWithConstraints(Modifier.fillMaxWidth().clickable(onClick = onOpen).testTag("latest-${listing.listingId}").padding(10.dp)) {
        val compact = maxWidth < 300.dp || LocalDensity.current.fontScale > 1.3f
        Row(horizontalArrangement = Arrangement.spacedBy(9.dp), verticalAlignment = Alignment.CenterVertically) {
            SupplierProduceImage(listing, Modifier.size(48.dp).clip(RoundedCornerShape(9.dp)))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(listing.cropName, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, lineHeight = 17.sp, maxLines = 2)
                Text("${listing.location.lineSequence().firstOrNull().orEmpty()} • ${homeRelativeTime(listing.createdAt, now)}",
                    fontSize = 10.sp, lineHeight = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (compact) {
                    Text(homePrice(listing), fontWeight = FontWeight.SemiBold, fontSize = 11.sp, lineHeight = 15.sp)
                    Text(homeQuantity(listing), color = MaterialTheme.colorScheme.primary, fontSize = 10.sp, lineHeight = 14.sp)
                }
            }
            if (!compact) Column(horizontalAlignment = Alignment.End, modifier = Modifier.widthIn(max = 110.dp)) {
                Text(homePrice(listing), fontWeight = FontWeight.SemiBold, fontSize = 11.sp, lineHeight = 15.sp)
                Text(homeQuantity(listing), color = MaterialTheme.colorScheme.primary, fontSize = 10.sp, lineHeight = 14.sp, maxLines = 2)
            }
            Icon(Lucide.ChevronRight, null, Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun LocalFarmersPromo(onExplore: () -> Unit, modifier: Modifier) {
    Box(modifier.clip(RoundedCornerShape(16.dp)).heightIn(min = 220.dp)) {
        Image(painterResource(R.drawable.supplier_farmer_promo), null, Modifier.matchParentSize(), contentScale = ContentScale.Crop, alignment = Alignment.CenterEnd)
        val dark = MaterialTheme.colorScheme.background.luminance() < 0.5f
        Box(Modifier.matchParentSize().background(Brush.horizontalGradient(listOf(
            if (dark) Color(0xF0132920) else Color(0xF9FFF8E6), Color.Transparent))))
        Column(Modifier.fillMaxWidth(0.58f).padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Direct from\nLocal Farmers", fontSize = 23.sp, lineHeight = 27.sp, fontWeight = FontWeight.Bold)
            Text("Quality produce\nat the best prices", style = MaterialTheme.typography.bodySmall)
            Button(onExplore, contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = HomeGreen, contentColor = Color.White)) {
                Text("Explore More", fontSize = 12.sp); Icon(Lucide.ChevronRight, null, Modifier.size(15.dp))
            }
        }
    }
}

/** Supplier root destinations share the same navigation surface. */
@Composable
fun SupplierHomeBottomNavigation(onNavigate: (String) -> Unit, modifier: Modifier = Modifier,
    selectedRoute: String = com.agroroute.core.navigation.Screen.SupplierHome.route) {
    val tabs = listOf(
        Triple(com.agroroute.core.navigation.Screen.SupplierHome.route, "Home", Lucide.Home),
        Triple(com.agroroute.core.navigation.Screen.SupplierMarketplace.route, "Marketplace", Lucide.Store),
        Triple(com.agroroute.core.navigation.Screen.SupplierMap.route, "Map", Lucide.MapPin),
        Triple(com.agroroute.core.navigation.Screen.SupplierOrders.route, "Orders", Lucide.Receipt),
        Triple(com.agroroute.core.navigation.Screen.SupplierProfile.route, "Profile", Lucide.User)
    )
    Box(modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 16.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center) {
        Surface(Modifier.widthIn(max = 520.dp).fillMaxWidth(), color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(28.dp), shadowElevation = 3.dp,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))) {
            Row(Modifier.padding(horizontal = 6.dp, vertical = 6.dp)) {
                tabs.forEachIndexed { index, (route, label, icon) ->
                    val selected = route == selectedRoute
                    val tint by animateColorAsState(
                        if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        tween(180), label = "supplierNavTint")
                    val capsule by animateColorAsState(
                        if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                        tween(180), label = "supplierNavCapsule")
                    Column(Modifier.weight(1f).clip(RoundedCornerShape(20.dp)).background(capsule).clickable { onNavigate(route) }
                        .heightIn(min = 56.dp).padding(6.dp), horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(icon, label, Modifier.size(24.dp), tint = tint)
                        Text(label, fontSize = 11.sp, lineHeight = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
                            color = tint, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
                    }
                }
            }
        }
}
}
