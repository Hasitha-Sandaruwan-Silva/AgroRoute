package com.agroroute.core.location

import java.util.Locale

fun usableDeviceCoordinates(latitude: Double, longitude: Double): Boolean =
    latitude.isFinite() && latitude in -90.0..90.0 &&
        longitude.isFinite() && longitude in -180.0..180.0 &&
        !(latitude == 0.0 && longitude == 0.0)

/** Keeps existing Firestore/Room text fields compatible, including older address-only values. */
data class SelectedLocation(val address: String, val latitude: Double, val longitude: Double) {
    init {
        require(latitude.isFinite() && latitude in -90.0..90.0)
        require(longitude.isFinite() && longitude in -180.0..180.0)
    }

    fun coordinates(): String = String.format(Locale.US, "%.6f, %.6f", latitude, longitude)
    fun toStoredText(): String = "${address.trim()}\n[${coordinates()}]".trim()

    companion object {
        private val suffix = Regex("\\[(-?\\d+(?:\\.\\d+)?),\\s*(-?\\d+(?:\\.\\d+)?)\\]$")
        fun fromStoredText(value: String): SelectedLocation? {
            val match = suffix.find(value.trim()) ?: return null
            return runCatching {
                SelectedLocation(
                    value.trim().substring(0, match.range.first).trim(),
                    match.groupValues[1].toDouble(), match.groupValues[2].toDouble()
                )
            }.getOrNull()
        }
    }
}
