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
    SHOW_LOCATION("location"),
    ALARM_ENABLED("alarm_enabled"),
    NOTIFICATION_DISTANCE_LIMIT("notification_distance_limit"),
    VIBRATION_DISTANCE_LIMIT("vibration_distance_limit"),
    REGION("region"),
    DATA_SOURCE("data_source"),
    MEASUREMENT_UNIT("measurement_unit"),
    DO_NOT_SLEEP("do_not_sleep"),
    INTERVAL_DURATION("interval_duration"),
    HISTORIC_TIMESTEP("historic_timestep");
    
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
            stringToValueMap.put(key.toString(), key);
        }
    }

    public static PreferenceKey fromString(String string) {
        return stringToValueMap.get(string);
    }
}
