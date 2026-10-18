package com.agroroute.features.farmer.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.agroroute.R
import com.agroroute.core.model.OrderStatus

val LocalFarmerPresentation = staticCompositionLocalOf { false }
val FarmerForest = Color(0xFF08452E)
val FarmerCream = Color(0xFFF8F5EB)

/** Only visual tokens are scoped; state and callbacks stay with their existing owners. */
@Composable
fun FarmerPresentation(content: @Composable () -> Unit) {
    val base = MaterialTheme.colorScheme
    val dark = base.background.luminance() < .5f
    val colors = if (dark) base.copy(
        primary = Color(0xFFB6D99B), onPrimary = Color(0xFF103B24),
        primaryContainer = Color(0xFF28523B), onPrimaryContainer = Color(0xFFE2F0D6),
        background = Color(0xFF0D241B), onBackground = FarmerCream,
        surface = Color(0xFF193529), onSurface = FarmerCream,
        surfaceVariant = Color(0xFF244233), onSurfaceVariant = Color(0xFFB8CBBE),
        surfaceContainerLowest = Color(0xFF0D241B), surfaceContainerLow = Color(0xFF142E22),
        surfaceContainer = Color(0xFF193529), surfaceContainerHigh = Color(0xFF244233),
        surfaceContainerHighest = Color(0xFF2E4C3C), outlineVariant = Color(0xFF365442)
    ) else base.copy(
        primary = Color(0xFF07512D), onPrimary = FarmerCream,
        primaryContainer = Color(0xFFE4EDCF), onPrimaryContainer = Color(0xFF17452C),
        background = Color(0xFFF7F5EC), onBackground = Color(0xFF123829),
        surface = Color(0xFFFFFDF7), onSurface = Color(0xFF16382B),
        surfaceVariant = Color(0xFFF0F0E4), onSurfaceVariant = Color(0xFF5A6C60),
        surfaceContainerLowest = Color(0xFFFFFDF7), surfaceContainerLow = Color(0xFFF3F2E7),
        surfaceContainer = Color(0xFFF3F2E7), surfaceContainerHigh = Color(0xFFECEEDC),
        surfaceContainerHighest = Color(0xFFE4E8D8), outlineVariant = Color(0xFFE2E6D9)
    )
    val extended = com.agroroute.core.theme.LocalAgroExtendedColors.current.copy(
        pageBg = colors.background, cardSurface = colors.surface, cardBorder = colors.outlineVariant,
        divider = colors.outlineVariant, iconContainer = colors.primaryContainer,
        iconOnContainer = colors.onPrimaryContainer, accentIconContainer = colors.surfaceVariant,
        accentIconOnContainer = colors.primary
    )
    CompositionLocalProvider(LocalFarmerPresentation provides true,
        com.agroroute.core.theme.LocalAgroExtendedColors provides extended) {
        MaterialTheme(colorScheme = colors, typography = MaterialTheme.typography,
            shapes = MaterialTheme.shapes.copy(small = RoundedCornerShape(12.dp), medium = RoundedCornerShape(16.dp),
                large = RoundedCornerShape(22.dp), extraLarge = RoundedCornerShape(26.dp)), content = content)
    }
}

@Composable
fun FarmerArtworkHero(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Box(modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp))) {
        Image(painterResource(R.drawable.farmer_home_hero), null,
            Modifier.matchParentSize(), contentScale = ContentScale.Crop)
        Box(Modifier.matchParentSize().background(Brush.horizontalGradient(
            listOf(FarmerForest.copy(alpha = .94f), FarmerForest.copy(alpha = .15f)))))
        CompositionLocalProvider(LocalContentColor provides FarmerCream) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp), content = content)
        }
    }
}

@Composable
fun FarmerPromo(onCreate: (() -> Unit)? = null) {
    Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp))) {
        Image(painterResource(R.drawable.farmer_home_promo), null,
            Modifier.matchParentSize(), contentScale = ContentScale.Crop)
        Box(Modifier.matchParentSize().background(Brush.horizontalGradient(
            listOf(FarmerForest.copy(alpha = .96f), FarmerForest.copy(alpha = .25f)))))
        Column(Modifier.padding(18.dp).fillMaxWidth(.78f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(if (onCreate != null) "Better prices.\nBigger opportunities." else "Grow together.\nTrade with confidence.",
                style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = FarmerCream)
            Text("Connect with buyers for your next harvest.",
                style = MaterialTheme.typography.bodySmall, color = FarmerCream)
            if (onCreate != null) Button(onClick = onCreate,
                colors = ButtonDefaults.buttonColors(containerColor = FarmerCream, contentColor = FarmerForest)) {
                Text("Create New Listing", style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

/** Order existence means accepted; all subsequent progress comes from its saved status. */
@Composable
fun FarmerOrderProgress(status: OrderStatus) {
    if (status == OrderStatus.CANCELLED) {
        Text("Cancelled", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelMedium)
        return
    }
    val current = when (status) {
        OrderStatus.AWAITING_PICKUP -> 0
        OrderStatus.IN_TRANSIT -> 2
        OrderStatus.DELIVERED -> 3
        OrderStatus.CANCELLED -> 0
    }
    val labels = listOf("Accepted", "Pickup", "In transit", "Delivered")
    Row(Modifier.fillMaxWidth()) {
        labels.forEachIndexed { index, label ->
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Row(Modifier.fillMaxWidth().height(24.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (index == 0) Spacer(Modifier.weight(1f))
                    else HorizontalDivider(Modifier.weight(1f),
                        color = if (index <= current) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant)
                    Surface(shape = androidx.compose.foundation.shape.CircleShape,
                        color = if (index <= current) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant) {
                        if (index == 0 || index < current || status == OrderStatus.DELIVERED) Icon(
                            com.github.thelacspace.icons.Lucide.Check, null, Modifier.padding(4.dp).size(14.dp),
                            tint = MaterialTheme.colorScheme.onPrimary)
                        else Box(Modifier.size(22.dp))
                    }
                    if (index == labels.lastIndex) Spacer(Modifier.weight(1f))
                    else HorizontalDivider(Modifier.weight(1f),
                        color = if (index < current) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant)
                }
                Text(label, style = MaterialTheme.typography.labelSmall,
                    color = if (index <= current) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    modifier = Modifier.padding(top = 4.dp))
            }
        }
    }
}
