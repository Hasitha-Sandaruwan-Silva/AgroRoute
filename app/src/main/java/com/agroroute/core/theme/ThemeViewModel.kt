package com.agroroute.core.theme

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

/**
 * Exposes the persisted [ThemeMode] to the UI and forwards user choices to
 * [ThemePreference]. New file — no existing ViewModel is touched.
 */
@HiltViewModel
class ThemeViewModel @Inject constructor(
    private val themePreference: ThemePreference
) : ViewModel() {

    val mode: StateFlow<ThemeMode> = themePreference.mode

    fun setMode(mode: ThemeMode) {
        themePreference.setMode(mode)
    }
}
