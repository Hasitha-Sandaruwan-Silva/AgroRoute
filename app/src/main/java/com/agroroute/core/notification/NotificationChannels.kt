package com.agroroute.core.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build

/**
 * Creates and holds Android notification channel IDs used across AgroRoute.
 */
object NotificationChannels {
    const val HARVEST_REMINDERS = "harvest_reminders"
    const val BIDS = "bids"
    const val ORDERS = "orders"
    const val ORDER_REQUESTS = "order_requests"
    const val LISTINGS = "listings"
    const val GENERAL = "general"

    fun ensureCreated(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        val channels = listOf(
            NotificationChannel(
                HARVEST_REMINDERS,
                "Harvest reminders",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply { description = "Reminders on expected harvest dates" },
            NotificationChannel(
                BIDS,
                "Bids",
                NotificationManager.IMPORTANCE_HIGH
            ).apply { description = "New bids and bid status updates" },
            NotificationChannel(
                ORDERS,
                "Orders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply { description = "Order creation and status updates" },
            NotificationChannel(
                ORDER_REQUESTS,
                "Order requests",
                NotificationManager.IMPORTANCE_HIGH
            ).apply { description = "Direct order request updates" },
            NotificationChannel(
                LISTINGS,
                "Listings",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply { description = "Listing-related alerts" },
            NotificationChannel(
                GENERAL,
                "General",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply { description = "Other AgroRoute notifications" }
        )
        manager.createNotificationChannels(channels)
    }
}
