package com.agroroute.core.util

import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

object HarvestAvailabilityDate {
    fun format(timestamp: Long): String =
        Instant.ofEpochMilli(timestamp)
            .atZone(ZoneOffset.UTC)
            .format(DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.getDefault()))

    fun label(timestamp: Long): String = "Available from ${format(timestamp)}"
}