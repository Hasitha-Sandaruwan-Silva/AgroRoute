package com.agroroute.core.components

import androidx.compose.runtime.*
import com.agroroute.core.util.presentationClock

@Composable
fun listingPresentationTime(): Long {
    val clock = remember { presentationClock() }
    val now by clock.collectAsState(initial = System.currentTimeMillis())
    return now
}
