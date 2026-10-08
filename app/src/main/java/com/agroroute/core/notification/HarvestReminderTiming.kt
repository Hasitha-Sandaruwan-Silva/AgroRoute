package com.agroroute.core.notification

import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset

/**
 * Pure timing helpers for harvest-day local reminders (09:00 device local time).
 */
object HarvestReminderTiming {

    /**
     * Returns the epoch millis for a 09:00 local reminder on the UTC harvest calendar day,
     * or null when [expectedHarvestAt] is null or the reminder time is already past.
     */
    fun reminderAtMillis(
        expectedHarvestAt: Long?,
        nowMillis: Long = System.currentTimeMillis(),
        zoneId: ZoneId = ZoneId.systemDefault()
    ): Long? {
        val harvestAt = expectedHarvestAt ?: return null
        val harvestDay = Instant.ofEpochMilli(harvestAt)
            .atZone(ZoneOffset.UTC)
            .toLocalDate()
        val reminderAt = harvestDay.atTime(LocalTime.of(9, 0))
            .atZone(zoneId)
            .toInstant()
            .toEpochMilli()
        return reminderAt.takeIf { it > nowMillis }
    }
}
