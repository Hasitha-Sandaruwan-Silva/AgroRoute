package com.agroroute.core.notification

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NotificationInboxItemTest {
    @Test
    fun parsesInboxItemFieldsAndUnreadState() {
        val item = NotificationInboxItem.from(
            mapOf(
                "id" to "a".repeat(64),
                "eventType" to "BID_CREATED",
                "title" to "New bid",
                "body" to "A supplier placed a bid.",
                "route" to "view_bids/listing-1",
                "createdAt" to 1_750_000_000_000L,
                "readAt" to null,
                "listingId" to "listing-1"
            )
        )

        requireNotNull(item)
        assertEquals("view_bids/listing-1", item.route)
        assertEquals("listing-1", item.listingId)
        assertFalse(item.isRead)
        assertNull(item.readAt)
    }

    @Test
    fun parsesReadStateAndRejectsIncompleteItems() {
        val item = NotificationInboxItem.from(
            mapOf(
                "id" to "b".repeat(64),
                "eventType" to "ORDER_COMPLETED",
                "title" to "Order completed",
                "body" to "Your order was delivered.",
                "route" to "farmer_order_detail/order-2",
                "createdAt" to 1_750_000_000_000L,
                "readAt" to 1_750_000_100_000L
            )
        )

        assertTrue(item?.isRead == true)
        assertNull(NotificationInboxItem.from(mapOf("id" to "partial")))
    }
}
