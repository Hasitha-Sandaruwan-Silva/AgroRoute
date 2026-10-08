package com.agroroute.features.farmer.shell

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.agroroute.core.navigation.Screen
import com.github.thelacspace.icons.Lucide

private data class FarmerTab(val route: String, val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector)

private val farmerTabs = listOf(
    FarmerTab(Screen.FarmerHome.route, "Home", Lucide.Home),
    FarmerTab(Screen.MyListings.route, "Listings", Lucide.FileText),
    FarmerTab(Screen.FarmerOrders.route, "Orders", Lucide.Receipt),
    FarmerTab(Screen.FarmerProfile.route, "Profile", Lucide.User)
)

@Composable
fun FarmerBottomBar(
    selectedRoute: String,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val primary = MaterialTheme.colorScheme.primary
    val surface = MaterialTheme.colorScheme.surface
    val outlineVariant = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)

    Box(modifier = modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 20.dp, vertical = 10.dp)) {
        Surface(
            modifier = Modifier.fillMaxWidth()
                .shadow(16.dp, RoundedCornerShape(28.dp), ambientColor = primary.copy(alpha = 0.18f), spotColor = primary.copy(alpha = 0.12f))
                .border(1.dp, outlineVariant, RoundedCornerShape(28.dp)),
            shape = RoundedCornerShape(28.dp),
            color = surface.copy(alpha = 0.96f),
            tonalElevation = 3.dp
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().height(64.dp).padding(horizontal = 6.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                farmerTabs.forEach { tab ->
                    val isSelected = selectedRoute.startsWith(tab.route.substringBefore("{"))
                    FarmerTabItem(tab = tab, selected = isSelected, onClick = { onNavigate(tab.route) })
                }
            }
        }
    }
}

@Composable
private fun FarmerTabItem(tab: FarmerTab, selected: Boolean, onClick: () -> Unit) {
    // BEAUTIFUL ANIMATION - SMOOTH - NO LAG - OPTIMIZED
    val scale by animateFloatAsState(
        targetValue = if (selected) 1f else 0.92f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessMediumLow),
        label = "scale"
    )
    val bgAlpha by animateFloatAsState(
        targetValue = if (selected) 0.1f else 0f,
        animationSpec = tween(200),
        label = "bgAlpha"
    )
    val interactionSource = remember { MutableInteractionSource() }

    val bgColor = MaterialTheme.colorScheme.primary.copy(alpha = bgAlpha)
    val iconTint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant

    Row(
        modifier = Modifier.scale(scale).clip(RoundedCornerShape(20.dp)).background(bgColor)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(imageVector = tab.icon, contentDescription = tab.label, tint = iconTint, modifier = Modifier.size(22.dp))
        // BEAUTIFUL LABEL ANIMATION - SLIDE + FADE
        AnimatedVisibility(
            visible = selected,
            enter = fadeIn(tween(200)) + expandHorizontally(tween(200), expandFrom = Alignment.Start),
            exit = fadeOut(tween(150)) + shrinkHorizontally(tween(150), shrinkTowards = Alignment.Start)
        ) {
            Row {
                Spacer(Modifier.width(8.dp))
                Text(
                    text = tab.label,
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold, fontSize = 13.sp),
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
