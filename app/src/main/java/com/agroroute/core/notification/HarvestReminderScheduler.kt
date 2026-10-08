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
    @ApplicationContext private val context: Context
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
        ) ?: return
        context.getSystemService(AlarmManager::class.java).cancel(pendingIntent)
        pendingIntent.cancel()
    }

    companion object {
        const val ACTION_HARVEST_REMINDER = "com.agroroute.action.HARVEST_REMINDER"
        const val EXTRA_LISTING_ID = "listing_id"
        const val EXTRA_CROP_NAME = "crop_name"
        const val EXTRA_FARMER_ID = "farmer_id"
    }
}
