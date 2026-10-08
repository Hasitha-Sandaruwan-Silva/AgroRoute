package com.agroroute.core.notification

import com.agroroute.core.model.Bid
import com.agroroute.core.model.BidStatus
import com.agroroute.core.model.Order
import com.agroroute.core.model.OrderRequest
import com.agroroute.core.model.OrderRequestStatus
import com.agroroute.core.model.OrderStatus
import com.agroroute.core.model.UserRole

/**
 * Pure helpers that map domain events to notification title/body text.
 */
object NotificationMessageFactory {

    data class Message(
        val channelId: String,
        val title: String,
        val body: String,
        val notificationId: Int,
        val deepLinkRoute: String? = null
    )

    fun harvestReminder(cropName: String, listingId: String): Message =
        Message(
            channelId = NotificationChannels.HARVEST_REMINDERS,
            title = "Harvest reminder",
            body = if (cropName.isBlank()) {
                "Your harvest is expected today."
            } else {
                "$cropName is expected today."
            },
            notificationId = listingId.hashCode(),
            deepLinkRoute = null
        )

    fun forNewBid(bid: Bid, cropHint: String? = null): Message {
        val crop = cropHint?.takeIf { it.isNotBlank() } ?: "your listing"
        return Message(
            channelId = NotificationChannels.BIDS,
            title = "New bid received",
            body = "${bid.supplierName.ifBlank { "A supplier" }} offered LKR ${formatPrice(bid.offerPrice)} on $crop.",
            notificationId = ("bid_new_" + bid.bidId).hashCode()
        )
    }

    fun forBidStatus(bid: Bid, role: UserRole): Message? {
        if (role != UserRole.SUPPLIER) return null
        return when (bid.status) {
            BidStatus.ACCEPTED -> Message(
                channelId = NotificationChannels.BIDS,
                title = "Bid accepted",
                body = "Your bid of LKR ${formatPrice(bid.offerPrice)} was accepted.",
                notificationId = ("bid_status_" + bid.bidId).hashCode()
            )
            BidStatus.REJECTED -> Message(
                channelId = NotificationChannels.BIDS,
                title = "Bid rejected",
                body = "Your bid of LKR ${formatPrice(bid.offerPrice)} was rejected.",
                notificationId = ("bid_status_" + bid.bidId).hashCode()
            )
            else -> null
        }
    }

    fun forNewOrderRequest(request: OrderRequest): Message =
        Message(
            channelId = NotificationChannels.ORDER_REQUESTS,
            title = "New order request",
            body = "${request.supplierName.ifBlank { "A supplier" }} requested ${formatQty(request.requestedQuantity)} of ${request.cropName.ifBlank { "produce" }}.",
            notificationId = ("or_new_" + request.requestId).hashCode()
        )

    fun forOrderRequestStatus(request: OrderRequest, role: UserRole): Message? {
        if (role != UserRole.SUPPLIER) return null
        return when (request.status) {
            OrderRequestStatus.ACCEPTED -> Message(
                channelId = NotificationChannels.ORDER_REQUESTS,
                title = "Order request accepted",
                body = "Your request for ${request.cropName.ifBlank { "produce" }} was accepted.",
                notificationId = ("or_status_" + request.requestId).hashCode()
            )
            OrderRequestStatus.REJECTED -> Message(
                channelId = NotificationChannels.ORDER_REQUESTS,
                title = "Order request rejected",
                body = "Your request for ${request.cropName.ifBlank { "produce" }} was rejected.",
                notificationId = ("or_status_" + request.requestId).hashCode()
            )
            else -> null
        }
    }

    fun forNewOrder(order: Order): Message =
        Message(
            channelId = NotificationChannels.ORDERS,
            title = "New order",
            body = "${order.cropName.ifBlank { "Produce" }} order is awaiting pickup.",
            notificationId = ("order_new_" + order.orderId).hashCode()
        )

    fun forOrderStatus(order: Order): Message? {
        val statusLabel = when (order.status) {
            OrderStatus.AWAITING_PICKUP -> return null // covered by new-order
            OrderStatus.IN_TRANSIT -> "in transit"
            OrderStatus.DELIVERED -> "delivered"
            OrderStatus.CANCELLED -> "cancelled"
        }
        return Message(
            channelId = NotificationChannels.ORDERS,
            title = "Order update",
            body = "${order.cropName.ifBlank { "Your order" }} is now $statusLabel.",
            notificationId = ("order_status_" + order.orderId + "_" + order.status.name).hashCode()
        )
    }

    fun fromFcmData(data: Map<String, String>): Message? {
        val title = data["title"]?.takeIf { it.isNotBlank() } ?: return null
        val body = data["body"]?.takeIf { it.isNotBlank() } ?: return null
        val type = data["type"].orEmpty()
        val channelId = when {
            type.startsWith("bid") -> NotificationChannels.BIDS
            type.startsWith("order_request") -> NotificationChannels.ORDER_REQUESTS
            type.startsWith("order") -> NotificationChannels.ORDERS
            type.startsWith("listing") || type.startsWith("harvest") -> NotificationChannels.LISTINGS
            else -> data["channelId"] ?: NotificationChannels.GENERAL
        }
        val idKey = data["targetId"] ?: data["collapseKey"] ?: (title + body)
        return Message(
            channelId = channelId,
            title = title,
            body = body,
            notificationId = idKey.hashCode()
        )
    }

    private fun formatPrice(value: Double): String =
        if (value == value.toLong().toDouble()) value.toLong().toString() else "%.2f".format(value)

    private fun formatQty(value: Double): String =
        if (value == value.toLong().toDouble()) value.toLong().toString() else "%.1f".format(value)
}
