package com.agroroute.core.navigation

import com.agroroute.core.util.Constants

/**
 * Sealed class defining every named destination in the app's navigation graph.
 * Each object/class exposes a [route] string used by [NavGraph] and [NavController].
 */
sealed class Screen(val route: String) {

    // ── Launch & Auth flow ───────────────────────────────────────────────────────

    data object Welcome      : Screen("welcome")
    data object Login        : Screen("login")
    data object Register     : Screen("register")
    data object RoleSelection: Screen("role_selection")
    data object FarmerProfile: Screen("farmer_profile")
    data object SupplierProfile: Screen("supplier_profile")

    // ── Home (role-specific dashboards) ───────────────────────────────────────
    data object FarmerHome   : Screen("farmer_home")
    data object SupplierHome : Screen("supplier_home")

    // ── Farmer flow ────────────────────────────────────────────────────────────
    data object CreateListing: Screen("create_listing")
    data object MyListings   : Screen("my_listings")
    data object FarmerBids   : Screen("farmer_bids")
    data object HarvestCalendar : Screen("harvest_calendar")

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

    // ── Farmer Orders ─────────────────────────────────────────────────────────
    data object FarmerOrders : Screen("farmer_orders")

    /** Requires [orderId] argument. */
    data object FarmerOrderDetail : Screen("farmer_order_detail/{${Constants.ARG_ORDER_ID}}") {
        fun createRoute(orderId: String) = "farmer_order_detail/$orderId"
    }

    // ── Supplier Orders ───────────────────────────────────────────────────────
    data object SupplierOrders : Screen("supplier_orders")

    /** Requires [orderId] argument. */
    data object SupplierOrderDetail : Screen("supplier_order_detail/{${Constants.ARG_ORDER_ID}}") {
        fun createRoute(orderId: String) = "supplier_order_detail/$orderId"
    }
}