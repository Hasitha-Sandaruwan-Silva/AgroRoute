package com.agroroute.features.supplier.shell

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import com.github.thelacspace.icons.Lucide
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.agroroute.core.navigation.Screen

private data class SupplierTab(
    val route: String,
    val label: String,
    val icon: ImageVector
)

private val supplierTabs = listOf(
    SupplierTab(Screen.SupplierHome.route, "Home", Lucide.Home),
    SupplierTab(Screen.SupplierMarketplace.route, "Marketplace", Lucide.ShoppingBag),
    SupplierTab(Screen.SupplierOrders.route, "Orders", Lucide.Receipt),
    SupplierTab(Screen.SupplierProfile.route, "Profile", Lucide.User)
)

@Composable
fun SupplierBottomBar(
    selectedRoute: String,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .shadow(
                elevation = 12.dp,
                shape = RoundedCornerShape(28.dp),
                ambientColor = Color(0xFF16A34A).copy(alpha = 0.22f),
                spotColor = Color(0xFF16A34A).copy(alpha = 0.18f)
            )
            .padding(horizontal = 16.dp, vertical = 12.dp),
        shape = RoundedCornerShape(28.dp),
        shadowElevation = 12.dp,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            supplierTabs.forEach { tab ->
                SupplierTabItem(
                    tab = tab,
                    selected = selectedRoute.startsWith(tab.route.substringBefore("{")),
                    onClick = { onNavigate(tab.route) }
                )
            }
        }
    }
}

@Composable
private fun SupplierTabItem(
    tab: SupplierTab,
    selected: Boolean,
    onClick: () -> Unit
) {
    val iconScale by animateFloatAsState(
        targetValue = if (selected) 1.12f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "supplierIconScale"
    )
    val iconTint by animateColorAsState(
        targetValue = if (selected) Color(0xFF16A34A)  // Primary green for selected
        else Color(0xFF9CA3AF),  // Light gray for unselected
        label = "supplierIconTint"
    )
    val interactionSource = remember { MutableInteractionSource() }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .background(
                if (selected) {
                    Color(0xFF16A34A).copy(alpha = 0.1f)  // Light green background for selected
                } else {
                    Color.Transparent
                }
            )
            .padding(horizontal = 10.dp, vertical = 5.dp)
    ) {
        Icon(
            imageVector = tab.icon,
            contentDescription = tab.label,
            tint = iconTint,
            modifier = Modifier
                .size(40.dp)
                .padding(9.dp)
                .scale(iconScale)
        )
        if (selected) {
            Text(
                text = tab.label,
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF16A34A),  // Primary green for selected text
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}