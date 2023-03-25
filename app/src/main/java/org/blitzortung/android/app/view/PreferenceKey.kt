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

package org.blitzortung.android.app.view

import android.content.SharedPreferences
import android.util.Log
import org.blitzortung.android.app.Main.Companion.LOG_TAG

enum class PreferenceKey(val key: String) {
    USERNAME("username"),
    PASSWORD("password"),
    SERVICE_URL("service_url"),
    RASTER_SIZE("raster_size"),
    COUNT_THRESHOLD("count_threshold"),
    MAP_TYPE("map_mode"),
    MAP_SCALE("map_scale"),
    MAP_FADE("map_fade"),
    COLOR_SCHEME("color_scheme"),
    QUERY_PERIOD("query_period"),
    BACKGROUND_QUERY_PERIOD("background_query_period"),
    SHOW_LOCATION("location"),
    OWN_LOCATION_SIZE("own_location_size"),
    ALERT_ENABLED("alarm_enabled"),
    ALERT_SOUND_SIGNAL("alarm_sound_signal"),
    ALERT_VIBRATION_SIGNAL("alarm_vibration_signal"),
    ALERT_NOTIFICATION_DISTANCE_LIMIT("notification_distance_limit"),
    ALERT_SIGNALING_DISTANCE_LIMIT("signaling_distance_limit"),
    ALERT_SIGNALING_THRESHOLD_TIME("signaling_threshold_time"),
    REGION("region"),
    DATA_SOURCE("data_source"),
    MEASUREMENT_UNIT("measurement_unit"),
    DO_NOT_SLEEP("do_not_sleep"),
    INTERVAL_DURATION("interval_duration"),
    HISTORIC_TIMESTEP("historic_timestep"),
    LOCATION_MODE("location_mode"),
    LOCATION_LONGITUDE("location_longitude"),
    LOCATION_LATITUDE("location_latitude"),
    KEEP_ZOOM_GOTO_OWN_LOCATION("keep_zoom_goto_own_location");

    override fun toString(): String {
        return key
    }

    companion object {

        private val stringToValueMap = HashMap<String, PreferenceKey>()

        init {
            for (key in values()) {
                val keyString = key.toString()
                if (keyString in stringToValueMap) {
                    throw IllegalStateException("key value '%s' already defined".format(keyString))
                }
                stringToValueMap[keyString] = key
            }
        }

        fun fromString(string: String): PreferenceKey? {
            val preferenceKey = stringToValueMap[string]
            if (preferenceKey == null) {
                Log.e(LOG_TAG, "no preferenceKey for $string")
            }
            return preferenceKey
        }
    }
}

//Helper function to retrieve a preference value of a PreferenceKey
internal inline fun <reified T> SharedPreferences.get(prefKey: PreferenceKey, default: T): T {
    val key = prefKey.toString()

    //Set<String> is not possible because of type erasure, so for Set<String> we still need to use the old way
    val value: Any = when (default) {
        is Long -> getLong(key, default)
        is Int -> getInt(key, default)
        is Boolean -> this.getBoolean(key, default)
        is String -> getString(key, default) ?: default
        is Float -> getFloat(key, default)
        else -> throw IllegalArgumentException("Type ${T::class} cannot be retrieved from a SharedPreference")
    }

    return value as T
}

internal inline fun <reified T, V> SharedPreferences.getAndConvert(
    prefKey: PreferenceKey,
    default: T,
    convert: (T) -> V
): V {
    val value = this.get(prefKey, default)
    return convert(value)
}

/**
 *  A generic extension function to set a SharedPreference-Value
 */
internal inline fun <reified T> SharedPreferences.Editor.put(key: String, value: T) {
    when (value) {
        is String -> putString(key, value)
        is Int -> putInt(key, value)
        is Boolean -> putBoolean(key, value)
        is Float -> putFloat(key, value)
        is Long -> putLong(key, value)
        else -> throw IllegalArgumentException("Type ${T::class} cannoted be put inside a SharedPreference")
    }
}

internal inline fun <reified T> SharedPreferences.Editor.put(key: PreferenceKey, value: T) {
    val keyString = key.key

    put(keyString, value)
}

interface OnSharedPreferenceChangeListener : SharedPreferences.OnSharedPreferenceChangeListener {
    fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, vararg keys: PreferenceKey) {
        keys.forEach { onSharedPreferenceChanged(sharedPreferences, it) }
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, keyString: String) {
        val key = PreferenceKey.fromString(keyString)
        key?.also { onSharedPreferenceChanged(sharedPreferences, it) }
    }

    fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: PreferenceKey)

}
