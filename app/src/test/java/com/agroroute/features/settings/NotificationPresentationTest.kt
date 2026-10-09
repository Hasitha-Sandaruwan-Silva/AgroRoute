package com.agroroute.features.settings

import com.agroroute.core.model.UserRole
import com.agroroute.core.notification.NotificationInboxItem
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class NotificationPresentationTest {
    private fun item(route: String) = NotificationInboxItem(
        id = "event-1", eventType = "ORDER_COMPLETED", title = "Order completed",
        body = "Your order is complete.", route = route, createdAt = 1, readAt = null
    )

    @Test
    fun `badge hides when read and caps counts without inventing unread items`() {
        assertNull(notificationBadge(0))
        assertEquals("1", notificationBadge(1))
        assertEquals("9", notificationBadge(9))
        assertEquals("9+", notificationBadge(10))
        assertEquals("9+", notificationBadge(100))
    }

    @Test
    fun `routes stay role specific and preserve supplied identifiers`() {
        assertEquals("farmer_order_detail/order-1", notificationDestination(item("farmer_order_detail/order-1"), UserRole.FARMER))
        assertEquals("supplier_order_detail/order-2", notificationDestination(item("supplier_order_detail/order-2"), UserRole.SUPPLIER))
        assertNull(notificationDestination(item("supplier_order_detail/order-2"), UserRole.FARMER))
        assertNull(notificationDestination(item("farmer_order_detail/order-1"), UserRole.SUPPLIER))
        assertNull(notificationDestination(item("supplier_order_detail/order-2"), null))
    }

    @Test
    fun `missing malformed or placeholder destinations read safely without navigation`() {
        for (route in listOf("", "supplier_order_detail/", "supplier_order_detail/{orderId}",
            "supplier_order_detail/a/b", "supplier_order_detail/..", "supplier_order_detail/a?b",
            "supplier_order_detail/a#b", "supplier_order_detail/a b", "unknown/order-1")) {
            assertNull(route, notificationDestination(item(route), UserRole.SUPPLIER))
        }
    }

    @Test
    fun `existing farmer request and bid destinations remain supported`() {
        assertEquals("farmer_order_requests", notificationDestination(item("farmer_order_requests"), UserRole.FARMER))
        assertEquals("view_bids/listing-1", notificationDestination(item("view_bids/listing-1"), UserRole.FARMER))
        assertEquals("supplier_bids", notificationDestination(item("supplier_bids"), UserRole.SUPPLIER))
    }

    @Test
    fun `date sections use local day boundaries including daylight saving without changing timestamps`() {
        val zone = ZoneId.of("America/New_York")
        val today = LocalDate.of(2026, 3, 9)
        val todayStart = today.atStartOfDay(zone).toInstant().toEpochMilli()
        val yesterdayStart = today.minusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
        assertEquals(NotificationDay.TODAY, notificationDay(todayStart, today, zone))
        assertEquals(NotificationDay.YESTERDAY, notificationDay(todayStart - 1, today, zone))
        assertEquals(NotificationDay.YESTERDAY, notificationDay(yesterdayStart, today, zone))
        assertEquals(NotificationDay.EARLIER, notificationDay(yesterdayStart - 1, today, zone))
    }
}
