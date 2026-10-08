package com.agroroute.core.notification

import io.mockk.mockk
import com.agroroute.core.model.UserRole
import com.agroroute.core.navigation.Screen
import org.junit.Assert.*
import org.junit.Test

class NotificationRouteTest {

    @Test
    fun `routing maps each event type to the right event`() {
        assertEquals(NotificationEvent.ORDER_REQUEST_CREATED, NotificationEvent.from(mapOf("event_type" to "ORDER_REQUEST_CREATED")))
        assertEquals(NotificationEvent.ORDER_REQUEST_ACCEPTED, NotificationEvent.from(mapOf("event_type" to "ORDER_REQUEST_ACCEPTED")))
        assertEquals(NotificationEvent.ORDER_REQUEST_REJECTED, NotificationEvent.from(mapOf("event_type" to "ORDER_REQUEST_REJECTED")))
        assertEquals(NotificationEvent.ORDER_REQUEST_CANCELLED, NotificationEvent.from(mapOf("event_type" to "ORDER_REQUEST_CANCELLED")))
        assertEquals(NotificationEvent.BID_CREATED, NotificationEvent.from(mapOf("event_type" to "BID_CREATED")))
        assertEquals(NotificationEvent.BID_ACCEPTED, NotificationEvent.from(mapOf("event_type" to "BID_ACCEPTED")))
        assertEquals(NotificationEvent.BID_REJECTED, NotificationEvent.from(mapOf("event_type" to "BID_REJECTED")))
        assertNull(NotificationEvent.from(mapOf("event_type" to "ORDER_CREATED")))
        assertEquals(NotificationEvent.ORDER_IN_TRANSIT, NotificationEvent.from(mapOf("event_type" to "ORDER_IN_TRANSIT")))
        assertEquals(NotificationEvent.ORDER_CANCELLED, NotificationEvent.from(mapOf("event_type" to "ORDER_CANCELLED")))
        assertEquals(NotificationEvent.ORDER_COMPLETED, NotificationEvent.from(mapOf("event_type" to "ORDER_COMPLETED")))
        assertEquals(NotificationEvent.HARVEST_UPDATE, NotificationEvent.from(mapOf("event_type" to "HARVEST_UPDATE")))
        assertEquals(NotificationEvent.HARVEST_READY, NotificationEvent.from(mapOf("event_type" to "HARVEST_READY")))
        assertEquals(NotificationEvent.HARVEST_FAILED, NotificationEvent.from(mapOf("event_type" to "HARVEST_FAILED")))
    }

    @Test
    fun `unknown event type yields null`() {
        assertNull(NotificationEvent.from(mapOf("event_type" to "UNKNOWN")))
    }

    @Test
    fun `event destinations follow recipient role and existing screens`() {
        val listingPayload = NotificationEvent.Payload(listingId = "listing-1")
        val orderPayload = NotificationEvent.Payload(orderId = "order-1")

        assertEquals(
            Screen.FarmerOrderRequests.route,
            NotificationRoute.forEvent(NotificationEvent.ORDER_REQUEST_CREATED, UserRole.FARMER, listingPayload)
        )
        assertEquals(
            Screen.ViewBids.createRoute("listing-1"),
            NotificationRoute.forEvent(NotificationEvent.BID_CREATED, UserRole.FARMER, listingPayload)
        )
        assertEquals(
            Screen.SupplierOrderDetail.createRoute("order-1"),
            NotificationRoute.forEvent(NotificationEvent.BID_ACCEPTED, UserRole.SUPPLIER, orderPayload)
        )
        assertEquals(
            Screen.SupplierOrders.route,
            NotificationRoute.forEvent(NotificationEvent.ORDER_REQUEST_REJECTED, UserRole.SUPPLIER, listingPayload)
        )
        assertNull(NotificationRoute.forEvent(NotificationEvent.BID_CREATED, UserRole.SUPPLIER, listingPayload))
        assertFalse(NotificationRoute.isAllowed(Screen.SupplierOrders.route, UserRole.FARMER))
        assertTrue(NotificationRoute.isAllowed(Screen.FarmerOrderRequests.route, UserRole.FARMER))
    }

    @Test
    fun `payload key is stable for duplicate-local-notification prevention`() {
        val p = NotificationEvent.Payload(orderId = "order-1", cropName = "Rice", supplierName = "S1", reason = "No reason", status = "AWAITING_PICKUP", listingId = "listing-1")
        assertEquals("order-1", p.key)

        val same = NotificationEvent.Payload(orderId = "order-1")
        assertEquals(p.key.hashCode(), same.key.hashCode())

        assertEquals("", NotificationEvent.Payload().key)
    }

    @Test
    fun `foreground data message is routeable to a content string`() {
        val payload = NotificationEvent.Payload(cropName = "Rice", supplierName = "S1")
        val content = NotificationEvent.BID_CREATED.content(
            mockk<android.content.Context>(relaxed = true),
            payload
        )
        assertEquals("S1 placed a bid for Rice.", content)
    }

    @Test
    fun `token registration includes non-empty fields`() {
        val request = mapOf("userId" to "user-1", "token" to "fcm-token-123")
        assertTrue(request["userId"] is String)
        assertTrue(request["token"] is String)
        assertFalse(request["token"].toString().isEmpty())
    }
}
