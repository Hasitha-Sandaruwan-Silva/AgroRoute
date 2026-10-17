package com.agroroute.core.notification

import android.util.Log
import com.agroroute.core.model.UserProfile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/** One account-scoped inbox listener delivers persisted events while the process is alive. */
@Singleton
class RealtimeEventNotifier @Inject constructor(
    private val notificationService: NotificationService
) {
    private var sessionJob: Job? = null

    fun start(scope: CoroutineScope, profile: UserProfile) {
        stop()
        sessionJob = scope.launch {
            try { notificationService.watchInbox(profile) }
            catch (e: kotlinx.coroutines.CancellationException) { throw e }
            catch (e: Exception) { Log.w("RealtimeEventNotifier", "Inbox watch failed", e) }
        }
    }

    fun stop() {
        sessionJob?.cancel()
        sessionJob = null
    }
}
