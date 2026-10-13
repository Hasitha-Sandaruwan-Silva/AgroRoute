package com.agroroute.core.runtime
import androidx.compose.runtime.Composable
import com.agroroute.core.navigation.NavGraph
@Composable fun AppExperience(notificationRoute: String? = null, notificationEventId: String? = null,
    onNotificationRouteHandled: (String?) -> Unit = {}) {
    NavGraph(notificationRoute = notificationRoute, notificationEventId = notificationEventId,
        onNotificationRouteHandled = onNotificationRouteHandled)
}
@Composable fun AppEntryAction(tint: androidx.compose.ui.graphics.Color = androidx.compose.ui.graphics.Color.White) = Unit
