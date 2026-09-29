package com.agroroute.features.farmer.shell

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import com.github.thelacspace.icons.Lucide
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.agroroute.core.navigation.Screen

private data class FarmerTab(
    val route: String,
    val label: String,
    val icon: ImageVector
)

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

    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 10.dp)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(
                    elevation = 16.dp,
                    shape = RoundedCornerShape(28.dp),
                    ambientColor = primary.copy(alpha = 0.18f),
                    spotColor = primary.copy(alpha = 0.12f)
                )
                .border(1.dp, outlineVariant, RoundedCornerShape(28.dp)),
            shape = RoundedCornerShape(28.dp),
            color = surface.copy(alpha = 0.96f),
            tonalElevation = 3.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .padding(horizontal = 6.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                farmerTabs.forEach { tab ->
                    val isSelected = selectedRoute.startsWith(tab.route.substringBefore("{"))
                    FarmerTabItem(
                        tab = tab,
                        selected = isSelected,
                        onClick = { onNavigate(tab.route) }
                    )
                }
            }
        }
    }
}

@Composable
private fun FarmerTabItem(
    tab: FarmerTab,
    selected: Boolean,
    onClick: () -> Unit
) {
    val scale by animateFloatAsState(
        targetValue = if (selected) 1f else 0.96f,
        animationSpec = spring(dampingRatio = 0.75f, stiffness = Spring.StiffnessMediumLow),
        label = "tabScale"
    )
    val bgColor by animateColorAsState(
        targetValue = if (selected)
            Color(0xFF16A34A).copy(alpha = 0.1f)  // Light green background for selected
        else
            Color.Transparent,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "tabBg"
    )
    val iconTint by animateColorAsState(
        targetValue = if (selected)
            Color(0xFF16A34A)  // Primary green for selected
        else
            Color(0xFF9CA3AF),  // Light gray for unselected
        label = "tabIconTint"
    )
    val interactionSource = remember { MutableInteractionSource() }

    Row(
        modifier = Modifier
            .scale(scale)
            .clip(RoundedCornerShape(20.dp))
            .background(bgColor)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 12.dp, vertical = 10.dp)
            .widthIn(min = 52.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = tab.icon,
            contentDescription = tab.label,
            tint = iconTint,
            modifier = Modifier.size(22.dp)
        )

        // Label slides in only when selected
        AnimatedVisibility(
            visible = selected,
            enter = fadeIn() + expandHorizontally(expandFrom = Alignment.Start),
            exit = fadeOut() + shrinkHorizontally(shrinkTowards = Alignment.Start)
        ) {
            Row {
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = tab.label,
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        letterSpacing = 0.1.sp
                    ),
                    color = Color(0xFF16A34A),  // Primary green for selected text
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}