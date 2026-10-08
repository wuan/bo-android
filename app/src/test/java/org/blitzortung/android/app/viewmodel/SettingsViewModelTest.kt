package org.blitzortung.android.app.viewmodel

import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import io.mockk.MockKAnnotations
import io.mockk.Runs
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.just
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.blitzortung.android.app.view.PreferenceKey
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {
    @MockK
    private lateinit var preferences: SharedPreferences

    @MockK
    private lateinit var editor: SharedPreferences.Editor

    private lateinit var uut: SettingsViewModel

    private val listenerSlot = slot<SharedPreferences.OnSharedPreferenceChangeListener>()

    @Before
    fun setUp() {
        MockKAnnotations.init(this, relaxed = true)
        every { preferences.registerOnSharedPreferenceChangeListener(capture(listenerSlot)) } just Runs
        every { preferences.edit() } returns editor

        uut = SettingsViewModel(preferences)
    }

    private fun TestScope.collectPreferenceChanges(received: MutableList<PreferenceKey>) {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            uut.preferenceChanged.collect { received.add(it) }
        }
    }

    @Test
    fun mapsKnownPreferenceKey() =
        runTest {
            val received = mutableListOf<PreferenceKey>()
            collectPreferenceChanges(received)

            listenerSlot.captured.onSharedPreferenceChanged(preferences, PreferenceKey.USERNAME.key)

            assertThat(received).containsExactly(PreferenceKey.USERNAME)
        }

    @Test
    fun ignoresUnknownPreferenceKey() =
        runTest {
            val received = mutableListOf<PreferenceKey>()
            collectPreferenceChanges(received)

            listenerSlot.captured.onSharedPreferenceChanged(preferences, "does_not_exist")

            assertThat(received).isEmpty()
        }

    @Test
    fun deliversAllPreferenceChangesWithoutConflation() =
        runTest {
            val received = mutableListOf<PreferenceKey>()
            collectPreferenceChanges(received)

            listenerSlot.captured.onSharedPreferenceChanged(preferences, PreferenceKey.USERNAME.key)
            listenerSlot.captured.onSharedPreferenceChanged(preferences, PreferenceKey.PASSWORD.key)

            assertThat(received).containsExactly(PreferenceKey.USERNAME, PreferenceKey.PASSWORD)
        }

    @Test
    fun readsTypedPreferences() {
        every { preferences.getString(PreferenceKey.USERNAME.key, "default") } returns "user"
        every { preferences.getInt(PreferenceKey.QUERY_PERIOD.key, 60) } returns 30
        every { preferences.getBoolean(PreferenceKey.ALERT_ENABLED.key, false) } returns true
        every { preferences.getFloat(PreferenceKey.GRID_SIZE.key, 1.0f) } returns 2.0f

        assertThat(uut.getStringPreference(PreferenceKey.USERNAME, "default")).isEqualTo("user")
        assertThat(uut.getIntPreference(PreferenceKey.QUERY_PERIOD, 60)).isEqualTo(30)
        assertThat(uut.getBooleanPreference(PreferenceKey.ALERT_ENABLED, false)).isTrue()
        assertThat(uut.getFloatPreference(PreferenceKey.GRID_SIZE, 1.0f)).isEqualTo(2.0f)
    }

    @Test
    fun stringPreferenceFallsBackToDefault() {
        every { preferences.getString(PreferenceKey.USERNAME.key, "default") } returns null

        assertThat(uut.getStringPreference(PreferenceKey.USERNAME, "default")).isEqualTo("default")
    }

    @Test
    fun writesTypedPreferences() {
        every { editor.putString(any(), any()) } returns editor
        every { editor.putInt(any(), any()) } returns editor
        every { editor.putBoolean(any(), any()) } returns editor
        every { editor.apply() } just Runs

        uut.setStringPreference(PreferenceKey.USERNAME, "user")
        uut.setIntPreference(PreferenceKey.QUERY_PERIOD, 30)
        uut.setBooleanPreference(PreferenceKey.ALERT_ENABLED, true)

        verify {
            editor.putString(PreferenceKey.USERNAME.key, "user")
            editor.putInt(PreferenceKey.QUERY_PERIOD.key, 30)
            editor.putBoolean(PreferenceKey.ALERT_ENABLED.key, true)
        }
    }

    @Test
    fun unregistersListenerOnClear() {
        val method = ViewModel::class.java.getDeclaredMethod("onCleared")
        method.isAccessible = true
        method.invoke(uut)

        verify { preferences.unregisterOnSharedPreferenceChangeListener(listenerSlot.captured) }
    }
}
