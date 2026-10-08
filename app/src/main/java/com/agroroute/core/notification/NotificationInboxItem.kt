package com.agroroute.core.notification

data class NotificationInboxItem(
    val id: String,
    val eventType: String,
    val title: String,
    val body: String,
    val route: String,
    val createdAt: Long,
    val readAt: Long?,
    val orderId: String = "",
    val bidId: String = "",
    val requestId: String = "",
    val listingId: String = ""
) {
    val isRead: Boolean get() = readAt != null

    companion object {
        fun from(data: Map<*, *>): NotificationInboxItem? {
            val id = data["id"] as? String ?: return null
            val eventType = data["eventType"] as? String ?: return null
            val title = data["title"] as? String ?: return null
            val body = data["body"] as? String ?: return null
            val route = data["route"] as? String ?: return null
            val createdAt = (data["createdAt"] as? Number)?.toLong() ?: return null
            val readAt = (data["readAt"] as? Number)?.toLong()
            return NotificationInboxItem(
                id = id,
                eventType = eventType,
                title = title,
                body = body,
                route = route,
                createdAt = createdAt,
                readAt = readAt,
                orderId = data["orderId"] as? String ?: "",
                bidId = data["bidId"] as? String ?: "",
                requestId = data["requestId"] as? String ?: "",
                listingId = data["listingId"] as? String ?: ""
            )
        }
    }
}
