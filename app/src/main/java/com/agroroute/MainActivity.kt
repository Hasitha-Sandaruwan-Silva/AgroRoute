package com.agroroute

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.os.Bundle
import android.view.View
import android.view.animation.AnticipateInterpolator
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.animation.doOnEnd
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import com.agroroute.core.navigation.NavGraph
import com.agroroute.core.theme.AgroRouteTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Single-activity entry point that hosts the Compose NavHost for the entire application.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        // 1. Install System Native Splash Screen
        val splashScreen = installSplashScreen()

        super.onCreate(savedInstanceState)

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
            AgroRouteTheme {
                NavGraph()
            }
        }
    }
}