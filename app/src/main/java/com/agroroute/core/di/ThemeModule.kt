package com.agroroute.core.di

import com.agroroute.core.theme.SharedThemeStore
import com.agroroute.core.theme.ThemeStore
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Binds the theme preference persistence seam to its SharedPreferences
 * implementation. [com.agroroute.core.theme.ThemePreference] itself is
 * constructor-injected and @Singleton, so no extra provider is needed.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class ThemeModule {

    @Binds
    abstract fun bindThemeStore(impl: SharedThemeStore): ThemeStore
}
