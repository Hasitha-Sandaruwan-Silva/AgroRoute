package com.agroroute.core.notification

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Handles FCM data messages while the process is alive (including foreground).
 *
 * Notification-payload messages delivered while the app is backgrounded/closed are shown by
 * the system tray; this service still receives data payloads and token refresh callbacks.
 */
@AndroidEntryPoint
class AgroRouteFirebaseMessagingService : FirebaseMessagingService() {

    @Inject lateinit var publisher: AppNotificationPublisher

    override fun onNewToken(token: String) {
        Log.d(TAG, "FCM token refreshed (length=${token.length})")
        // Topics are re-synced by NotificationBootstrap on auth; no Firestore write (rules).
    }

    override fun onMessageReceived(message: RemoteMessage) {
        NotificationChannels.ensureCreated(this)
        val dataMessage = NotificationMessageFactory.fromFcmData(message.data)
        if (dataMessage != null) {
            publisher.publish(dataMessage)
            return
        }
        val notification = message.notification ?: return
        publisher.publish(
            NotificationMessageFactory.Message(
                channelId = NotificationChannels.GENERAL,
                title = notification.title.orEmpty().ifBlank { "AgroRoute" },
                body = notification.body.orEmpty().ifBlank { "You have a new update." },
                notificationId = (message.messageId ?: System.currentTimeMillis().toString()).hashCode()
            )
        )
    }

    companion object {
        private const val TAG = "AgroRouteFcm"
    }
}
