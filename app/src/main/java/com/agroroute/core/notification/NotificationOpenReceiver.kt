package com.agroroute.core.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.agroroute.MainActivity

/**
 * Opens the app when a user taps a notification while the app is **closed**.
 *
 * Stores the delivery route as an [Intent] extra and launches the app's launcher
 * activity. The app's navigation graph reads [NotificationAction.EXTRA_ROUTE],
 * [EXTRA_ORDER_ID], [EXTRA_BID_ID], and [EXTRA_LISTING_ID] to place the user at the
 * right screen.
 */
class NotificationOpenReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val route = intent.getStringExtra(NotificationAction.EXTRA_ROUTE) ?: return
        val action = intent.action ?: return

        val launch = Intent(context, MainActivity::class.java).apply {
                this.action = action
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            putExtra(NotificationAction.EXTRA_ROUTE, route)
            putExtra(NotificationAction.EXTRA_ORDER_ID, intent.getStringExtra(NotificationAction.EXTRA_ORDER_ID))
            putExtra(NotificationAction.EXTRA_BID_ID, intent.getStringExtra(NotificationAction.EXTRA_BID_ID))
            putExtra(NotificationAction.EXTRA_LISTING_ID, intent.getStringExtra(NotificationAction.EXTRA_LISTING_ID))
        }

        try {
            context.startActivity(launch)
        } catch (e: java.lang.RuntimeException) {
            // Some OEM/security policies block startActivity from a BroadcastReceiver;
            // the user can still tap the notification to open the app.
        }
    }
}
