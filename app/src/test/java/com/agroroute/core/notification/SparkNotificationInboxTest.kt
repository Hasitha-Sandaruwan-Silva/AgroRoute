package com.agroroute.core.notification

import com.google.firebase.Timestamp
import org.junit.Assert.*
import org.junit.Test

class SparkNotificationInboxTest {
    @Test fun existingRepositoryPayloadsResolveTheirPersistedEntityWithoutInventedIds() {
        val acceptedBid = NotificationEvent.Payload(orderId = "bid-order")
        assertEquals("bid-order", SparkNotificationInbox.entity(NotificationEvent.BID_ACCEPTED, acceptedBid))
        val createdRequest = NotificationEvent.Payload(orderId = "request")
        assertEquals("request", SparkNotificationInbox.entity(NotificationEvent.ORDER_REQUEST_CREATED, createdRequest))
        val harvest = NotificationEvent.Payload(requestId = "preharvest", listingId = "listing")
        assertEquals("preharvest", SparkNotificationInbox.entity(NotificationEvent.HARVEST_READY, harvest))
    }
    @Test fun legacyInboxRemainsReadableAfterServerTimestampReadAcknowledgement() {
        val row = mapOf("id" to "old", "eventType" to "ORDER_COMPLETED", "title" to "Delivered",
            "body" to "Order delivered", "route" to "farmer_order_detail/order", "createdAt" to 1L,
            "readAt" to Timestamp(java.util.Date(2000)))
        val item = NotificationInboxItem.from(row)!!
        assertEquals(1L,item.createdAt); assertEquals(2000L,item.readAt); assertTrue(item.isRead)
    }
}
