package com.agroroute.core.notification

import android.content.Context
import com.agroroute.core.model.UserRole
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import kotlinx.coroutines.tasks.await

/** Existing inbox, reference-only Spark records: displayed text/routes come from persisted resources. */
internal object SparkNotificationInbox {
    fun entity(event: NotificationEvent, p: NotificationEvent.Payload): String = when {
        event.name.startsWith("BID_") -> p.bidId.ifBlank { p.orderId }
        event.name.startsWith("ORDER_REQUEST_") || event.name.startsWith("HARVEST_") -> p.requestId.ifBlank { p.orderId }
        else -> p.orderId
    }
    suspend fun render(context: Context, db: FirebaseFirestore, row: DocumentSnapshot, role: UserRole): NotificationInboxItem? {
        val data = row.data ?: return null
        if (!data.containsKey("entityId")) return NotificationInboxItem.from(data)
        val event = runCatching { NotificationEvent.valueOf(row.getString("eventType").orEmpty()) }.getOrNull() ?: return null
        val id = row.getString("entityId") ?: return null
        if (event == NotificationEvent.CHAT_MESSAGE) {
            val conversationId = row.getString("conversationId") ?: return null
            val message = db.collection("conversations").document(conversationId).collection("messages")
                .document(id).get(Source.SERVER).await()
            val senderId = message.getString("senderId") ?: return null
            if (senderId != row.getString("actorId")) return null
            val sender = db.collection("users").document(senderId).get(Source.SERVER).await()
            return ChatNotification.item(row.id, senderId, sender.getString("displayName").orEmpty(),
                message.getString("text").orEmpty(), row.getTimestamp("createdAt")?.toDate()?.time ?: return null,
                row.getTimestamp("readAt")?.toDate()?.time, message.getString("orderId").orEmpty())
        }
        val collection = when {
            event == NotificationEvent.BID_ACCEPTED -> "orders"
            event.name.startsWith("BID_") -> "bids"
            event.name.startsWith("ORDER_REQUEST_") || event.name.startsWith("HARVEST_") -> "order_requests"
            else -> "orders"
        }
        val source = db.collection(collection).document(id).get(Source.SERVER).await()
        if (!source.exists()) return null
        val p = NotificationEvent.Payload(orderId = if (event in listOf(NotificationEvent.BID_ACCEPTED,
                NotificationEvent.ORDER_REQUEST_ACCEPTED) || collection == "orders") id else "",
            bidId = if (collection == "bids") id else "", requestId = if (collection == "order_requests") id else "",
            listingId = source.getString("listingId").orEmpty(), cropName = source.getString("cropName").orEmpty(),
            supplierName = source.getString("supplierName").orEmpty())
        val route = NotificationRoute.forEvent(event, role, p) ?: return null
        return NotificationInboxItem(row.id, event.name, event.title, event.content(context, p), route,
            row.getTimestamp("createdAt")?.toDate()?.time ?: return null,
            row.getTimestamp("readAt")?.toDate()?.time, p.orderId, p.bidId, p.requestId, p.listingId)
    }
}
