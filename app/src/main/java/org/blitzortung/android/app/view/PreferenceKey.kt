package org.blitzortung.android.app.view

import java.util.*

enum class PreferenceKey internal constructor(private val key: String) {
    USERNAME("username"),
    PASSWORD("password"),
    SERVICE_URL("service_url"),
    RASTER_SIZE("raster_size"),
    COUNT_THRESHOLD("count_threshold"),
    MAP_TYPE("map_mode"),
    MAP_FADE("map_fade"),
    COLOR_SCHEME("color_scheme"),
    QUERY_PERIOD("query_period"),
    BACKGROUND_QUERY_PERIOD("background_query_period"),
    SHOW_PARTICIPANTS("show_participants"),
    SHOW_LOCATION("location"),
    ALERT_ENABLED("alarm_enabled"),
    ALERT_SOUND_SIGNAL("alarm_sound_signal"),
    ALERT_VIBRATION_SIGNAL("alarm_vibration_signal"),
    ALERT_NOTIFICATION_DISTANCE_LIMIT("notification_distance_limit"),
    ALERT_SIGNALING_DISTANCE_LIMIT("signaling_distance_limit"),
    REGION("region"),
    DATA_SOURCE("data_source"),
    MEASUREMENT_UNIT("measurement_unit"),
    DO_NOT_SLEEP("do_not_sleep"),
    INTERVAL_DURATION("interval_duration"),
    HISTORIC_TIMESTEP("historic_timestep"),
    LOCATION_MODE("location_mode"),
    LOCATION_LONGITUDE("location_longitude"),
    LOCATION_LATITUDE("location_latitude");

    override fun toString(): String {
        return key
    }

    companion object {

        private val stringToValueMap = HashMap<String, PreferenceKey>()

        init {
            for (key in PreferenceKey.values()) {
                val keyString = key.toString()
                if (keyString in stringToValueMap) {
                    throw IllegalStateException("key value '%s' already defined".format(keyString))
                }
                stringToValueMap[keyString] = key
            }
        }

        fun fromString(string: String): PreferenceKey {
            return stringToValueMap[string]!!
        }
    }
}
