package com.agroroute.core.util

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow

/** Local presentation refresh only. Firestore Rules remain authoritative at commit. */
fun presentationClock() = flow {
    while (true) {
        emit(System.currentTimeMillis())
        delay(1_000)
    }
}
