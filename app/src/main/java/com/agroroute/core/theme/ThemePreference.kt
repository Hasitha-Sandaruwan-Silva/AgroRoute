package com.agroroute.core.theme

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/** Three-way theme mode. SYSTEM is the default and follows the device setting. */
enum class ThemeMode {
    SYSTEM, LIGHT, DARK;

    companion object {
        fun from(raw: String?): ThemeMode =
            entries.firstOrNull { it.name == raw } ?: SYSTEM
    }
}

/** Minimal persistence seam so [ThemePreference] can be unit tested with a fake. */
interface ThemeStore {
    fun readMode(): String?
    fun writeMode(value: String)
}

/**
 * Holds the user's theme choice. The current mode is exposed as a Flow so the
 * activity can apply it immediately (no restart) and it survives process death.
 */
@Singleton
class ThemePreference @Inject constructor(private val store: ThemeStore) {

    private val _mode = MutableStateFlow(ThemeMode.from(store.readMode()))
    val mode: StateFlow<ThemeMode> = _mode.asStateFlow()

    fun setMode(mode: ThemeMode) {
        store.writeMode(mode.name)
        _mode.value = mode
    }
}

/** [ThemeStore] backed by SharedPreferences (no DataStore dependency in this project). */
class SharedThemeStore @Inject constructor(@ApplicationContext context: Context) : ThemeStore {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    override fun readMode(): String? = prefs.getString(KEY_MODE, null)

    override fun writeMode(value: String) {
        prefs.edit().putString(KEY_MODE, value).apply()
    }

    private companion object {
        const val PREFS_NAME = "agroroute_theme"
        const val KEY_MODE = "theme_mode"
    }
}
