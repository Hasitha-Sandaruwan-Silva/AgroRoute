package com.agroroute.ui.navigation

import com.agroroute.util.Constants

/**
 * Sealed class defining every named destination in the app's navigation graph.
 * Each object/class exposes a [route] string used by [NavGraph] and [NavController].
 */
sealed class Screen(val route: String) {

    // ── Auth flow ──────────────────────────────────────────────────────────────
    data object Login        : Screen("login")
    data object Register     : Screen("register")
    data object RoleSelection: Screen("role_selection")

    // ── Farmer flow ────────────────────────────────────────────────────────────
    data object FarmerHome   : Screen("farmer_home")
    data object CreateListing: Screen("create_listing")
    data object MyListings   : Screen("my_listings")

    /** Requires [listingId] argument — navigate with [Screen.ViewBids.createRoute]. */
    data object ViewBids : Screen("view_bids/{${Constants.ARG_LISTING_ID}}") {
        fun createRoute(listingId: String) = "view_bids/$listingId"
    }

    // ── Supplier flow ──────────────────────────────────────────────────────────
    data object SupplierMarketplace: Screen("supplier_marketplace")

    /** Requires [listingId] argument. */
    data object PlaceBid : Screen("place_bid/{${Constants.ARG_LISTING_ID}}") {
        fun createRoute(listingId: String) = "place_bid/$listingId"
    }

    // ── Shared / Orders ───────────────────────────────────────────────────────
    /** Requires [orderId] argument. */
    data object OrderDetail : Screen("order_detail/{${Constants.ARG_ORDER_ID}}") {
        fun createRoute(orderId: String) = "order_detail/$orderId"
    }

    data object OrderStatus : Screen("order_status")
}
