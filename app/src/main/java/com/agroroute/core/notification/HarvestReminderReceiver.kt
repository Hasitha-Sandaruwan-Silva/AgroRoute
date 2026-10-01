package com.agroroute.core.notification

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

class HarvestReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) return

        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                "Harvest reminders",
                NotificationManager.IMPORTANCE_DEFAULT
            )
        )
        val cropName = intent.getStringExtra(HarvestReminderScheduler.EXTRA_CROP_NAME)
            .orEmpty()
        val listingId = intent.getStringExtra(HarvestReminderScheduler.EXTRA_LISTING_ID)
            .orEmpty()
        val notification = Notification.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle("Harvest reminder")
            .setContentText(if (cropName.isBlank()) "Your harvest is expected today." else "$cropName is expected today.")
            .setAutoCancel(true)
            .build()
        manager.notify(listingId.hashCode(), notification)
    }

    companion object {
        private const val CHANNEL_ID = "harvest_reminders"
    }
}
