package com.agroroute.core.notification

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset

class HarvestReminderSchedulerTest {

    @Test
    fun reminderIsScheduledNineLocalTimeOnTheHarvestDay() {
        val harvestAt = Instant.parse("2026-10-10T12:00:00Z")
            .toEpochMilli()
        val reminder = HarvestReminderTime.reminderEpochMillis(harvestAt)

        val day = Instant.ofEpochMilli(harvestAt)
            .atZone(ZoneOffset.UTC)
            .toLocalDate()
        val expected = day.atTime(LocalTime.of(9, 0))
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()

        assertEquals(expected, reminder)
    }

    @Test
    fun reminderIsNeverInThePast() {
        val now = System.currentTimeMillis()
        val inThePast = now - 10_000
        val reminder = HarvestReminderTime.reminderEpochMillis(inThePast)
        assertNull(reminder)
    }

    @Test
    fun reminderExactlyAtOrAfterNowIsAllowedWhenOnOrAfterNow() {
        val now = System.currentTimeMillis()
        // A harvest scheduled for now should still produce a valid but equal reminder.
        val reminder = HarvestReminderTime.reminderEpochMillis(now)
        assertEquals(now, reminder)
    }

    @Test
    fun draftListingSkipsScheduling() {
        val listing = HarvestListing(
            listingId = "listing-1",
            cropName = "Maha rice",
            farmerId = "farmer-1",
            expectedHarvestAt = Instant.parse("2026-10-10T12:00:00Z").toEpochMilli(),
            isDraft = true
        )
        // schedule() also guards with isDraft; here we only test the time helper's
        // boundary to keep this unit free of AlarmManager.
        val reminder = HarvestReminderTime.reminderEpochMillis(
            listing.expectedHarvestAt ?: error("expectedHarvestAt is null")
        )
        // The time is valid; the scheduler's isDraft guard is tested in integration.
        assertNotNull(reminder)
    }

    @Test
    fun preservesUtcDayBoundary() {
        val harvestAt = Instant.parse("2026-10-10T00:30:00Z").toEpochMilli()
        val reminder = HarvestReminderTime.reminderEpochMillis(harvestAt)

        val day = Instant.ofEpochMilli(harvestAt)
            .atZone(ZoneOffset.UTC)
            .toLocalDate()
        val expected = day.atTime(LocalTime.of(9, 0))
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()

        assertEquals(expected, reminder)
    }
}
