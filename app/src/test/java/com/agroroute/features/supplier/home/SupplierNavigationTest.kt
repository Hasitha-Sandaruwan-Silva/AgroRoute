package com.agroroute.features.supplier.home

import com.agroroute.core.model.UserRole
import com.agroroute.core.navigation.Screen
import com.agroroute.core.navigation.roleHomeDestination
import com.agroroute.core.navigation.sharedSettingsRoutes
import com.agroroute.features.farmer.shell.farmerRoutes
import com.agroroute.features.supplier.shell.supplierRoutes
import com.agroroute.features.supplier.shell.supplierTabRoutes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SupplierNavigationTest {

    private val supplierOnlyRoutes = setOf(
        Screen.SupplierHome.route,
        Screen.SupplierMarketplace.route,
        Screen.SupplierOrders.route,
        Screen.SupplierBids.route,
        Screen.SupplierProfile.route,
        Screen.PlaceBid.route,
        Screen.PlaceOrderRequest.route,
        Screen.SupplierOrderDetail.route
    )

    private val farmerOnlyRoutes = setOf(
        Screen.FarmerHome.route,
        Screen.CreateListing.route,
        Screen.EditListing.route,
        Screen.MyListings.route,
        Screen.FarmerBids.route,
        Screen.HarvestCalendar.route,
        Screen.ViewBids.route,
        Screen.FarmerOrders.route,
        Screen.FarmerOrderRequests.route,
        Screen.FarmerOrderDetail.route,
        Screen.FarmerProfile.route
    )

    @Test
    fun supplierTabsAndDetailRoutesAreAllReachableWithinSupplierShell() {
        assertEquals(
            setOf(
                Screen.SupplierHome.route,
                Screen.SupplierMarketplace.route,
                Screen.SupplierOrders.route,
                Screen.SupplierBids.route,
                Screen.SupplierProfile.route
            ),
            supplierTabRoutes
        )
        assertTrue(supplierTabRoutes.all { it in supplierRoutes })
        assertTrue(supplierOnlyRoutes.all { it in supplierRoutes })
    }

    @Test
    fun supplierHomeButtonsResolveOnlyToSupplierDestinations() {
        val homeButtonRoutes = setOf(
            Screen.SupplierMarketplace.route,
            Screen.SupplierOrders.route,
            Screen.SupplierBids.route
        )
        assertTrue(homeButtonRoutes.all { it in supplierRoutes })
        assertTrue(homeButtonRoutes.none { it in farmerRoutes })
        assertEquals(Screen.SupplierHome.route, roleHomeDestination(UserRole.SUPPLIER))
    }

    @Test
    fun farmerOnlyActionsAreNotExposedToSuppliers() {
        assertTrue(farmerOnlyRoutes.none { it in supplierRoutes })
        assertTrue(supplierOnlyRoutes.none { it in farmerRoutes })
    }

    @Test
    fun supplierDetailRoutesCarryRequiredParameters() {
        val listingId = "listing-1"
        val orderId = "order-1"

        assertEquals("place_bid/$listingId", Screen.PlaceBid.createRoute(listingId))
        assertEquals("place_order_request/$listingId", Screen.PlaceOrderRequest.createRoute(listingId))
        assertEquals("supplier_order_detail/$orderId", Screen.SupplierOrderDetail.createRoute(orderId))

        assertTrue(Screen.PlaceBid.route in supplierRoutes)
        assertTrue(Screen.PlaceOrderRequest.route in supplierRoutes)
        assertTrue(Screen.SupplierOrderDetail.route in supplierRoutes)
    }

    @Test
    fun sharedSettingsRoutesAreNotBouncedByRoleGuard() {
        val settingsRoutes = setOf(
            Screen.HelpSupport.route,
            Screen.About.route,
            Screen.Privacy.route
        )
        assertEquals(settingsRoutes, sharedSettingsRoutes)
        // Present in both role shells so each profile screen can open them…
        assertTrue(settingsRoutes.all { it in farmerRoutes })
        assertTrue(settingsRoutes.all { it in supplierRoutes })
        // …and excluded from the role-guard bounce so they stay reachable.
        assertTrue(settingsRoutes.all { it in sharedSettingsRoutes })
        assertFalse(Screen.SupplierMarketplace.route in sharedSettingsRoutes)
        assertFalse(Screen.CreateListing.route in sharedSettingsRoutes)
    }
}
