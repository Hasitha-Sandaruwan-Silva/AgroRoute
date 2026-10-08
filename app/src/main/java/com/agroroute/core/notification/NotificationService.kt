package com.agroroute.core.notification

import android.app.NotificationManager
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Posts application-level notifications for order, bid, order-request, and listing
 * lifecycle events. This does not handle push (FCM) delivery — the app in the repo
 * uses local alarms and local notifications only, so this is the complete set of
 * notifications the existing system can produce.
 */
@Singleton
class NotificationService @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val manager: NotificationManager get() = context.getSystemService(NotificationManager::class.java)

    /** Pre-warms the shared notification channel before any notification is issued. */
    fun ensureChannel() {
        NotificationHelper.createOrGetChannel(context)
    }

    fun post(event: NotificationEvent, payload: NotificationEvent.Payload) {
        if (!NotificationHelper.hasNotificationPermission(context)) return
        ensureChannel()

        val notification = android.app.Notification.Builder(context, NotificationHelper.CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(event.title)
            .setContentText(event.content(context, payload))
            .setAutoCancel(true)
            .setPriority(event.importance)
            .setCategory(android.app.NotificationCompat.CATEGORY_PROGRESS)
            .build()

        manager.notify(payload.orderId.hashCode() + event.ordinal, notification)
    }

    fun cancelOrderSuggestion(orderId: String) {
        manager?.cancel(orderId.hashCode())
    }

    fun cancelAll() {
        manager?.cancelAll()
    }
}
