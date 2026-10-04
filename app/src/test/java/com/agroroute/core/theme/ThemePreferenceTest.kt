package com.agroroute.core.theme

import org.junit.Assert.assertEquals
import org.junit.Test

class FakeThemeStore(initialMode: String? = null) : ThemeStore {
    private var storedValue: String? = initialMode

    override fun readMode(): String? = storedValue

    override fun writeMode(value: String) {
        storedValue = value
    }
}

class ThemePreferenceTest {

    @Test
    fun defaultModeIsSystemWhenStoreIsEmpty() {
        val store = FakeThemeStore(initialMode = null)
        val preference = ThemePreference(store)

        assertEquals(ThemeMode.SYSTEM, preference.mode.value)
    }

    @Test
    fun setModeUpdatesFlowAndPersistsValue() {
        val store = FakeThemeStore(initialMode = null)
        val preference = ThemePreference(store)

        preference.setMode(ThemeMode.DARK)

        assertEquals(ThemeMode.DARK, preference.mode.value)
        assertEquals("DARK", store.readMode())
    }

    @Test
    fun initialModeIsRestoredFromStore() {
        val store = FakeThemeStore(initialMode = "LIGHT")
        val preference = ThemePreference(store)

        assertEquals(ThemeMode.LIGHT, preference.mode.value)
    }

    @Test
    fun viewModelDelegatesModeChangesToPreference() {
        val store = FakeThemeStore(initialMode = null)
        val preference = ThemePreference(store)
        val viewModel = ThemeViewModel(preference)

        assertEquals(ThemeMode.SYSTEM, viewModel.mode.value)

        viewModel.setMode(ThemeMode.LIGHT)

        assertEquals(ThemeMode.LIGHT, viewModel.mode.value)
        assertEquals(ThemeMode.LIGHT, preference.mode.value)
        assertEquals("LIGHT", store.readMode())
    }
}
