/*

   Copyright 2026 Andreas Würl

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
import androidx.lifecycle.ViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.blitzortung.android.app.view.PreferenceKey

/**
 * SettingsViewModel provides reactive access to preference changes and typed
 * read/write helpers around [SharedPreferences].
 */
class SettingsViewModel
    @Inject
    constructor(
        private val preferences: SharedPreferences,
    ) : ViewModel() {
        private val _preferenceChanged = MutableStateFlow<PreferenceKey?>(null)
        val preferenceChanged: StateFlow<PreferenceKey?> = _preferenceChanged.asStateFlow()

        private val preferenceChangeListener =
            SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
                key?.let {
                    _preferenceChanged.value = PreferenceKey.fromString(it)
                }
            }

        init {
            preferences.registerOnSharedPreferenceChangeListener(preferenceChangeListener)
        }

        fun getStringPreference(
            key: PreferenceKey,
            defaultValue: String,
        ): String = preferences.getString(key.key, defaultValue) ?: defaultValue

        fun getIntPreference(
            key: PreferenceKey,
            defaultValue: Int,
        ): Int = preferences.getInt(key.key, defaultValue)

        fun getBooleanPreference(
            key: PreferenceKey,
            defaultValue: Boolean,
        ): Boolean = preferences.getBoolean(key.key, defaultValue)

        fun getFloatPreference(
            key: PreferenceKey,
            defaultValue: Float,
        ): Float = preferences.getFloat(key.key, defaultValue)

        fun setStringPreference(
            key: PreferenceKey,
            value: String,
        ) {
            preferences.edit().putString(key.key, value).apply()
        }

        fun setIntPreference(
            key: PreferenceKey,
            value: Int,
        ) {
            preferences.edit().putInt(key.key, value).apply()
        }

        fun setBooleanPreference(
            key: PreferenceKey,
            value: Boolean,
        ) {
            preferences.edit().putBoolean(key.key, value).apply()
        }

        fun clearPreferenceChange() {
            _preferenceChanged.value = null
        }

        override fun onCleared() {
            super.onCleared()
            preferences.unregisterOnSharedPreferenceChangeListener(preferenceChangeListener)
        }
    }
