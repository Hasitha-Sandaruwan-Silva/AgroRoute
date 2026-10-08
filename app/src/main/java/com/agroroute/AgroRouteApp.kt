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
		FirebaseAuth.getInstance().addAuthStateListener { auth ->
			val userId = auth.currentUser?.uid ?: return@addAuthStateListener
			FirebaseMessaging.getInstance().token.addOnSuccessListener { token ->
				notificationScope.launch {
					NotificationService(this@AgroRouteApp).registerDeviceToken(userId, token)
				}
			}
		}
	}
}
