package com.agroroute.features.farmer.home

import com.agroroute.core.navigation.Screen
import com.agroroute.core.navigation.sharedSettingsRoutes
import com.agroroute.features.farmer.shell.farmerRoutes
import com.agroroute.features.farmer.shell.farmerTabRoutes
import com.agroroute.features.supplier.shell.supplierRoutes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FarmerNavigationTest {

    @Test
    fun farmerHomeActionsResolveOnlyToFarmerDestinations() {
        val homeRoutes = FarmerHomeDestination.values().map { it.route }.toSet()

        assertEquals(
            setOf(
                Screen.CreateListing.route,
                Screen.MyListings.route,
                Screen.FarmerOrders.route,
                Screen.HarvestCalendar.route,
                Screen.FarmerOrderRequests.route
            ),
            homeRoutes
        )
        assertTrue(homeRoutes.all { it in farmerRoutes })
        assertFalse(Screen.SupplierMarketplace.route in homeRoutes)
        assertFalse(Screen.PlaceBid.route in homeRoutes)
        assertTrue(Screen.SupplierMarketplace.route in supplierRoutes)
        assertTrue(Screen.PlaceBid.route in supplierRoutes)
    }

    @Test
    fun farmerListingActionResolvesToIncomingBidsNotPlaceBid() {
        val listingId = "farmer-listing"
        val farmerRoute = Screen.ViewBids.createRoute(listingId)

        assertEquals("view_bids/$listingId", farmerRoute)
        assertTrue(Screen.ViewBids.route in farmerRoutes)
        assertFalse(farmerRoute == Screen.PlaceBid.createRoute(listingId))
        assertTrue(Screen.PlaceBid.route in supplierRoutes)
    }

    @Test
    fun farmerDetailRoutesStayReachableWithRequiredParameters() {
        val listingId = "listing-1"
        val orderId = "order-1"

        assertEquals("edit_listing/$listingId", Screen.EditListing.createRoute(listingId))
        assertEquals("farmer_order_detail/$orderId", Screen.FarmerOrderDetail.createRoute(orderId))

        assertTrue(Screen.EditListing.route in farmerRoutes)
        assertTrue(Screen.FarmerOrderDetail.route in farmerRoutes)
        assertTrue(Screen.FarmerOrderRequests.route in farmerRoutes)
        assertTrue(farmerTabRoutes.all { it in farmerRoutes })
    }

    @Test
    fun farmerCanOpenSharedSettingsWithoutBeingBouncedHome() {
        val settingsRoutes = setOf(
            Screen.HelpSupport.route,
            Screen.About.route,
            Screen.Privacy.route
        )
        assertTrue(settingsRoutes.all { it in farmerRoutes })
        assertTrue(settingsRoutes.all { it in sharedSettingsRoutes })
        assertFalse(Screen.SupplierMarketplace.route in sharedSettingsRoutes)
    }
}