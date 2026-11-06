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
 * The direction in which the car map background is shaded. There is no unshaded
 * value: the car always washes the map so the strike data reads first.
 */
enum class MapShading {
    /** Translucent black scrim with the bright strike palette. */
    DARK,

    /** Translucent white scrim with the darker strike palette. */
    BRIGHT,
}

/**
 * How the car chooses the map shading direction.
 */
enum class MapShadingMode {
    /** Use dark shading in a dark car theme and bright shading in a light one. */
    FOLLOW_CAR,

    /** Always shade the map dark. */
    DARK,

    /** Always shade the map bright. */
    BRIGHT,
    ;

    /** The shading direction for the given car theme. */
    fun shading(carIsDark: Boolean): MapShading =
        when (this) {
            DARK -> MapShading.DARK
            BRIGHT -> MapShading.BRIGHT
            FOLLOW_CAR -> if (carIsDark) MapShading.DARK else MapShading.BRIGHT
        }

    companion object {
        /** Parses a stored mode name, falling back to [FOLLOW_CAR] when absent or unknown. */
        fun fromStored(value: String?): MapShadingMode = entries.firstOrNull { it.name == value } ?: FOLLOW_CAR
    }
}

/**
 * A stored map camera: the centre coordinate and zoom the map last showed.
 */
data class CarCamera(
    val latitude: Double,
    val longitude: Double,
    val zoom: Double,
)

/**
 * Car-only preferences, kept in a dedicated file so the car experience never
 * shares or overwrites the phone app's default preferences. Screens and the map
 * read and write the same instance, which is provided with the car dependencies.
 */
class CarSettings(context: Context) {
    private val preferences: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val mapShadingModeObservers = mutableListOf<(MapShadingMode) -> Unit>()

    /** The selected map shading mode; defaults to [MapShadingMode.FOLLOW_CAR]. */
    var mapShadingMode: MapShadingMode
        get() = MapShadingMode.fromStored(preferences.getString(KEY_MAP_SHADING_MODE, null))
        set(value) {
            preferences.edit().putString(KEY_MAP_SHADING_MODE, value.name).apply()
            mapShadingModeObservers.toList().forEach { it(value) }
        }

    /** Registers [observer] to be notified whenever the map shading mode changes. */
    fun observeMapShadingMode(observer: (MapShadingMode) -> Unit) {
        mapShadingModeObservers.add(observer)
    }

    /** Removes a previously registered map shading mode observer. */
    fun removeMapShadingModeObserver(observer: (MapShadingMode) -> Unit) {
        mapShadingModeObservers.remove(observer)
    }

    /** The stored map camera, or `null` when none has been saved. */
    val camera: CarCamera?
        get() {
            if (!preferences.contains(KEY_CAMERA_LATITUDE) ||
                !preferences.contains(KEY_CAMERA_LONGITUDE) ||
                !preferences.contains(KEY_CAMERA_ZOOM)
            ) {
                return null
            }
            return CarCamera(
                latitude = preferences.getFloat(KEY_CAMERA_LATITUDE, 0f).toDouble(),
                longitude = preferences.getFloat(KEY_CAMERA_LONGITUDE, 0f).toDouble(),
                zoom = preferences.getFloat(KEY_CAMERA_ZOOM, 0f).toDouble(),
            )
        }

    /** Stores the map camera so the next session can restore it. */
    fun storeCamera(camera: CarCamera) {
        preferences
            .edit()
            .putFloat(KEY_CAMERA_LATITUDE, camera.latitude.toFloat())
            .putFloat(KEY_CAMERA_LONGITUDE, camera.longitude.toFloat())
            .putFloat(KEY_CAMERA_ZOOM, camera.zoom.toFloat())
            .apply()
    }

    companion object {
        /** Car-only preferences file; never the app's default preferences. */
        const val PREFS_NAME = "car_settings"

        /** Key for the map shading mode; stored as its name for forward-compatibility. */
        const val KEY_MAP_SHADING_MODE = "map_shading_mode"

        /** Key for the stored camera centre latitude. */
        const val KEY_CAMERA_LATITUDE = "camera_latitude"

        /** Key for the stored camera centre longitude. */
        const val KEY_CAMERA_LONGITUDE = "camera_longitude"

        /** Key for the stored camera zoom. */
        const val KEY_CAMERA_ZOOM = "camera_zoom"
    }
}
