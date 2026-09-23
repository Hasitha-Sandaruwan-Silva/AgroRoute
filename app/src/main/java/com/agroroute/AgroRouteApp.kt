package com.agroroute

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Application class that bootstraps Hilt dependency injection for the entire app.
 */
@HiltAndroidApp
class AgroRouteApp : Application()
