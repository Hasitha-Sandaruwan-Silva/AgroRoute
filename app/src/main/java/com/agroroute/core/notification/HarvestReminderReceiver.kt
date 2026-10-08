package com.agroroute.core.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.app.PendingIntent
import androidx.core.app.NotificationCompat
import android.os.Build
import androidx.core.content.ContextCompat
import com.agroroute.R
import com.agroroute.MainActivity
import com.agroroute.core.navigation.Screen

class HarvestReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val farmerId = intent.getStringExtra(HarvestReminderScheduler.EXTRA_FARMER_ID).orEmpty()
        if (farmerId.isBlank() ||
            com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid != farmerId
        ) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) != android.content.pm.PackageManager.PERMISSION_GRANTED
        ) return

        val appContext = context.applicationContext ?: context
        val cropName = intent.getStringExtra(NotificationAction.EXTRA_CROP_NAME).orEmpty()
        val listingId = intent.getStringExtra(NotificationAction.EXTRA_LISTING_ID).orEmpty()

        NotificationHelper.createOrGetChannel(appContext)

        val payload = NotificationEvent.Payload(
            cropName = cropName,
            listingId = listingId
        )
        val launchIntent = Intent(appContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(NotificationAction.EXTRA_ROUTE, Screen.EditListing.createRoute(listingId))
            putExtra(NotificationAction.EXTRA_LISTING_ID, listingId)
        }
        val pendingIntent = PendingIntent.getActivity(
            appContext,
            listingId.hashCode(),
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(appContext, NotificationHelper.CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle(appContext.resources.getString(R.string.harvest_reminder_title))
            .setContentText(NotificationEvent.HARVEST_REMINDER.content(appContext, payload))
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()
        appContext.getSystemService(android.app.NotificationManager::class.java)
            .notify(listingId.hashCode(), notification)
    }
}



