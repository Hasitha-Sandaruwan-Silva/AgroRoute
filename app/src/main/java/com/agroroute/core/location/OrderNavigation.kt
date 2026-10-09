package com.agroroute.core.location

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import java.util.Locale

internal fun navigationCoordinate(location: SelectedLocation): String =
    String.format(Locale.US, "%.6f,%.6f", location.latitude, location.longitude)

/** The caller passes an order snapshot; no profile lookup or API key is involved. */
fun openOrderDirections(context: Context, destination: SelectedLocation): Boolean {
    val coordinate = navigationCoordinate(destination)
    val intents = listOf(
        Intent(Intent.ACTION_VIEW, Uri.parse("google.navigation:q=$coordinate&mode=d"))
            .setPackage("com.google.android.apps.maps"),
        Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/maps/dir/?api=1&destination=$coordinate&travelmode=driving")),
        Intent(Intent.ACTION_VIEW, Uri.parse("geo:$coordinate?q=$coordinate"))
    )
    for (intent in intents) {
        try {
            context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            return true
        } catch (_: ActivityNotFoundException) {
            // Try the next handler without requiring Android package visibility queries.
        } catch (_: SecurityException) {
            // A disabled/restricted external app should not crash order details.
        }
    }
    return false
}
