package com.agroroute.features.supplier.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import com.agroroute.core.model.HarvestListing
import com.agroroute.core.location.SelectedLocation
import com.agroroute.features.supplier.marketplace.MarketplaceLocationPreview
import com.agroroute.R
import com.github.thelacspace.icons.Lucide

/** Scoped by the existing supplier shell, including shared dialogs and inbox. */
val LocalSupplierPresentation = staticCompositionLocalOf { false }

/** Presentation only: callers retain their existing state, eligibility and actions. */
@Composable
fun SupplierCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    containerColor: Color = MaterialTheme.colorScheme.surface,
    elevation: Dp = 2.dp,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    val colors = CardDefaults.cardColors(containerColor = containerColor)
    val elevations = CardDefaults.cardElevation(defaultElevation = elevation)
    if (onClick != null) {
        Card(onClick = onClick, modifier = modifier, shape = RoundedCornerShape(22.dp),
            colors = colors, elevation = elevations) {
            Column(Modifier.padding(contentPadding), content = content)
        }
    } else {
        Card(modifier = modifier, shape = RoundedCornerShape(22.dp), colors = colors,
            elevation = elevations) { Column(Modifier.padding(contentPadding), content = content) }
    }
}

@Composable
fun SupplierSectionHeader(title: String, subtitle: String? = null, icon: ImageVector = Lucide.Leaf,
    modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Surface(shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.primaryContainer) {
            Icon(icon, null, Modifier.padding(12.dp).size(22.dp), tint = MaterialTheme.colorScheme.primary)
        }
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun SupplierMarketBanner(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().heightIn(min = 104.dp).clip(RoundedCornerShape(22.dp))) {
        Image(painterResource(R.drawable.supplier_farm_hero), null, Modifier.matchParentSize(),
            contentScale = ContentScale.Crop)
        Surface(Modifier.matchParentSize(), color = Color(0xFF073D24).copy(alpha = .76f)) {}
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Fresh from local farms", style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold, color = Color.White)
            Text("Discover harvests. Compare offers. Connect with farmers.",
                style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = .88f))
        }
    }
}

/** Local art is explicitly a fallback; a valid seller URL always takes precedence. */
@Composable
fun SupplierListingImage(imageUrl: String?, modifier: Modifier = Modifier, height: Dp = 160.dp) {
    var failed by remember(imageUrl) { mutableStateOf(false) }
    Box(modifier.fillMaxWidth().height(height).clip(RoundedCornerShape(16.dp))) {
        if (imageUrl.isNullOrBlank() || failed) {
            Image(painterResource(R.drawable.supplier_produce_fallback), "Listing photo unavailable; generic produce illustration",
                Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        } else {
            AsyncImage(imageUrl, "Listing photo", Modifier.fillMaxSize(), contentScale = ContentScale.Crop,
                onError = { failed = true })
        }
    }
}

@Composable
fun SupplierEmptyState(icon: ImageVector, title: String, subtitle: String? = null,
    modifier: Modifier = Modifier) {
    SupplierCard(modifier.padding(24.dp).fillMaxWidth(), elevation = 0.dp) {
        Column(Modifier.fillMaxWidth().padding(vertical = 12.dp), horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Image(painterResource(R.drawable.supplier_empty_state), null,
                Modifier.size(112.dp).clip(RoundedCornerShape(24.dp)), contentScale = ContentScale.Crop)
            Icon(icon, null, Modifier.size(24.dp), tint = MaterialTheme.colorScheme.primary)
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        }
    }
}

/** The details gallery keeps every real listing photograph; local art is fallback only. */
@Composable
fun SupplierListingGallery(imageUrls: List<String>) {
    if (imageUrls.size <= 1) SupplierListingImage(imageUrls.firstOrNull(), height = 208.dp)
    else LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        itemsIndexed(imageUrls) { _, url ->
            SupplierListingImage(url, Modifier.width(260.dp), height = 208.dp)
        }
    }
}

@Composable
fun SupplierListingContext(listing: HarvestListing) {
    Spacer(Modifier.height(12.dp))
    Text("Listing details", style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary)
    listing.expiresAt?.let { expiry ->
        Text("Expires ${java.text.DateFormat.getDateTimeInstance(java.text.DateFormat.MEDIUM,
            java.text.DateFormat.SHORT).format(java.util.Date(expiry))}",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    SelectedLocation.fromStoredText(listing.location)?.let { pickup ->
        MarketplaceLocationPreview(pickup, Modifier.padding(top = 12.dp))
    }
}

/** Lifecycle display only; no actions or inferred transitions. */
@Composable
fun SupplierOrderProgress(currentStatus: com.agroroute.core.model.OrderStatus, modifier: Modifier = Modifier) {
    val step = when (currentStatus) {
        com.agroroute.core.model.OrderStatus.AWAITING_PICKUP -> 1
        com.agroroute.core.model.OrderStatus.IN_TRANSIT -> 2
        com.agroroute.core.model.OrderStatus.DELIVERED -> 3
        com.agroroute.core.model.OrderStatus.CANCELLED -> -1
    }
    val labels = listOf("Accepted", "Pickup", "In Transit", "Delivered")
    Column(modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            labels.forEachIndexed { index, _ ->
                val completed = step >= 0 && (index < step || step == 3)
                val active = index == step && step != 3
                val color = when {
                    completed -> MaterialTheme.colorScheme.primary
                    active -> MaterialTheme.colorScheme.tertiary
                    else -> MaterialTheme.colorScheme.outlineVariant
                }
                Surface(shape = androidx.compose.foundation.shape.CircleShape, color = color) {
                    Box(Modifier.size(26.dp), contentAlignment = Alignment.Center) {
                        if (completed || active) Icon(Lucide.Check, null, Modifier.size(16.dp),
                            tint = if (completed) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onTertiary)
                    }
                }
                if (index < labels.lastIndex) HorizontalDivider(Modifier.weight(1f), thickness = 3.dp,
                    color = if (index < step) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant)
            }
        }
        Spacer(Modifier.height(6.dp))
        Row {
            labels.forEachIndexed { index, label ->
                Text(label, Modifier.weight(1f), style = MaterialTheme.typography.labelSmall,
                    textAlign = when (index) { 0 -> androidx.compose.ui.text.style.TextAlign.Start
                        3 -> androidx.compose.ui.text.style.TextAlign.End
                        else -> androidx.compose.ui.text.style.TextAlign.Center },
                    color = if (index == step) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (step < 0) Text("Cancelled", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.error)
    }
}
