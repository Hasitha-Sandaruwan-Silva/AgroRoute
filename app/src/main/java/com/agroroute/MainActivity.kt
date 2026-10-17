package com.agroroute

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.agroroute.core.runtime.appViewModel as hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.agroroute.core.components.BrandedStartup
import com.agroroute.features.auth.AuthViewModel
import com.agroroute.core.notification.NotificationAction
import com.agroroute.core.notification.NotificationService
import com.agroroute.core.theme.AgroRouteTheme
import com.agroroute.core.theme.ThemeMode
import com.agroroute.core.theme.ThemeViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

/**
 * Single-activity entry point that hosts the Compose NavHost for the entire application.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private var notificationRoute by mutableStateOf<String?>(null)
    private var notificationEventId by mutableStateOf<String?>(null)
    private var nativeSplashFinished by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        // 1. Install System Native Splash Screen
        val splashScreen = installSplashScreen()

        super.onCreate(savedInstanceState)
        notificationRoute = intent.getStringExtra(NotificationAction.EXTRA_ROUTE)
        notificationEventId = intent.getStringExtra(NotificationAction.EXTRA_EVENT_ID)

        enableEdgeToEdge()

        // Continue the native artwork in Compose without holding the system splash.
        splashScreen.setOnExitAnimationListener { splashScreenView ->
            nativeSplashFinished = true
            splashScreenView.remove()
        }

        setContent {
            val themeViewModel: ThemeViewModel = hiltViewModel()
            val mode by themeViewModel.mode.collectAsStateWithLifecycle()
            val systemDark = isSystemInDarkTheme()
            val darkTheme = when (mode) {
                ThemeMode.SYSTEM -> systemDark
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }

            AgroRouteTheme(darkTheme = darkTheme) {
                val authViewModel: AuthViewModel = hiltViewModel()
                val authState by authViewModel.uiState.collectAsStateWithLifecycle()
                BrandedStartup(authState.sessionResolved, nativeSplashFinished, darkTheme, systemDark) {
                    com.agroroute.core.runtime.AppExperience(
                        notificationRoute = notificationRoute,
                        notificationEventId = notificationEventId,
                        onNotificationRouteHandled = { eventId ->
                            notificationRoute = null
                            notificationEventId = null
                            if (eventId != null) {
                                lifecycleScope.launch {
                                    runCatching { NotificationService(this@MainActivity).markInboxItemRead(eventId) }
                                }
                            }
                        }
                    )
                }
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == com.agroroute.core.notification.NotificationPermission.REQUEST_CODE) {
            com.agroroute.core.notification.NotificationDeliveryRefresh.refresh()
        }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        notificationRoute = intent.getStringExtra(NotificationAction.EXTRA_ROUTE)
        notificationEventId = intent.getStringExtra(NotificationAction.EXTRA_EVENT_ID)
    }
}
