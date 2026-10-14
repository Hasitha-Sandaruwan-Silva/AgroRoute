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
val FarmerForest = Color(0xFF063D2B)
val FarmerCream = Color(0xFFF8F9EF)

/** Only visual tokens are scoped; state and callbacks stay with their existing owners. */
@Composable
fun FarmerPresentation(content: @Composable () -> Unit) {
    val base = MaterialTheme.colorScheme
    val dark = base.background.luminance() < .5f
    val colors = if (dark) base.copy(
        primary = Color(0xFFB7EF79), onPrimary = Color(0xFF103B24),
        primaryContainer = Color(0xFF234B32), onPrimaryContainer = Color(0xFFD3F4B4),
        background = Color(0xFF071F17), onBackground = FarmerCream,
        surface = Color(0xFF15392B), onSurface = FarmerCream,
        surfaceVariant = Color(0xFF244A39), onSurfaceVariant = Color(0xFFB6CBBE),
        outlineVariant = Color(0xFF345442)
    ) else base.copy(
        primary = Color(0xFF006332), onPrimary = FarmerCream,
        primaryContainer = Color(0xFFE2F3D6), onPrimaryContainer = Color(0xFF124624),
        background = Color(0xFFF3F8EF), onBackground = Color(0xFF122B22),
        surface = Color(0xFFFCFDF7), onSurface = Color(0xFF122B22),
        surfaceVariant = Color(0xFFEDF3E8), onSurfaceVariant = Color(0xFF50665A),
        outlineVariant = Color(0xFFDCE8D7)
    )
    CompositionLocalProvider(LocalFarmerPresentation provides true) {
        MaterialTheme(colorScheme = colors, typography = MaterialTheme.typography,
            shapes = MaterialTheme.shapes, content = content)
    }
}

@Composable
fun FarmerArtworkHero(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Box(modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp))) {
        Image(painterResource(R.drawable.farmer_home_hero), null,
            Modifier.matchParentSize(), contentScale = ContentScale.Crop)
        Box(Modifier.matchParentSize().background(Brush.horizontalGradient(
            listOf(FarmerForest.copy(alpha = .93f), FarmerForest.copy(alpha = .45f)))))
        CompositionLocalProvider(LocalContentColor provides FarmerCream) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp), content = content)
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
        Column(Modifier.padding(18.dp).fillMaxWidth(.75f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(if (onCreate != null) "Grow More\nSell Smarter" else "Grow Together\nfor a Better Tomorrow",
                style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = FarmerCream)
            Text("Connect with buyers and find more opportunity for your harvest.",
                style = MaterialTheme.typography.bodySmall, color = FarmerCream)
            if (onCreate != null) Button(onClick = onCreate,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC1F575), contentColor = FarmerForest)) {
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
