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

package org.blitzortung.android.car

import android.content.Context
import android.content.SharedPreferences

/**
 * How the car map surface is dimmed by its translucent overlay.
 */
enum class MapDimMode {
    /** Dim the map while the car host reports dark mode. */
    FOLLOW_CAR,

    /** Always dim the map. */
    ON,

    /** Never dim the map. */
    OFF,
    ;

    /** Whether the map should be dimmed for the given car theme. */
    fun isDimmed(carIsDark: Boolean): Boolean =
        when (this) {
            ON -> true
            OFF -> false
            FOLLOW_CAR -> carIsDark
        }

    companion object {
        /** Parses a stored mode name, falling back to [FOLLOW_CAR] when absent or unknown. */
        fun fromStored(value: String?): MapDimMode = entries.firstOrNull { it.name == value } ?: FOLLOW_CAR
    }
}

/**
 * Car-only preferences, kept in a dedicated file so the car experience never
 * shares or overwrites the phone app's default preferences. Screens and the map
 * read and write the same instance, which is provided with the car dependencies.
 */
class CarSettings(context: Context) {
    private val preferences: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val mapDimModeObservers = mutableListOf<(MapDimMode) -> Unit>()

    /** The selected map dimming mode; defaults to [MapDimMode.FOLLOW_CAR]. */
    var mapDimMode: MapDimMode
        get() = MapDimMode.fromStored(preferences.getString(KEY_MAP_DIM_MODE, null))
        set(value) {
            preferences.edit().putString(KEY_MAP_DIM_MODE, value.name).apply()
            mapDimModeObservers.toList().forEach { it(value) }
        }

    /** Registers [observer] to be notified whenever the map dimming mode changes. */
    fun observeMapDimMode(observer: (MapDimMode) -> Unit) {
        mapDimModeObservers.add(observer)
    }

    /** Removes a previously registered map dimming mode observer. */
    fun removeMapDimModeObserver(observer: (MapDimMode) -> Unit) {
        mapDimModeObservers.remove(observer)
    }

    companion object {
        /** Car-only preferences file; never the app's default preferences. */
        const val PREFS_NAME = "car_settings"

        /** Key for the map dimming mode; stored as its name for forward-compatibility. */
        const val KEY_MAP_DIM_MODE = "map_dim_mode"
    }
}
