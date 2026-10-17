package com.agroroute.core.notification

import com.google.firebase.Timestamp
import org.junit.Assert.*
import org.junit.Test

class SparkNotificationInboxTest {
    @Test fun acceptedBidRendersFromTheCommittedOrderRatherThanTheBid() = kotlinx.coroutines.test.runTest {
        val db = io.mockk.mockk<com.google.firebase.firestore.FirebaseFirestore>()
        val row = io.mockk.mockk<com.google.firebase.firestore.DocumentSnapshot>(relaxed = true)
        val orders = io.mockk.mockk<com.google.firebase.firestore.CollectionReference>()
        val ref = io.mockk.mockk<com.google.firebase.firestore.DocumentReference>()
        val order = io.mockk.mockk<com.google.firebase.firestore.DocumentSnapshot>(relaxed = true)
        io.mockk.every { row.data } returns mapOf("entityId" to "bid-order")
        io.mockk.every { row.id } returns "BID_ACCEPTED_bid-order"
        io.mockk.every { row.getString("eventType") } returns "BID_ACCEPTED"
        io.mockk.every { row.getString("entityId") } returns "bid-order"
        io.mockk.every { row.getTimestamp("createdAt") } returns Timestamp(java.util.Date(1000))
        io.mockk.every { row.getTimestamp("readAt") } returns null
        io.mockk.every { db.collection("orders") } returns orders
        io.mockk.every { orders.document("bid-order") } returns ref
        io.mockk.every { ref.get(com.google.firebase.firestore.Source.SERVER) } returns com.google.android.gms.tasks.Tasks.forResult(order)
        io.mockk.every { order.exists() } returns true
        io.mockk.every { order.getString("cropName") } returns "Rice"
        val item = SparkNotificationInbox.render(io.mockk.mockk(relaxed = true), db, row,
            com.agroroute.core.model.UserRole.SUPPLIER)!!
        assertEquals("supplier_order_detail/bid-order", item.route)
        assertTrue(item.body.contains("Rice"))
        io.mockk.verify(exactly = 1) { db.collection("orders") }
    }

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
