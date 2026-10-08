package com.agroroute.core.notification

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Push events are authorized and delivered by Cloud Functions. FCM data messages are
 * rendered here in every app state so each logical event has one tappable notification.
 */
@Singleton
class NotificationService @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? android.app.NotificationManager

    /** Returns the event encoded by a Firebase `data` map, or `null` if unknown. */
    fun fromData(data: Map<String, String>): NotificationEvent? = NotificationEvent.from(data)

    fun post(
        event: NotificationEvent,
        payload: NotificationEvent.Payload,
        route: String = "",
        title: String = event.title,
        body: String = event.content(context, payload),
        eventId: String = ""
    ) {
        if (!NotificationHelper.hasNotificationPermission(context)) return
        NotificationHelper.createOrGetChannel(context)

        val launchIntent = Intent(context, com.agroroute.MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(NotificationAction.EXTRA_ROUTE, route)
            putExtra(NotificationAction.EXTRA_ORDER_ID, payload.orderId)
            putExtra(NotificationAction.EXTRA_BID_ID, payload.bidId)
            putExtra(NotificationAction.EXTRA_LISTING_ID, payload.listingId)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            (eventId.ifBlank { payload.key + event.name }).hashCode(),
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, NotificationHelper.CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setPriority(
                if (event.importance == android.app.NotificationManager.IMPORTANCE_HIGH) {
                    NotificationCompat.PRIORITY_HIGH
                } else {
                    NotificationCompat.PRIORITY_DEFAULT
                }
            )
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager?.notify((eventId.ifBlank { payload.key + event.name }).hashCode(), notification)
    }

    /** Sends a token refresh to the trusted backend. */
    suspend fun registerDeviceToken(userId: String, token: String) {
        if (com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid != userId) return
        try {
            firebaseFunctions.getHttpsCallable("registerDeviceToken").call(mapOf("token" to token)).await()
        } catch (error: Exception) {
            Log.e("AgroRouteFCM", "Failed to register device token", error)
        }
    }

    suspend fun unregisterDeviceToken(userId: String) {
        if (com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid != userId) return
        try {
            val token = com.google.firebase.messaging.FirebaseMessaging.getInstance().token.await()
            firebaseFunctions.getHttpsCallable("unregisterDeviceToken").call(
                mapOf("userId" to userId, "token" to token)
            ).await()
        } catch (error: Exception) {
            Log.e("AgroRouteFCM", "Failed to unregister device token", error)
        }
    }

    /**
    * Sends a push through the trusted backend, which verifies the event and recipient
    * from Firestore and sends data-only FCM.
     */
    suspend fun sendPushToServer(
        recipientUserId: String,
        event: NotificationEvent,
        payload: NotificationEvent.Payload,
        actorUserId: String
    ) {
        try {
            firebaseFunctions.getHttpsCallable("sendNotification").call(
                mapOf(
                    "recipientUserId" to recipientUserId,
                    "actorUserId" to actorUserId,
                    "eventType" to event.name,
                    "payload" to payload.toMap()
                )
            ).await()
        } catch (error: Exception) {
            Log.e("AgroRouteFCM", "sendPushToServer failed", error)
        }
    }

    fun cancelLocalNotification(key: String) {
        notificationManager?.cancel(key.hashCode())
    }

    fun cancelAllLocal() {
        notificationManager?.cancelAll()
    }

    private val firebaseFunctions get() = com.google.firebase.functions.FirebaseFunctions.getInstance()
}
