package com.agroroute.core.notification

import com.agroroute.core.model.UserRole
import com.agroroute.core.navigation.Screen

/** Maps authenticated notification recipients to existing role-appropriate screens. */
object NotificationRoute {
    fun forEvent(
        event: NotificationEvent,
        role: UserRole,
        payload: NotificationEvent.Payload
    ): String? = when (event) {
        NotificationEvent.ORDER_REQUEST_CREATED ->
            if (role == UserRole.FARMER) Screen.FarmerOrderRequests.route else null
        NotificationEvent.ORDER_REQUEST_ACCEPTED ->
            if (role == UserRole.SUPPLIER) payload.orderId.toRoute(Screen.SupplierOrderDetail::createRoute) else null
        NotificationEvent.ORDER_REQUEST_REJECTED ->
            if (role == UserRole.SUPPLIER) Screen.SupplierOrders.route else null
        NotificationEvent.ORDER_REQUEST_CANCELLED ->
            if (role == UserRole.FARMER) Screen.FarmerOrderRequests.route else null
        NotificationEvent.BID_CREATED, NotificationEvent.BID_WITHDRAWN ->
            if (role == UserRole.FARMER) payload.listingId.toRoute(Screen.ViewBids::createRoute) else null
        NotificationEvent.BID_REJECTED ->
            if (role == UserRole.SUPPLIER) Screen.SupplierBids.route else null
        NotificationEvent.BID_ACCEPTED ->
            if (role == UserRole.SUPPLIER) payload.orderId.toRoute(Screen.SupplierOrderDetail::createRoute) else null
        NotificationEvent.ORDER_IN_TRANSIT,
        NotificationEvent.ORDER_CANCELLED,
        NotificationEvent.ORDER_COMPLETED -> when (role) {
            UserRole.FARMER -> payload.orderId.toRoute(Screen.FarmerOrderDetail::createRoute)
            UserRole.SUPPLIER -> payload.orderId.toRoute(Screen.SupplierOrderDetail::createRoute)
        }
        NotificationEvent.HARVEST_REMINDER -> payload.listingId.toRoute(Screen.EditListing::createRoute)
        NotificationEvent.HARVEST_READY, NotificationEvent.HARVEST_FAILED, NotificationEvent.HARVEST_UPDATE ->
            if (role == UserRole.SUPPLIER) Screen.SupplierOrders.route else null
    }

    fun isAllowed(route: String, role: UserRole): Boolean {
        if (route.isBlank() || route.split('/').any(String::isBlank)) return false
        return when (role) {
            UserRole.FARMER -> route == Screen.FarmerOrderRequests.route ||
                route == Screen.HarvestCalendar.route ||
                route.startsWith("farmer_order_detail/") ||
                route.startsWith("view_bids/") ||
                route.startsWith("edit_listing/")
            UserRole.SUPPLIER -> route == Screen.SupplierOrders.route ||
                route == Screen.SupplierBids.route ||
                route == Screen.SupplierMarketplace.route ||
                route.startsWith("supplier_order_detail/")
        }
    }

    private fun String.toRoute(create: (String) -> String): String? =
        takeIf(String::isNotBlank)?.let(create)
}
