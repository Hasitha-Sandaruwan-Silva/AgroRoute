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
    data object EditListing : Screen("edit_listing/{${Constants.ARG_LISTING_ID}}") {
        fun createRoute(listingId: String) = "edit_listing/$listingId"
    }
    data object MyListings   : Screen("my_listings")
    data object FarmerBids   : Screen("farmer_bids")
    data object HarvestCalendar : Screen("harvest_calendar")

    /** Requires [listingId] argument — navigate with [Screen.ViewBids.createRoute]. */
    data object ViewBids : Screen("view_bids/{${Constants.ARG_LISTING_ID}}") {
        fun createRoute(listingId: String) = "view_bids/$listingId"
    }

    // ── Supplier flow ──────────────────────────────────────────────────────────
    data object SupplierMarketplace: Screen("supplier_marketplace")
    data object SupplierBids: Screen("supplier_bids")

    /** Requires [listingId] argument. */
    data object PlaceBid : Screen("place_bid/{${Constants.ARG_LISTING_ID}}") {
        fun createRoute(listingId: String) = "place_bid/$listingId"
    }

    /** Requires [listingId] argument. */
    data object PlaceOrderRequest : Screen("place_order_request/{${Constants.ARG_LISTING_ID}}") {
        fun createRoute(listingId: String) = "place_order_request/$listingId"
    }

    // ── Farmer Orders ─────────────────────────────────────────────────────────
    data object FarmerOrders : Screen("farmer_orders")

    data object FarmerOrderRequests : Screen("farmer_order_requests")

    /** Requires [orderId] argument. */
    data object FarmerOrderDetail : Screen("farmer_order_detail/{${Constants.ARG_ORDER_ID}}") {
        fun createRoute(orderId: String) = "farmer_order_detail/$orderId"
    }

    // ── Supplier Orders ───────────────────────────────────────────────────────
    data object SupplierMap : Screen("supplier_map")
    data object SupplierOrders : Screen("supplier_orders")

    /** Requires [orderId] argument. */
    data object SupplierOrderDetail : Screen("supplier_order_detail/{${Constants.ARG_ORDER_ID}}") {
        fun createRoute(orderId: String) = "supplier_order_detail/$orderId"
    }

    // ── Settings Sub-Pages ────────────────────────────────────────────────────
    data object HelpSupport : Screen("help_support")
    data object About       : Screen("about")
    data object Privacy     : Screen("privacy")
    data object NotificationSettings : Screen("notification_settings")
    data object NotificationInbox : Screen("notification_inbox")

    // ── Leaderboard ───────────────────────────────────────────────────────────
    data object FarmerLeaderboard : Screen("farmer_leaderboard")
    data object SupplierLeaderboard : Screen("supplier_leaderboard")

    // ── Reviews ───────────────────────────────────────────────────────────────
    data object SubmitReview : Screen("submit_review/{${Constants.ARG_ORDER_ID}}/{${Constants.ARG_USER_ID}}/{${Constants.ARG_USER_NAME}}") {
        fun createRoute(orderId: String, revieweeId: String, revieweeName: String) =
            "submit_review/$orderId/$revieweeId/$revieweeName"
    }
}