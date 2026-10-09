package com.agroroute.features.settings

import com.agroroute.core.model.UserRole
import com.agroroute.core.notification.NotificationInboxItem
import com.agroroute.core.notification.NotificationRoute
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

internal enum class NotificationDay(val label: String) {
    TODAY("Today"), YESTERDAY("Yesterday"), EARLIER("Earlier")
}

internal fun notificationDay(
    timestamp: Long,
    today: LocalDate = LocalDate.now(),
    zone: ZoneId = ZoneId.systemDefault()
): NotificationDay {
    val day = Instant.ofEpochMilli(timestamp).atZone(zone).toLocalDate()
    return when {
        day >= today -> NotificationDay.TODAY
        day == today.minusDays(1) -> NotificationDay.YESTERDAY
        else -> NotificationDay.EARLIER
    }
}

internal fun notificationBadge(count: Int): String? = when {
    count <= 0 -> null
    count > 9 -> "9+"
    else -> count.toString()
}

/** Use the supplied route only. Never synthesize destinations from partial payloads. */
internal fun notificationDestination(item: NotificationInboxItem, role: UserRole?): String? {
    if (role == null || !NotificationRoute.isAllowed(item.route, role)) return null
    val parts = item.route.split('/')
    if (parts.size > 2) return null
    if (parts.any { part ->
            part.isBlank() || part == "." || part == ".." ||
                part.any { it.isWhitespace() || it.isISOControl() || it in "{}?#\\" }
        }) return null
    return item.route
}
