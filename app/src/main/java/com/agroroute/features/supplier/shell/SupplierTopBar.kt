package com.agroroute.features.supplier.shell

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import com.github.thelacspace.icons.Lucide
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.agroroute.core.navigation.Screen

@Composable
fun SupplierTopBar(
    currentRoute: String,
    navController: NavController,
    modifier: Modifier = Modifier
) {
    val isTab = currentRoute in supplierTabRoutes

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(bottomStart = 22.dp, bottomEnd = 22.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        shadowElevation = 3.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (!isTab) {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(Lucide.ArrowLeft, contentDescription = "Back")
                }
                Spacer(Modifier.width(4.dp))
            } else {
                Surface(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        text = "SP",
                        modifier = Modifier.padding(top = 8.dp),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                Spacer(Modifier.width(10.dp))
            }
            Text(
                text = supplierTitle(currentRoute),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            if (isTab) {
                IconButton(onClick = { /* TODO: supplier top bar action */ }) {
                    Icon(Lucide.MoreVertical, contentDescription = "More options")
                }
            }
        }
    }
}

internal val supplierTabRoutes = setOf(
    Screen.SupplierHome.route,
    Screen.SupplierMarketplace.route,
    Screen.SupplierOrders.route,
    Screen.SupplierProfile.route
)

internal val supplierRoutes = setOf(
    *supplierTabRoutes.toTypedArray(),
    Screen.PlaceBid.route,
    Screen.SupplierOrderDetail.route
)

internal fun supplierTitle(route: String): String = when {
    route == Screen.SupplierHome.route -> "Home"
    route == Screen.SupplierMarketplace.route -> "Marketplace"
    route == Screen.SupplierOrders.route -> "Orders"
    route == Screen.SupplierProfile.route -> "Profile"
    route.startsWith(Screen.PlaceBid.route.substringBefore("{")) -> "Place Bid"
    route.startsWith(Screen.SupplierOrderDetail.route.substringBefore("{")) -> "Order Details"
    else -> "Supplier"
}