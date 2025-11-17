/*

   Copyright 2015 Andreas Würl

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.

*/

package org.blitzortung.android.app.viewmodel

import android.content.SharedPreferences
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.assertj.core.api.Assertions.assertThat
import org.blitzortung.android.app.view.PreferenceKey
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {
    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = UnconfinedTestDispatcher()

    private lateinit var preferences: SharedPreferences
    private lateinit var editor: SharedPreferences.Editor
    private lateinit var viewModel: SettingsViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        preferences = mockk(relaxed = true)
        editor = mockk(relaxed = true)

        every { preferences.edit() } returns editor
        every { editor.putString(any(), any()) } returns editor
        every { editor.putInt(any(), any()) } returns editor
        every { editor.putBoolean(any(), any()) } returns editor
        every { editor.apply() } returns Unit

        viewModel = SettingsViewModel(preferences)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `getStringPreference returns value from preferences`() {
        every { preferences.getString(PreferenceKey.DATA_SOURCE.key, "default") } returns "RPC"

        val result = viewModel.getStringPreference(PreferenceKey.DATA_SOURCE, "default")

        assertThat(result).isEqualTo("RPC")
    }

    @Test
    fun `getIntPreference returns value from preferences`() {
        every { preferences.getInt(PreferenceKey.QUERY_PERIOD.key, 60) } returns 120

        val result = viewModel.getIntPreference(PreferenceKey.QUERY_PERIOD, 60)

        assertThat(result).isEqualTo(120)
    }

    @Test
    fun `getBooleanPreference returns value from preferences`() {
        every { preferences.getBoolean(PreferenceKey.SHOW_LOCATION.key, false) } returns true

        val result = viewModel.getBooleanPreference(PreferenceKey.SHOW_LOCATION, false)

        assertThat(result).isTrue()
    }

    @Test
    fun `getFloatPreference returns value from preferences`() {
        val testKey = PreferenceKey.SHOW_LOCATION
        every { preferences.getFloat(testKey.key, 1.0f) } returns 2.5f

        val result = viewModel.getFloatPreference(testKey, 1.0f)

        assertThat(result).isEqualTo(2.5f)
    }

    @Test
    fun `setStringPreference updates preferences`() {
        viewModel.setStringPreference(PreferenceKey.DATA_SOURCE, "HTTP")

        verify { editor.putString(PreferenceKey.DATA_SOURCE.key, "HTTP") }
        verify { editor.apply() }
    }

    @Test
    fun `setIntPreference updates preferences`() {
        viewModel.setIntPreference(PreferenceKey.QUERY_PERIOD, 90)

        verify { editor.putInt(PreferenceKey.QUERY_PERIOD.key, 90) }
        verify { editor.apply() }
    }

    @Test
    fun `setBooleanPreference updates preferences`() {
        viewModel.setBooleanPreference(PreferenceKey.SHOW_LOCATION, true)

        verify { editor.putBoolean(PreferenceKey.SHOW_LOCATION.key, true) }
        verify { editor.apply() }
    }

    @Test
    fun `preference change listener is registered`() {
        verify { preferences.registerOnSharedPreferenceChangeListener(any()) }
    }

    @Test
    fun `preference change listener receives callbacks`() {
        val listenerSlot = slot<SharedPreferences.OnSharedPreferenceChangeListener>()
        every {
            preferences.registerOnSharedPreferenceChangeListener(capture(listenerSlot))
        } returns Unit

        viewModel = SettingsViewModel(preferences)

        // Verify listener was registered
        verify { preferences.registerOnSharedPreferenceChangeListener(any()) }
    }

    @Test
    fun `clearPreferenceChange resets state`() {
        val listenerSlot = slot<SharedPreferences.OnSharedPreferenceChangeListener>()
        every {
            preferences.registerOnSharedPreferenceChangeListener(capture(listenerSlot))
        } returns Unit

        viewModel = SettingsViewModel(preferences)

        val listener = listenerSlot.captured
        listener.onSharedPreferenceChanged(preferences, PreferenceKey.DATA_SOURCE.key)

        viewModel.clearPreferenceChange()

        assertThat(viewModel.preferenceChanged.value).isNull()
    }

    @Test
    fun `onCleared unregisters preference listener`() {
        // Trigger onCleared via reflection since it's protected
        val onClearedMethod = SettingsViewModel::class.java.getDeclaredMethod("onCleared")
        onClearedMethod.isAccessible = true
        onClearedMethod.invoke(viewModel)

        verify { preferences.unregisterOnSharedPreferenceChangeListener(any()) }
    }
}
