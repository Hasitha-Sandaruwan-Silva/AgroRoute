package com.agroroute.core.notification

import com.agroroute.features.participants.ParticipantRoutes
import kotlinx.coroutines.flow.MutableStateFlow

/** Reference-only inbox records; names and previews are rendered from the saved sources. */
object ChatNotification {
    fun eventId(conversationId: String, messageId: String) = "CHAT_MESSAGE_${conversationId}_$messageId"
    fun preview(text: String): String {
        val clean = text.replace(Regex("\\s+"), " ").trim()
        return if (clean.codePointCount(0, clean.length) <= 120) clean
            else clean.substring(0, clean.offsetByCodePoints(0, 119)) + "\u2026"
    }
    fun item(id: String, senderId: String, senderName: String, text: String, createdAt: Long,
        readAt: Long?, orderId: String = ""): NotificationInboxItem = NotificationInboxItem(
        id, NotificationEvent.CHAT_MESSAGE.name, "New message from ${senderName.ifBlank { "a participant" }}",
        (if (orderId.isBlank()) "" else "Order #${orderId.takeLast(12)} \u2022 ") + preview(text),
        ParticipantRoutes.chat(senderId, orderId), createdAt, readAt, orderId = orderId)
}

/** Only the resumed conversation suppresses a local alert; inbox read state still updates. */
internal object ChatNotificationVisibility {
    val visible = MutableStateFlow<Pair<String, String>?>(null)
}

/** Reconsider unread events when notification settings change; never poll Firestore. */
internal object NotificationDeliveryRefresh {
    val changes = MutableStateFlow(0L)
    fun refresh() { changes.value += 1 }
}
