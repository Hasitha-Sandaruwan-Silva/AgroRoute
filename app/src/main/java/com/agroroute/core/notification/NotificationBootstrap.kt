package com.agroroute.core.notification

import android.content.Context
import android.util.Log
import com.agroroute.core.model.UserProfile
import com.agroroute.core.repository.AuthRepository
import com.agroroute.core.util.Result
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Starts notification channels, FCM topic sync, harvest reminder restore, and realtime watches.
 */
@Singleton
class NotificationBootstrap @Inject constructor(
    @ApplicationContext private val context: Context,
    private val authRepository: AuthRepository,
    private val fcmTopicManager: FcmTopicManager,
    private val realtimeEventNotifier: RealtimeEventNotifier,
    private val harvestReminderScheduler: HarvestReminderScheduler
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var started = false

    fun start() {
        if (started) return
        started = true
        NotificationChannels.ensureCreated(context)
        harvestReminderScheduler.rescheduleAllStored()

        scope.launch {
            authRepository.getCurrentUserProfile().collectLatest { result ->
                val profile = (result as? Result.Success)?.data
                if (profile == null) {
                    realtimeEventNotifier.stop()
                    fcmTopicManager.clear()
                } else {
                    onSignedIn(profile)
                }
            }
        }
    }

    private suspend fun onSignedIn(profile: UserProfile) {
        fcmTopicManager.syncForUser(profile.userId, profile.role)
        val token = fcmTopicManager.currentTokenOrNull()
        if (token != null) {
            Log.d(TAG, "FCM token ready (length=${token.length}); topics synced for ${profile.role}")
        } else {
            Log.w(TAG, "FCM token unavailable — push receive not verified on this device")
        }
        realtimeEventNotifier.start(scope, profile)
    }

    companion object {
        private const val TAG = "NotificationBootstrap"
    }
}
