package com.agroroute.features.supplier.shell

import android.app.Activity
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import com.github.thelacspace.icons.Lucide
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.navigation.NavController
import com.agroroute.R
import com.agroroute.core.navigation.Screen

private val SupplierGreen = Color(0xFF2E7D32)
private val SupplierDarkGreen = Color(0xFF14532D)
private val SupplierMint = Color(0xFFE8F5E9)
private val SupplierBorder = Color(0xFFE2E8F0)

@Composable
fun SupplierTopBar(
    currentRoute: String,
    navController: NavController,
    modifier: Modifier = Modifier
) {
    val isTab = currentRoute in supplierTabRoutes

    // ─── STATUS BAR ICONS කළු පැහැයට හැරවීම ───
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = true
        }
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = Color.White,
        shadowElevation = 4.dp // Premium Look එකක් සඳහා shadow එක 4.dp ලෙස වැඩි කරන ලදී
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .statusBarsPadding()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(68.dp)
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (!isTab) {
                    IconButton(
                        onClick = { navController.popBackStack() },
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(SupplierMint)
                    ) {
                        Icon(
                            imageVector = Lucide.ArrowLeft,
                            contentDescription = "Back",
                            tint = SupplierGreen,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                } else {
                    Image(
                        painter = painterResource(id = R.drawable.logo),
                        contentDescription = "AgroRoute logo",
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isTab) "AgroRoute" else supplierTitle(currentRoute),
                        style = if (isTab) {
                            // Fresh Premium Gradient Text Style
                            TextStyle(
                                brush = Brush.linearGradient(
                                    colors = listOf(
                                        SupplierDarkGreen,
                                        SupplierGreen
                                    )
                                ),
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 22.sp,
                                letterSpacing = (-0.3).sp
                            )
                        } else {
                            MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.2.sp
                            )
                        },
                        color = if (isTab) Color.Unspecified else SupplierDarkGreen,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (isTab) {
                        Text(
                            text = supplierTitle(currentRoute),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF64748B), // පැහැදිලි TextSecondary වර්ණය
                            maxLines = 1
                        )
                    }
                }
            }
            HorizontalDivider(color = SupplierBorder.copy(alpha = 0.5f), thickness = 0.8.dp)
        }
    }
}

internal val supplierTabRoutes = setOf(
    Screen.SupplierHome.route,
    Screen.SupplierMarketplace.route,
    Screen.SupplierOrders.route,
    Screen.SupplierBids.route,
    Screen.SupplierProfile.route
)

internal val supplierRoutes = setOf(
    *supplierTabRoutes.toTypedArray(),
    Screen.PlaceBid.route,
    Screen.PlaceOrderRequest.route,
    Screen.SupplierOrderDetail.route,
    Screen.HelpSupport.route,
    Screen.About.route,
    Screen.Privacy.route
)

internal fun supplierTitle(route: String): String = when {
    route == Screen.SupplierHome.route -> "Home"
    route == Screen.SupplierMarketplace.route -> "Marketplace"
    route == Screen.SupplierOrders.route -> "Orders"
    route == Screen.SupplierBids.route -> "My Bids"
    route == Screen.SupplierProfile.route -> "Profile"
    route == Screen.HelpSupport.route -> "Help & Support"
    route == Screen.About.route -> "About"
    route == Screen.Privacy.route -> "Privacy & Terms"
    route.startsWith(Screen.PlaceBid.route.substringBefore("{")) -> "Place Bid"
    route.startsWith(Screen.PlaceOrderRequest.route.substringBefore("{")) -> "Place Order Request"
    route.startsWith(Screen.SupplierOrderDetail.route.substringBefore("{")) -> "Order Details"
    else -> "Supplier"
}