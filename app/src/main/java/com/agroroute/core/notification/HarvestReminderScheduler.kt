package com.agroroute.core.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.agroroute.core.model.HarvestListing
import com.agroroute.core.model.HarvestStatus
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import javax.inject.Inject
import javax.inject.Singleton

/** Pure helper for the harvest-reminder wall-clock time — fully testable without Android framework. */
internal object HarvestReminderTime {

    fun reminderEpochMillis(harvestAtMillis: Long): Long? {
        val harvestDay = Instant.ofEpochMilli(harvestAtMillis)
            .atZone(ZoneOffset.UTC)
            .toLocalDate()
        val reminderAt = harvestDay.atTime(LocalTime.of(9, 0))
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
        return if (reminderAt <= System.currentTimeMillis()) null else reminderAt
    }
}

@Singleton
class HarvestReminderScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val store: HarvestReminderStore
) {
    fun schedule(listing: HarvestListing) {
        val harvestAt = listing.expectedHarvestAt
        if (listing.isDraft || listing.harvestStatus != HarvestStatus.EXPECTED ||
            harvestAt == null || !NotificationHelper.areNotificationsEnabled(context)
        ) {
            cancel(listing.listingId)
            return
        }

        val reminderAt = HarvestReminderTime.reminderEpochMillis(harvestAt) ?: run {
            cancel(listing.listingId)
            return
        }

        val intent = Intent(context, HarvestReminderReceiver::class.java).apply {
            action = ACTION_HARVEST_REMINDER
            putExtra(EXTRA_LISTING_ID, listing.listingId)
            putExtra(EXTRA_CROP_NAME, listing.cropName)
            putExtra(EXTRA_FARMER_ID, listing.farmerId)
            putExtra(
                NotificationAction.EXTRA_EVENT_ID,
                "${NotificationService.LOCAL_HARVEST_PREFIX}${listing.listingId}:$harvestAt"
            )
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            listing.listingId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        context.getSystemService(AlarmManager::class.java)
            .setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, reminderAt, pendingIntent)

        store.upsert(listing.listingId, listing.cropName, harvestAt, listing.farmerId)
    }

    fun cancel(listingId: String) {
        val intent = Intent(context, HarvestReminderReceiver::class.java).apply {
            action = ACTION_HARVEST_REMINDER
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            listingId.hashCode(),
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            context.getSystemService(AlarmManager::class.java).cancel(pendingIntent)
            pendingIntent.cancel()
        }
        store.remove(listingId)
    }

    fun rescheduleAllStored() {
        if (!NotificationHelper.areNotificationsEnabled(context)) return
        val currentUserId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: return
        store.all().forEach { reminder ->
            // Older ownerless reminders are recreated by listing sync. Never assign them to a different account.
            if (reminder.farmerId != currentUserId) return@forEach
            val reminderAt = HarvestReminderTime.reminderEpochMillis(reminder.expectedHarvestAt)
            if (reminderAt == null) {
                store.remove(reminder.listingId)
            } else {
                val intent = Intent(context, HarvestReminderReceiver::class.java).apply {
                    action = ACTION_HARVEST_REMINDER
                    putExtra(EXTRA_LISTING_ID, reminder.listingId)
                    putExtra(EXTRA_CROP_NAME, reminder.cropName)
                    putExtra(EXTRA_FARMER_ID, currentUserId)
                    putExtra(
                        NotificationAction.EXTRA_EVENT_ID,
                        "${NotificationService.LOCAL_HARVEST_PREFIX}${reminder.listingId}:${reminder.expectedHarvestAt}"
                    )
                }
                val pendingIntent = PendingIntent.getBroadcast(
                    context,
                    reminder.listingId.hashCode(),
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                context.getSystemService(AlarmManager::class.java)
                    .setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, reminderAt, pendingIntent)
            }
        }
    }

    companion object {
        const val ACTION_HARVEST_REMINDER = "com.agroroute.action.HARVEST_REMINDER"
        const val EXTRA_LISTING_ID = "listing_id"
        const val EXTRA_CROP_NAME = "crop_name"
        const val EXTRA_FARMER_ID = "farmer_id"
    }
}
