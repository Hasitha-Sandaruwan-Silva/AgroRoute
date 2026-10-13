package com.agroroute

import android.app.Application
import com.agroroute.core.notification.NotificationBootstrap
import com.agroroute.core.notification.NotificationService
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.messaging.FirebaseMessaging
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.collect
import javax.inject.Inject

/**
 * Application class that bootstraps Hilt dependency injection for the entire app.
 */
@HiltAndroidApp
class AgroRouteApp : Application() {
	@Inject lateinit var notificationBootstrap: NotificationBootstrap
	private val notificationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

	override fun onCreate() {
		super.onCreate()
		notificationBootstrap.start()
        val auth = FirebaseAuth.getInstance()
        auth.addAuthStateListener { registerCurrentDeviceToken(it) }
        notificationScope.launch {
            com.agroroute.core.runtime.BackgroundNetworkAccess.allowed
                .drop(1).filter { it }.collect { registerCurrentDeviceToken(auth) }
        }
    }

    private fun registerCurrentDeviceToken(auth: FirebaseAuth) {
        if (!com.agroroute.core.runtime.BackgroundNetworkAccess.allowed.value) return
        val userId = auth.currentUser?.uid ?: return
        FirebaseMessaging.getInstance().token.addOnSuccessListener { token ->
            notificationScope.launch {
                if (!com.agroroute.core.runtime.BackgroundNetworkAccess.allowed.value || auth.currentUser?.uid != userId) return@launch
                NotificationService(this@AgroRouteApp).registerDeviceToken(userId, token)
            }
        }
    }
}
