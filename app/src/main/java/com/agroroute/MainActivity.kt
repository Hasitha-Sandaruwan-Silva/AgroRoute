package com.agroroute

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.agroroute.core.navigation.NavGraph
import com.agroroute.core.theme.AgroRouteTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * Single-activity entry point that hosts the Compose NavHost for the entire application.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AgroRouteTheme {
                NavGraph()
            }
        }
    }
}
