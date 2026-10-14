package com.agroroute.features.supplier.shell

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.agroroute.features.supplier.home.SupplierHomeBottomNavigation

/** Every supplier tab shares the approved Home navigation surface and routes. */
@Composable
fun SupplierBottomBar(
    selectedRoute: String,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    SupplierHomeBottomNavigation(onNavigate, modifier, selectedRoute)
}
