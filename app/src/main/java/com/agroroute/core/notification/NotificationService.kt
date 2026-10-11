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
 * Spark inbox events are authorized by Firestore Rules. Optional future FCM messages are
 * rendered here in every app state so each logical event has one tappable notification.
 */
@Singleton
class NotificationService @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? android.app.NotificationManager

    /** Returns the event encoded by a Firebase `data` map, or `null` if unknown. */
    fun fromData(data: Map<String, String>): NotificationEvent? = NotificationEvent.from(data)

    suspend fun getInboxItems(limit: Int = 50): List<NotificationInboxItem> {
        val userId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: return emptyList()
        val localItems = LocalNotificationInbox.items(context, userId)
        val remoteItems = try {
            val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
            val role = db.collection("users").document(userId).get(com.google.firebase.firestore.Source.SERVER).await()
                .getString("role")?.let { com.agroroute.core.model.UserRole.valueOf(it) }
                ?: error("Account profile is unavailable")
            val rows = db.collection("user_notifications").document(userId).collection("items")
                .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(limit.coerceIn(1, 100).toLong()).get(com.google.firebase.firestore.Source.SERVER).await()
            rows.documents.mapNotNull { row -> SparkNotificationInbox.render(context, db, row, role) }
        } catch (error: Exception) {
            if (localItems.isNotEmpty()) return localItems
            throw error
        }
        return (remoteItems + localItems).distinctBy { it.id }
            .sortedByDescending { it.createdAt }
            .take(limit.coerceIn(1, 100))
    }

    suspend fun markInboxItemRead(notificationId: String) {
        val userId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
            ?: error("Sign in to update notifications.")
        if (notificationId.startsWith(LOCAL_HARVEST_PREFIX)) {
            LocalNotificationInbox.markRead(context, userId, notificationId)
            return
        }
        val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
        db.runTransaction { tx ->
            val ref = db.collection("user_notifications").document(userId).collection("items").document(notificationId)
            val row = tx.get(ref)
            if (row.exists() && row.get("readAt") == null)
                tx.update(ref, "readAt", com.google.firebase.firestore.FieldValue.serverTimestamp())
        }.await()
    }

    fun postHarvestReminder(farmerId: String, eventId: String, payload: NotificationEvent.Payload) {
        if (com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid != farmerId) return
        LocalNotificationInbox.upsert(
            context,
            farmerId,
            NotificationInboxItem(
                id = eventId,
                eventType = NotificationEvent.HARVEST_REMINDER.name,
                title = NotificationEvent.HARVEST_REMINDER.title,
                body = NotificationEvent.HARVEST_REMINDER.content(context, payload),
                route = com.agroroute.core.navigation.Screen.EditListing.createRoute(payload.listingId),
                createdAt = System.currentTimeMillis(),
                readAt = null,
                listingId = payload.listingId
            )
        )
        post(
            event = NotificationEvent.HARVEST_REMINDER,
            payload = payload,
            route = com.agroroute.core.navigation.Screen.EditListing.createRoute(payload.listingId),
            eventId = eventId
        )
    }

    fun post(
        event: NotificationEvent,
        payload: NotificationEvent.Payload,
        route: String = "",
        title: String = event.title,
        body: String = event.content(context, payload),
        eventId: String = ""
    ) {
        if (!NotificationHelper.areNotificationsEnabled(context)) return
        NotificationHelper.createOrGetChannel(context)

        val launchIntent = Intent(context, com.agroroute.MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(NotificationAction.EXTRA_ROUTE, route)
            putExtra(NotificationAction.EXTRA_ORDER_ID, payload.orderId)
            putExtra(NotificationAction.EXTRA_BID_ID, payload.bidId)
            putExtra(NotificationAction.EXTRA_LISTING_ID, payload.listingId)
            putExtra(NotificationAction.EXTRA_EVENT_ID, eventId)
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

    // Spark has no trusted FCM sender. Preserve bootstrap API without unavailable callables.
    suspend fun registerDeviceToken(userId: String, token: String) = Unit
    suspend fun unregisterDeviceToken(userId: String) = Unit

    /** Best-effort existing inbox write, constrained by recipient/resource/state Rules. No FCM. */
    suspend fun sendPushToServer(recipientUserId: String, event: NotificationEvent,
        payload: NotificationEvent.Payload, actorUserId: String) {
        if (com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid != actorUserId) return
        val entity = SparkNotificationInbox.entity(event, payload)
        if (entity.isBlank() || entity.contains('/') || recipientUserId == actorUserId) return
        val id = "${event.name}_$entity"
        try {
            val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
            val ref = db.collection("user_notifications").document(recipientUserId).collection("items").document(id)
            // Sender cannot read the recipient inbox; create-once Rules reject retries harmlessly.
            kotlinx.coroutines.withTimeout(5_000) {
                ref.set(mapOf("id" to id, "entityId" to entity, "eventType" to event.name,
                    "actorId" to actorUserId, "recipientId" to recipientUserId,
                    "createdAt" to com.google.firebase.firestore.FieldValue.serverTimestamp(), "readAt" to null)).await()
            }
        } catch (error: Exception) {
            Log.w("AgroRouteInbox", "Inbox event was not recorded (${error.javaClass.simpleName})")
        }
    }

    fun cancelLocalNotification(key: String) {
        notificationManager?.cancel(key.hashCode())
    }

    fun cancelAllLocal() {
        notificationManager?.cancelAll()
    }


    companion object {
        const val LOCAL_HARVEST_PREFIX = "local-harvest:"
    }
}
