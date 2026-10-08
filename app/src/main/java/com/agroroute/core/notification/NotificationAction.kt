package com.agroroute.core.notification

/** Intent actions and extras used by the notification receivers. */
object NotificationAction {
    const val ACTION_OPEN_ORDER = "com.agroroute.action.OPEN_ORDER"
    const val EXTRA_ORDER_ID = "order_id"
    const val EXTRA_CROP_NAME = "crop_name"
    const val EXTRA_LISTING_ID = "listing_id"
}
