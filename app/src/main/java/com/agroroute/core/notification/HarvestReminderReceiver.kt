package com.agroroute.core.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class HarvestReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val farmerId = intent.getStringExtra(HarvestReminderScheduler.EXTRA_FARMER_ID).orEmpty()
        if (farmerId.isBlank() ||
            com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid != farmerId
        ) return
        if (!NotificationHelper.areNotificationsEnabled(context)) return

        val appContext = context.applicationContext ?: context
        val cropName = intent.getStringExtra(NotificationAction.EXTRA_CROP_NAME).orEmpty()
        val listingId = intent.getStringExtra(NotificationAction.EXTRA_LISTING_ID).orEmpty()
        if (listingId.isBlank()) return

        val payload = NotificationEvent.Payload(
            cropName = cropName,
            listingId = listingId
        )
        val eventId = intent.getStringExtra(NotificationAction.EXTRA_EVENT_ID)
            ?: "${NotificationService.LOCAL_HARVEST_PREFIX}${listingId}:legacy"
        NotificationService(appContext).postHarvestReminder(farmerId, eventId, payload)
    }
}



