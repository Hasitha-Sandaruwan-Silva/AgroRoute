package com.agroroute.core.notification

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Handles Firebase Cloud Messaging events.
 *
 * - [onNewToken] refreshes the FCM token and syncs it to the trusted server via
 *   [NotificationService.registerDeviceToken].
 * - [onMessageReceived] handles data-only messages in foreground, background and
 *   process recreation, then posts one local notification with its deep link.
 */
class FirebaseNotificationService : FirebaseMessagingService() {
    private val notificationService by lazy { NotificationService(this) }
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        if (!com.agroroute.core.runtime.BackgroundNetworkAccess.allowed.value) return
        val currentUserId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
        currentUserId?.let { userId ->
            serviceScope.launch { if (com.agroroute.core.runtime.BackgroundNetworkAccess.allowed.value) notificationService.registerDeviceToken(userId, token) }
        }
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        val data = remoteMessage.data
        if (data.isEmpty()) return
        val signedInUserId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: return
        if (data["recipient_user_id"] != signedInUserId) return

        val event = NotificationEvent.from(data) ?: return
        val payload = NotificationEvent.Payload(
            orderId = data[NotificationAction.EXTRA_ORDER_ID] ?: "",
            bidId = data[NotificationAction.EXTRA_BID_ID] ?: "",
            requestId = data[NotificationAction.EXTRA_REQUEST_ID] ?: "",
            cropName = data[NotificationAction.EXTRA_CROP_NAME] ?: "",
            supplierName = data["supplier_name"] ?: "",
            reason = data["reason"] ?: "",
            status = data["status"] ?: "",
            listingId = data[NotificationAction.EXTRA_LISTING_ID] ?: ""
        )

        notificationService.post(
            event = event,
            payload = payload,
            route = data[NotificationAction.EXTRA_ROUTE].orEmpty(),
            title = data["title"] ?: event.title,
            body = data["body"] ?: event.content(this, payload),
            eventId = data[NotificationAction.EXTRA_EVENT_ID].orEmpty()
        )
    }

    override fun onDeletedMessages() {
        super.onDeletedMessages()
        if (!com.agroroute.core.runtime.BackgroundNetworkAccess.allowed.value) return
        // FCM may delete messages when the device is offline or under bulk deletion;
        // re-register the token to ensure resync.
        FirebaseMessaging.getInstance().token.addOnSuccessListener { token ->
            val currentUserId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
            currentUserId?.let { userId ->
                serviceScope.launch { if (com.agroroute.core.runtime.BackgroundNetworkAccess.allowed.value) notificationService.registerDeviceToken(userId, token) }
            }
        }
    }

}
