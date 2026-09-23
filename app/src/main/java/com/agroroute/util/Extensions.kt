package com.agroroute.util

import com.google.firebase.Timestamp
import java.text.NumberFormat
import java.util.Currency
import java.util.Date
import java.util.Locale

/**
 * Kotlin extension functions used across the app for formatting, conversion, and Flow utilities.
 */

// ── Date / Timestamp ──────────────────────────────────────────────────────────

/** Converts a [Timestamp] to a [Date]. */
fun Timestamp.toDate(): Date = this.toDate()

/** Formats a [Date] to a human-readable "dd MMM yyyy" string. */
fun Date.toDisplayString(): String {
    val fmt = java.text.SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    return fmt.format(this)
}

// ── Currency / Pricing ────────────────────────────────────────────────────────

/** Formats a [Double] as a localised currency string (e.g. "Rs. 1,200.00"). */
fun Double.toCurrencyString(currencyCode: String = "LKR"): String = try {
    val format = NumberFormat.getCurrencyInstance(Locale.getDefault())
    format.currency = Currency.getInstance(currencyCode)
    format.format(this)
} catch (e: Exception) {
    "Rs. %.2f".format(this)
}

// ── String helpers ────────────────────────────────────────────────────────────

/** Returns the string capitalised with the first letter uppercase. */
fun String.capitaliseFirst(): String =
    replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }

/** Trims a string and returns null if it is blank after trimming. */
fun String.trimOrNull(): String? = trim().ifBlank { null }

// ── Result helpers ────────────────────────────────────────────────────────────

/** Maps a [Result.Success] value through [transform]; passes [Result.Error]/[Result.Loading] through unchanged. */
inline fun <T, R> Result<T>.mapSuccess(transform: (T) -> R): Result<R> = when (this) {
    is Result.Success -> Result.Success(transform(data))
    is Result.Error   -> this
    is Result.Loading -> Result.Loading
}
