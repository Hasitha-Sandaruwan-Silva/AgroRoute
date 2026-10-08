package com.agroroute.core.notification

/** Events whose outcome is surfaced to the user as a local OS notification. */
enum class NotificationEvent(
    val title: String,
    val content: (context: android.content.Context, payload: Payload) -> String,
    val importance: android.app.NotificationManager.IMPORTANCE,
    val category: String = "other"
) {
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

    /** Fires when a supplier's accepted bid becomes a confirmed order. */
    BID_ACCEPTED(
        title = "Bid accepted",
        content = { context, p ->
            val crop = p.cropName.takeIf { it.isNotBlank() } ?: "this harvest"
            val supplier = p.supplierName.takeIf { it.isNotBlank() } ?: "a supplier"
            "$supplier accepted your bid for $crop. Your order is now awaiting pickup."
        },
        importance = android.app.NotificationManager.IMPORTANCE_HIGH,
        category = "order"
    ),

    /** Fires when a farmer accepts a supplier's order request. */
    ORDER_REQUEST_ACCEPTED(
        title = "Order request accepted",
        content = { context, p ->
            val crop = p.cropName.takeIf { it.isNotBlank() } ?: "this harvest"
            val supplier = p.supplierName.takeIf { it.isNotBlank() } ?: "a supplier"
            "$supplier's request for $crop has been accepted. Your order is now awaiting pickup."
        },
        importance = android.app.NotificationManager.IMPORTANCE_HIGH,
        category = "order"
    ),

    /** Fires when a farmer rejects an order request. */
    ORDER_REQUEST_REJECTED(
        title = "Order request rejected",
        content = { _, p ->
            val reason = p.reason.takeIf { it.isNotBlank() } ?: "No reason provided."
            "Your order request was rejected. $reason"
        },
        importance = android.app.NotificationManager.IMPORTANCE_DEFAULT,
        category = "order"
    ),

    /** Fires when a supplier cancels a pending order request. */
    ORDER_REQUEST_CANCELLED(
        title = "Order request cancelled",
        content = { _, p ->
            val reason = p.reason.takeIf { it.isNotBlank() } ?: "No reason provided."
            "The order request was cancelled. $reason"
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
        val status: String = "",
        val listingId: String = ""
    )
}
