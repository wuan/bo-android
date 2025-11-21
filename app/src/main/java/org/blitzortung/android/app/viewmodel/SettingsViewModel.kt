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
import androidx.lifecycle.ViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.blitzortung.android.app.view.PreferenceKey

/**
 * SettingsViewModel manages application settings and preferences.
 * Provides reactive access to preference changes and validation.
 */
class SettingsViewModel
    @Inject
    constructor(
        private val preferences: SharedPreferences,
    ) : ViewModel() {
        // Preference change notification
        private val _preferenceChanged = MutableStateFlow<PreferenceKey?>(null)
        val preferenceChanged: StateFlow<PreferenceKey?> = _preferenceChanged.asStateFlow()

        private val preferenceChangeListener =
            SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
                key?.let {
                    try {
                        val preferenceKey = PreferenceKey.valueOf(it)
                        _preferenceChanged.value = preferenceKey
                    } catch (e: IllegalArgumentException) {
                        // Ignore unknown preference keys
                    }
                }
            }

        init {
            preferences.registerOnSharedPreferenceChangeListener(preferenceChangeListener)
        }

        /**
         * Get string preference value
         */
        fun getStringPreference(
            key: PreferenceKey,
            defaultValue: String,
        ): String = preferences.getString(key.key, defaultValue) ?: defaultValue

        /**
         * Get int preference value
         */
        fun getIntPreference(
            key: PreferenceKey,
            defaultValue: Int,
        ): Int = preferences.getInt(key.key, defaultValue)

        /**
         * Get boolean preference value
         */
        fun getBooleanPreference(
            key: PreferenceKey,
            defaultValue: Boolean,
        ): Boolean = preferences.getBoolean(key.key, defaultValue)

        /**
         * Get float preference value
         */
        fun getFloatPreference(
            key: PreferenceKey,
            defaultValue: Float,
        ): Float = preferences.getFloat(key.key, defaultValue)

        /**
         * Set string preference value
         */
        fun setStringPreference(
            key: PreferenceKey,
            value: String,
        ) {
            preferences.edit().putString(key.key, value).apply()
        }

        /**
         * Set int preference value
         */
        fun setIntPreference(
            key: PreferenceKey,
            value: Int,
        ) {
            preferences.edit().putInt(key.key, value).apply()
        }

        /**
         * Set boolean preference value
         */
        fun setBooleanPreference(
            key: PreferenceKey,
            value: Boolean,
        ) {
            preferences.edit().putBoolean(key.key, value).apply()
        }

        /**
         * Clear preference change notification
         */
        fun clearPreferenceChange() {
            _preferenceChanged.value = null
        }

        override fun onCleared() {
            super.onCleared()
            preferences.unregisterOnSharedPreferenceChangeListener(preferenceChangeListener)
        }
    }
