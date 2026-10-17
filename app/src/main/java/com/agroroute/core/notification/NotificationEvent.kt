package com.agroroute.core.notification

/** Events whose outcome is surfaced to the user as a local OS notification. */
enum class NotificationEvent(
    val title: String,
    val content: (context: android.content.Context, payload: Payload) -> String,
    val importance: Int,
    val category: String = "other"
) {
    CHAT_MESSAGE(
        title = "New message", content = { _, _ -> "You have a new message." },
        importance = android.app.NotificationManager.IMPORTANCE_DEFAULT, category = "chat"
    ),
    /** Fires once when a farmer schedules an expected harvest on a live listing. */
    HARVEST_REMINDER(
        title = "Harvest reminder",
        content = { _, p ->
            val crop = p.cropName.takeIf { it.isNotBlank() } ?: "your harvest"
            "$crop is expected today."
        },
        importance = android.app.NotificationManager.IMPORTANCE_DEFAULT,
        category = "listing"
    ),

    HARVEST_READY(
        title = "Harvest ready",
        content = { _, p ->
            val crop = p.cropName.takeIf { it.isNotBlank() } ?: "your harvest"
            "$crop is ready. Check your order request for details."
        },
        importance = android.app.NotificationManager.IMPORTANCE_DEFAULT,
        category = "listing"
    ),

    HARVEST_UPDATE(
        title = "Harvest estimate updated",
        content = { _, p ->
            val crop = p.cropName.takeIf { it.isNotBlank() } ?: "a harvest"
            "The expected harvest details for $crop have changed."
        },
        importance = android.app.NotificationManager.IMPORTANCE_DEFAULT,
        category = "listing"
    ),

    HARVEST_FAILED(
        title = "Harvest unavailable",
        content = { _, p ->
            val crop = p.cropName.takeIf { it.isNotBlank() } ?: "the expected harvest"
            "$crop is no longer available."
        },
        importance = android.app.NotificationManager.IMPORTANCE_DEFAULT,
        category = "listing"
    ),

    /** Fires when a supplier's accepted bid becomes a confirmed order. */
    BID_ACCEPTED(
        title = "Bid accepted",
        content = { _, p ->
            val crop = p.cropName.takeIf { it.isNotBlank() } ?: "this harvest"
            "Your bid for $crop was accepted. Your order is now awaiting pickup."
        },
        importance = android.app.NotificationManager.IMPORTANCE_HIGH,
        category = "order"
    ),

    /** Fires when a farmer accepts a supplier's order request. */
    ORDER_REQUEST_ACCEPTED(
        title = "Order request accepted",
        content = { _, p ->
            val crop = p.cropName.takeIf { it.isNotBlank() } ?: "this harvest"
            "Your order request for $crop was accepted. Your order is awaiting pickup."
        },
        importance = android.app.NotificationManager.IMPORTANCE_HIGH,
        category = "order"
    ),

    /** Fires when a farmer rejects an order request. */
    ORDER_REQUEST_REJECTED(
        title = "Order request rejected",
        content = { _, p ->
            val crop = p.cropName.takeIf { it.isNotBlank() } ?: "your harvest"
            val reason = p.reason.takeIf { it.isNotBlank() } ?: ""
            "Your request for $crop was rejected${if (reason.isBlank()) "." else ": $reason"}"
        },
        importance = android.app.NotificationManager.IMPORTANCE_DEFAULT,
        category = "order"
    ),

    /** Fires when a supplier cancels a pending order request. */
    ORDER_REQUEST_CANCELLED(
        title = "Order request cancelled",
        content = { _, p ->
            val crop = p.cropName.takeIf { it.isNotBlank() } ?: "your harvest"
            "A supplier cancelled their request for $crop."
        },
        importance = android.app.NotificationManager.IMPORTANCE_DEFAULT,
        category = "order"
    ),
    /** Fires when a bid is rejected (farmer rejects a supplier's bid). */
    BID_REJECTED(
        title = "Bid rejected",
        content = { _, p ->
            val crop = p.cropName.takeIf { it.isNotBlank() } ?: "this harvest"
            "Your bid for $crop was rejected."
        },
        importance = android.app.NotificationManager.IMPORTANCE_DEFAULT,
        category = "bid"
    ),

    /** Fires when a supplier withdraws a pending bid. */
    BID_WITHDRAWN(
        title = "Bid withdrawn",
        content = { _, p ->
            val crop = p.cropName.takeIf { it.isNotBlank() } ?: "this harvest"
            "A supplier withdrew their bid for $crop."
        },
        importance = android.app.NotificationManager.IMPORTANCE_DEFAULT,
        category = "bid"
    ),

    /** Fires when a new bid is created (supplier places a bid). */
    BID_CREATED(
        title = "New bid",
        content = { _, p ->
            val crop = p.cropName.takeIf { it.isNotBlank() } ?: "this harvest"
            val supplier = p.supplierName.takeIf { it.isNotBlank() } ?: "a supplier"
            "$supplier placed a bid for $crop."
        },
        importance = android.app.NotificationManager.IMPORTANCE_DEFAULT,
        category = "bid"
    ),

    /** Fires when an order request is created (supplier places a request). */
    ORDER_REQUEST_CREATED(
        title = "Order request created",
        content = { _, p ->
            val crop = p.cropName.takeIf { it.isNotBlank() } ?: "this harvest"
            val supplier = p.supplierName.takeIf { it.isNotBlank() } ?: "A supplier"
            "$supplier sent an order request for $crop. Review it in your requests."
        },
        importance = android.app.NotificationManager.IMPORTANCE_DEFAULT,
        category = "order"
    ),

    /** Fires when an order is marked in transit. */
    ORDER_IN_TRANSIT(
        title = "Order in transit",
        content = { _, p ->
            "${p.cropName.ifBlank { "Your produce" }} order is now in transit."
        },
        importance = android.app.NotificationManager.IMPORTANCE_DEFAULT,
        category = "order"
    ),

    /** Fires when an order is cancelled. */
    ORDER_CANCELLED(
        title = "Order cancelled",
        content = { _, p ->
            "${p.cropName.ifBlank { "Your produce" }} order was cancelled."
        },
        importance = android.app.NotificationManager.IMPORTANCE_DEFAULT,
        category = "order"
    ),

    /** Fires when an order is completed (delivered). */
    ORDER_COMPLETED(
        title = "Order completed",
        content = { _, p ->
            "${p.cropName.ifBlank { "Your produce" }} order has been completed."
        },
        importance = android.app.NotificationManager.IMPORTANCE_DEFAULT,
        category = "order"
    ),
    ;
    data class Payload(
        val cropName: String = "",
        val supplierName: String = "",
        val reason: String = "",
        val orderId: String = "",
        val bidId: String = "",
        val requestId: String = "",
        val status: String = "",
        val listingId: String = ""
    ) : java.io.Serializable {
        val key: String
            get() = orderId.takeIf { it.isNotBlank() }
                ?: bidId.takeIf { it.isNotBlank() }
                ?: requestId.takeIf { it.isNotBlank() }
                ?: listingId.takeIf { it.isNotBlank() }
                ?: cropName.takeIf { it.isNotBlank() }
                ?: ""

        fun toMap(): Map<String, String> = mapOf(
            "crop_name" to cropName,
            "supplier_name" to supplierName,
            "reason" to reason,
            "order_id" to orderId,
            "bid_id" to bidId,
            "request_id" to requestId,
            "status" to status,
            "listing_id" to listingId
        ).filterValues { it.isNotBlank() }
    }

    companion object {
        /**
         * Parses a Firebase `data` map into the corresponding [NotificationEvent],
         * or `null` if the payload does not identify a known event.
         */
        fun from(data: Map<String, String>): NotificationEvent? {
            val eventType = data["event_type"] ?: return null
            val payload = Payload(
                orderId = data[NotificationAction.EXTRA_ORDER_ID] ?: data["order_id"] ?: "",
                bidId = data[NotificationAction.EXTRA_BID_ID] ?: data["bid_id"] ?: "",
                requestId = data["request_id"] ?: "",
                cropName = data["crop_name"] ?: data["cropName"] ?: "",
                supplierName = data["supplier_name"] ?: data["supplierName"] ?: "",
                reason = data["reason"] ?: "",
                status = data["status"] ?: "",
                listingId = data["listing_id"] ?: data["listingId"] ?: ""
            )

            return when (eventType) {
                "CHAT_MESSAGE" -> CHAT_MESSAGE
                "ORDER_REQUEST_CREATED" -> ORDER_REQUEST_CREATED
                "ORDER_REQUEST_ACCEPTED" -> ORDER_REQUEST_ACCEPTED
                "ORDER_REQUEST_REJECTED" -> ORDER_REQUEST_REJECTED
                "ORDER_REQUEST_CANCELLED" -> ORDER_REQUEST_CANCELLED
                "BID_CREATED" -> BID_CREATED
                "BID_ACCEPTED" -> BID_ACCEPTED
                "BID_REJECTED" -> BID_REJECTED
                "BID_WITHDRAWN" -> BID_WITHDRAWN
                "ORDER_IN_TRANSIT" -> ORDER_IN_TRANSIT
                "ORDER_CANCELLED" -> ORDER_CANCELLED
                "ORDER_COMPLETED" -> ORDER_COMPLETED
                "HARVEST_READY" -> HARVEST_READY
                "HARVEST_FAILED" -> HARVEST_FAILED
                "HARVEST_UPDATE" -> HARVEST_UPDATE
                "HARVEST_REMINDER" -> HARVEST_REMINDER
                else -> null
            }.takeIf { it != null }
        }
    }
}
