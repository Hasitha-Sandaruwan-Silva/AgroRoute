package com.agroroute

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.os.Bundle
import android.view.View
import android.view.animation.AnticipateInterpolator
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.animation.doOnEnd
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import com.agroroute.core.runtime.appViewModel as hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.agroroute.core.navigation.NavGraph
import com.agroroute.core.notification.NotificationAction
import com.agroroute.core.notification.NotificationService
import com.agroroute.core.theme.AgroRouteTheme
import com.agroroute.core.theme.ThemeMode
import com.agroroute.core.theme.ThemeViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Single-activity entry point that hosts the Compose NavHost for the entire application.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private var notificationRoute by mutableStateOf<String?>(null)
    private var notificationEventId by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        // 1. Install System Native Splash Screen
        val splashScreen = installSplashScreen()

        super.onCreate(savedInstanceState)
        notificationRoute = intent.getStringExtra(NotificationAction.EXTRA_ROUTE)
        notificationEventId = intent.getStringExtra(NotificationAction.EXTRA_EVENT_ID)

        // 2. Keep Splash Screen visible for 2.5 seconds
        var keepSplashScreen = true
        splashScreen.setKeepOnScreenCondition { keepSplashScreen }

        lifecycleScope.launch {
            delay(2500L) // 2.5 seconds loading delay
            keepSplashScreen = false
        }

        enableEdgeToEdge()
        WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightStatusBars = true

        // 3. Smooth exit animation when splash screen finishes
        splashScreen.setOnExitAnimationListener { splashScreenView ->
            // Scale/Zoom animation for the center icon
            val scaleX = ObjectAnimator.ofFloat(splashScreenView.iconView, View.SCALE_X, 1f, 1.25f)
            val scaleY = ObjectAnimator.ofFloat(splashScreenView.iconView, View.SCALE_Y, 1f, 1.25f)

            // Fade-out animation for the entire splash screen
            val alpha = ObjectAnimator.ofFloat(splashScreenView.view, View.ALPHA, 1f, 0f)

            AnimatorSet().apply {
                playTogether(scaleX, scaleY, alpha)
                duration = 500L // 0.5s exit animation duration
                interpolator = AnticipateInterpolator()

                // Remove the splash view when animation completes
                doOnEnd { splashScreenView.remove() }

                start()
            }
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

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        notificationRoute = intent.getStringExtra(NotificationAction.EXTRA_ROUTE)
        notificationEventId = intent.getStringExtra(NotificationAction.EXTRA_EVENT_ID)
    }
}
