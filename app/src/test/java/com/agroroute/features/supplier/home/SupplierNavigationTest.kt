package com.agroroute.features.supplier.home

import com.agroroute.core.navigation.Screen
import com.agroroute.core.navigation.sharedSettingsRoutes
import com.agroroute.features.farmer.shell.farmerRoutes
import com.agroroute.features.supplier.shell.supplierRoutes
import com.agroroute.features.supplier.shell.supplierTabRoutes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SupplierNavigationTest {

    private val farmerOnlyRoutes = setOf(
        Screen.FarmerHome.route,
        Screen.MyListings.route,
        Screen.FarmerBids.route,
        Screen.HarvestCalendar.route,
        Screen.CreateListing.route,
        Screen.EditListing.route,
        Screen.ViewBids.route,
        Screen.FarmerOrders.route,
        Screen.FarmerOrderRequests.route,
        Screen.FarmerOrderDetail.route
    )

    private val supplierOnlyRoutes = setOf(
        Screen.SupplierHome.route,
        Screen.SupplierMarketplace.route,
        Screen.SupplierBids.route,
        Screen.PlaceBid.route,
        Screen.PlaceOrderRequest.route,
        Screen.SupplierOrders.route,
        Screen.SupplierOrderDetail.route
    )

    @Test
    fun supplierBottomBarOnlyExposesSupplierRoutes() {
        assertEquals(
            setOf(
                Screen.SupplierHome.route,
                Screen.SupplierMarketplace.route,
                Screen.SupplierMap.route,
                Screen.SupplierOrders.route,
                Screen.SupplierProfile.route
            ),
            supplierTabRoutes
        )
        assertFalse(Screen.SupplierBids.route in supplierTabRoutes)
        assertTrue(Screen.SupplierBids.route in supplierRoutes)
        assertTrue(supplierTabRoutes.all { it in supplierRoutes })
        assertTrue(supplierTabRoutes.none { it in farmerOnlyRoutes })
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
        // Present in both role shells so each profile screen can open them…
        assertTrue(settingsRoutes.all { it in farmerRoutes })
        assertTrue(settingsRoutes.all { it in supplierRoutes })
        // …and excluded from the role-guard bounce so they stay reachable.
        assertTrue(settingsRoutes.all { it in sharedSettingsRoutes })
        assertTrue(Screen.FarmerLeaderboard.route in sharedSettingsRoutes)
        assertTrue(Screen.SupplierLeaderboard.route in sharedSettingsRoutes)
        assertTrue(Screen.SubmitReview.route in sharedSettingsRoutes)
        assertFalse(Screen.SupplierMarketplace.route in sharedSettingsRoutes)
        assertFalse(Screen.CreateListing.route in sharedSettingsRoutes)
    }
}
