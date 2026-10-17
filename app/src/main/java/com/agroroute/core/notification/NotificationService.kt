package com.agroroute.core.notification

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.CancellationException
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

    private val changes = MutableSharedFlow<Unit>(replay = 1)
    val inboxChanges = changes.asSharedFlow()

    /** Separate from the message commit: notification failure never makes a sent message retry. */
    suspend fun recordChatMessage(senderId: String, receiverId: String, conversationId: String, messageId: String) {
        if (senderId == receiverId || com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid != senderId) return
        val id = ChatNotification.eventId(conversationId, messageId)
        try {
            kotlinx.coroutines.withTimeout(5_000) {
                com.google.firebase.firestore.FirebaseFirestore.getInstance().collection("user_notifications")
                    .document(receiverId).collection("items").document(id).set(mapOf(
                        "id" to id, "entityId" to messageId, "conversationId" to conversationId,
                        "eventType" to NotificationEvent.CHAT_MESSAGE.name, "actorId" to senderId,
                        "recipientId" to receiverId, "createdAt" to com.google.firebase.firestore.FieldValue.serverTimestamp(),
                        "readAt" to null)).await()
            }
        } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
            Log.w("AgroRouteInbox", "Chat notification timed out after the message committed", e)
        } catch (e: CancellationException) { throw e }
        catch (e: Exception) { Log.w("AgroRouteInbox", "Chat notification was not recorded after message commit", e) }
    }

    /** Uses the existing account-scoped realtime session; no polling or push claims. */
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    suspend fun watchInbox(profile: com.agroroute.core.model.UserProfile) {
        val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
        val rows = kotlinx.coroutines.flow.callbackFlow<com.google.firebase.firestore.QuerySnapshot> {
            val registration = db.collection("user_notifications").document(profile.userId).collection("items")
                .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING).limit(100)
                .addSnapshotListener(com.google.firebase.firestore.MetadataChanges.INCLUDE) { snapshot, error ->
                    if (error != null) close(error) else if (snapshot != null) {
                        Log.d("AgroRouteInbox", "Inbox snapshot: count=${snapshot.size()} cache=${snapshot.metadata.isFromCache}")
                        trySend(snapshot)
                    }
                }
            awaitClose { registration.remove() }
        }.filter { !it.metadata.isFromCache }.retryWhen { cause, attempt ->
            if (com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid != profile.userId) false
            else {
                Log.w("AgroRouteInbox", "Inbox listener stopped; retrying", cause)
                kotlinx.coroutines.delay(((attempt + 1).coerceAtMost(6)) * 5_000)
                true
            }
        }
        val delivered = context.getSharedPreferences("chat-notifications:${profile.userId}", Context.MODE_PRIVATE)
        combine(rows, ChatNotificationVisibility.visible, NotificationDeliveryRefresh.changes) { snapshot, visible, _ -> snapshot to visible }
            .collect { (snapshot, visible) ->
                if (com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid != profile.userId) return@collect
                changes.emit(Unit)
                for (row in snapshot.documents) {
                    if (row.get("readAt") != null) continue
                    val event = runCatching { NotificationEvent.valueOf(row.getString("eventType").orEmpty()) }.getOrNull() ?: continue
                    val senderId = row.getString("actorId") ?: continue
                    if (senderId == profile.userId || row.getString("recipientId") != profile.userId) continue
                    if (event == NotificationEvent.CHAT_MESSAGE && visible == (profile.userId to senderId)) {
                        try { markInboxItemRead(row.id) }
                        catch (e: CancellationException) { throw e }
                        catch (e: Exception) { Log.w("AgroRouteInbox", "Could not mark visible chat notification read", e) }
                        continue
                    }
                    if (delivered.getBoolean(row.id, false)) continue
                    if (!NotificationHelper.areNotificationsEnabled(context, channelFor(event))) continue
                    try {
                        val item = SparkNotificationInbox.render(context, db, row, profile.role) ?: continue
                        if (com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid != profile.userId) return@collect
                        val posted = post(event, NotificationEvent.Payload(orderId = item.orderId),
                            route = item.route, title = item.title, body = item.body, eventId = item.id)
                        if (posted) delivered.edit().putBoolean(row.id, true).apply()
                    } catch (e: CancellationException) { throw e }
                    catch (e: Exception) { Log.w("AgroRouteInbox", "Could not render inbox notification", e) }
                }
            }
    }

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
        } catch (e: CancellationException) { throw e }
        catch (error: Exception) {
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
        cancelLocalNotification(notificationId)
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
    ): Boolean {
        val channelId = channelFor(event)
        NotificationChannels.ensureCreated(context)
        NotificationHelper.createOrGetChannel(context)
        if (!NotificationHelper.areNotificationsEnabled(context, channelId)) {
            Log.d("AgroRouteInbox", "Notification blocked by permission/app/channel settings: $channelId")
            return false
        }

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
        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(com.agroroute.R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setOnlyAlertOnce(event == NotificationEvent.CHAT_MESSAGE)
            .setPriority(
                if (event == NotificationEvent.CHAT_MESSAGE || event.importance == android.app.NotificationManager.IMPORTANCE_HIGH) {
                    NotificationCompat.PRIORITY_HIGH
                } else {
                    NotificationCompat.PRIORITY_DEFAULT
                }
            )
            .setCategory(if (event == NotificationEvent.CHAT_MESSAGE) NotificationCompat.CATEGORY_MESSAGE else NotificationCompat.CATEGORY_STATUS)
            .setContentIntent(pendingIntent)
            .build()

        val manager = notificationManager ?: return false
        try {
            manager.notify((eventId.ifBlank { payload.key + event.name }).hashCode(), notification)
        } catch (error: RuntimeException) {
            Log.w("AgroRouteInbox", "Android notification post failed: $channelId", error)
            return false
        }
        Log.d("AgroRouteInbox", "Android notification posted: ${event.name} channel=$channelId")
        return true
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
        internal fun channelFor(event: NotificationEvent): String = when {
            event == NotificationEvent.CHAT_MESSAGE -> NotificationHelper.MESSAGES_CHANNEL_ID
            event == NotificationEvent.HARVEST_REMINDER -> NotificationHelper.CHANNEL_ID
            event.name.startsWith("HARVEST_") -> NotificationChannels.LISTINGS
            event.name.startsWith("BID_") -> NotificationChannels.BIDS
            event.name.startsWith("ORDER_REQUEST_") -> NotificationChannels.ORDER_REQUESTS
            else -> NotificationChannels.ORDERS
        }

        const val LOCAL_HARVEST_PREFIX = "local-harvest:"
    }
}
