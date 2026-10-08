package com.agroroute.core.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat

/** Immutable, idempotent helpers for creating and issuing notifications. */
object NotificationHelper {

    const val CHANNEL_ID = "agroroute_notifications"

    /** Channel id supplied for the order/bid/order-request suggestion intent. */
    internal const val ORDER_SUGGESTION_CHANNEL_ID = "agraroute_order_suggestions"

    /** Whether the app has the `POST_NOTIFICATIONS` run-time permission. */
    fun hasNotificationPermission(context: Context): Boolean {
        val permissionGranted = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        return hasNotificationPermission(Build.VERSION.SDK_INT, permissionGranted)
    }

    internal fun hasNotificationPermission(apiLevel: Int, permissionGranted: Boolean): Boolean =
        apiLevel < Build.VERSION_CODES.TIRAMISU || permissionGranted

    fun createOrGetChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val manager = context.getSystemService(NotificationManager::class.java)
        val existing = manager.getNotificationChannel(CHANNEL_ID)
        if (existing != null) return

        val channel = NotificationChannel(
            CHANNEL_ID,
            "AgroRoute notifications",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Order, bid, order-request, and harvest-status updates."
            setShowBadge(true)
        }
        manager.createNotificationChannel(channel)
    }

    /** Sends a local suggestion so the user can open the order while the app is closed. */
    fun sendOrderSuggestion(
        context: Context,
        orderId: String,
        receiver: Class<out android.content.BroadcastReceiver>,
        pendingIntentFlags: Int = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    ) {
        if (!hasNotificationPermission(context)) return

        val manager = context.getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        if (manager.getNotificationChannel(ORDER_SUGGESTION_CHANNEL_ID) == null) {
            val channel = NotificationChannel(
                ORDER_SUGGESTION_CHANNEL_ID,
                "Order suggestions",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Suggests actions the user can take from an order notification."
                setShowBadge(true)
            }
            manager.createNotificationChannel(channel)
        }

        val intent = Intent(context, receiver).apply {
            action = com.agroroute.core.notification.NotificationAction.ACTION_OPEN_ORDER
            putExtra(com.agroroute.core.notification.NotificationAction.EXTRA_ORDER_ID, orderId)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            orderId.hashCode(),
            intent,
            pendingIntentFlags
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("New order")
            .setContentText("A buyer wants to place an order. Review it in the app.")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .build()

        manager.notify(orderId.hashCode(), notification)
    }
}
