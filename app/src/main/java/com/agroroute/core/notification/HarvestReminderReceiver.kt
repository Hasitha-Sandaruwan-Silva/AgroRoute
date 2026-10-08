package com.agroroute.core.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.content.ContextCompat
import com.agroroute.core.R

class HarvestReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) != android.content.pm.PackageManager.PERMISSION_GRANTED
        ) return

        val appContext = context.applicationContext ?: context
        val cropName = intent.getStringExtra(NotificationAction.EXTRA_CROP_NAME).orEmpty()
        val listingId = intent.getStringExtra(NotificationAction.EXTRA_LISTING_ID).orEmpty()

        NotificationHelper.createOrGetChannel(appContext.getSystemService(NotificationManager::class.java))

        val payload = NotificationEvent.Payload(
            cropName = cropName,
            listingId = listingId
        )
        val notification = android.app.Notification.Builder(appContext, NotificationHelper.CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle(appContext.resources.getString(R.string.harvest_reminder_title))
            .setContentText(NotificationEvent.HARVEST_REMINDER.content(appContext, payload))
            .setAutoCancel(true)
            .build()
        appContext.getSystemService(NotificationManager::class.java).notify(listingId.hashCode(), notification)
    }
}



