package com.agroroute.core.notification

import android.content.Context
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Persists scheduled harvest reminders so they can be restored after device reboot.
 */
@Singleton
class HarvestReminderStore @Inject constructor(
    @ApplicationContext context: Context
) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun upsert(listingId: String, cropName: String, expectedHarvestAt: Long, farmerId: String) {
        prefs.edit {
            putString(key(listingId), "$expectedHarvestAt|$cropName")
            putString("owner_$listingId", farmerId)
        }
    }

    fun remove(listingId: String) {
        prefs.edit { remove(key(listingId)); remove("owner_$listingId") }
    }

    fun all(): List<StoredHarvestReminder> =
        prefs.all.mapNotNull { (prefKey, value) ->
            if (!prefKey.startsWith(KEY_PREFIX)) return@mapNotNull null
            val listingId = prefKey.removePrefix(KEY_PREFIX)
            val raw = value as? String ?: return@mapNotNull null
            val sep = raw.indexOf('|')
            if (sep <= 0) return@mapNotNull null
            val harvestAt = raw.substring(0, sep).toLongOrNull() ?: return@mapNotNull null
            val cropName = raw.substring(sep + 1)
            StoredHarvestReminder(listingId, cropName, harvestAt, prefs.getString("owner_$listingId", null))
        }

    private fun key(listingId: String) = KEY_PREFIX + listingId

    companion object {
        private const val PREFS_NAME = "agroroute_harvest_reminders"
        private const val KEY_PREFIX = "reminder_"
    }
}

data class StoredHarvestReminder(
    val listingId: String,
    val cropName: String,
    val expectedHarvestAt: Long,
    val farmerId: String? = null
)
