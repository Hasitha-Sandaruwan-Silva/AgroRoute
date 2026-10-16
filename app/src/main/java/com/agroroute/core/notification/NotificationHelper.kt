package com.agroroute.core.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.core.app.NotificationManagerCompat

/** Immutable, idempotent helpers for creating and issuing notifications. */
object NotificationHelper {

    const val CHANNEL_ID = "agroroute_notifications"
    const val MESSAGES_CHANNEL_ID = "agroroute_messages"

    /** Whether the app has the `POST_NOTIFICATIONS` run-time permission. */
    fun hasNotificationPermission(context: Context): Boolean {
        val permissionGranted = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        return hasNotificationPermission(Build.VERSION.SDK_INT, permissionGranted)
    }

    internal fun hasNotificationPermission(apiLevel: Int, permissionGranted: Boolean): Boolean =
        apiLevel < Build.VERSION_CODES.TIRAMISU || permissionGranted

    internal fun isNotificationDeliveryEnabled(
        apiLevel: Int,
        permissionGranted: Boolean,
        appNotificationsEnabled: Boolean,
        channelEnabled: Boolean
    ): Boolean =
        hasNotificationPermission(apiLevel, permissionGranted) && appNotificationsEnabled && channelEnabled

    fun areNotificationsEnabled(context: Context, channelId: String = CHANNEL_ID): Boolean {
        val channelEnabled = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            true
        } else {
            val channel = context.getSystemService(NotificationManager::class.java)
            ?.getNotificationChannel(channelId)
            channel?.importance != NotificationManager.IMPORTANCE_NONE
        }
        val runtimePermissionGranted = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        return isNotificationDeliveryEnabled(
            apiLevel = Build.VERSION.SDK_INT,
            permissionGranted = runtimePermissionGranted,
            appNotificationsEnabled = NotificationManagerCompat.from(context).areNotificationsEnabled(),
            channelEnabled = channelEnabled
        )
    }

    fun createOrGetChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val manager = context.getSystemService(NotificationManager::class.java)
        if (manager.getNotificationChannel(MESSAGES_CHANNEL_ID) == null) {
            manager.createNotificationChannel(NotificationChannel(MESSAGES_CHANNEL_ID, "Messages",
                NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Chat and order communication notifications"
                setShowBadge(true)
            })
        }
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

}
