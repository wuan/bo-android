package org.blitzortung.android.app.view;

import java.util.HashMap;
import java.util.Map;

public enum PreferenceKey {
    USERNAME("username"),
    PASSWORD("password"),
    RASTER_SIZE("raster_size"),
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
    
    private final String key;
    
    private PreferenceKey(String key) {
        this.key = key;
    }

    @Override
    public String toString()
    {
        return key;
    }

    private static Map<String, PreferenceKey> stringToValueMap = new HashMap<String, PreferenceKey>();
    static {
        for (PreferenceKey key : PreferenceKey.values()) {
            String keyString = key.toString();
            if (stringToValueMap.containsKey(keyString)) {
                throw new IllegalStateException(String.format("key value '%s' already defined", keyString));
            }
            stringToValueMap.put(keyString, key);
        }
    }

    public static PreferenceKey fromString(String string) {
        return stringToValueMap.get(string);
    }
}
