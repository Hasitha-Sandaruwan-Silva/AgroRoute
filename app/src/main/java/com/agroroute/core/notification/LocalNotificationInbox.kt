package com.agroroute.core.notification

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

internal object LocalNotificationInbox {
    private const val KEY_ITEMS = "items"
    private val gson = Gson()
    private val itemListType = object : TypeToken<List<NotificationInboxItem>>() {}.type

    fun items(context: Context, userId: String): List<NotificationInboxItem> = runCatching {
        val json = preferences(context, userId).getString(KEY_ITEMS, null) ?: return emptyList()
        gson.fromJson<List<NotificationInboxItem>>(json, itemListType).orEmpty()
    }.getOrDefault(emptyList())

    fun upsert(context: Context, userId: String, item: NotificationInboxItem) {
        val updated = (items(context, userId).filterNot { it.id == item.id } + item)
            .sortedByDescending { it.createdAt }
            .take(100)
        preferences(context, userId).edit().putString(KEY_ITEMS, gson.toJson(updated)).apply()
    }

    fun markRead(context: Context, userId: String, itemId: String) {
        val updated = items(context, userId).map { item ->
            if (item.id == itemId && !item.isRead) item.copy(readAt = System.currentTimeMillis()) else item
        }
        preferences(context, userId).edit().putString(KEY_ITEMS, gson.toJson(updated)).apply()
    }

    private fun preferences(context: Context, userId: String) =
        context.getSharedPreferences("notification_inbox_$userId", Context.MODE_PRIVATE)
}
