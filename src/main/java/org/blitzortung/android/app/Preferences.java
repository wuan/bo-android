package org.blitzortung.android.app;

import org.blitzortung.android.data.provider.DataProviderType;

import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;

public class Preferences extends PreferenceActivity implements OnSharedPreferenceChangeListener {

	public static final String USERNAME_KEY = "username";
	public static final String PASSWORD_KEY = "password";
	public static final String RASTER_SIZE_KEY = "raster_size";
	public static final String MAP_TYPE_KEY = "map_mode";
	public static final String QUERY_PERIOD_KEY = "query_period";
	public static final String BACKGROUND_QUERY_PERIOD_KEY = "background_query_period";
	public static final String SHOW_LOCATION_KEY = "location";
	public static final String ALARM_ENABLED_KEY = "alarm_enabled";
	public static final String NOTIFICATION_DISTANCE_LIMIT = "notification_distance_limit";
	public static final String VIBRATION_DISTANCE_LIMIT = "vibration_distance_limit";	
	public static final String REGION_KEY = "region";
	public static final String DATA_SOURCE_KEY = "data_source";

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		addPreferencesFromResource(R.xml.preferences);

		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		prefs.registerOnSharedPreferenceChangeListener(this);

		onSharedPreferenceChanged(prefs, Preferences.DATA_SOURCE_KEY);
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
		if (key.equals(Preferences.DATA_SOURCE_KEY)) {
			String providerTypeString = sharedPreferences.getString(Preferences.DATA_SOURCE_KEY, DataProviderType.HTTP.toString());
			DataProviderType providerType = DataProviderType.valueOf(providerTypeString.toUpperCase());

            switch (providerType) {
			case HTTP:
				enableBlitzortungHttpMode();
				break;
			case RPC:
				enableAppServiceMode();
				break;
			}
		}
	}
	
	private void enableAppServiceMode() {
		findPreference("raster_size").setEnabled(true);
		findPreference("username").setEnabled(false);
		findPreference("password").setEnabled(false);
	}

	private void enableBlitzortungHttpMode() {
		findPreference("raster_size").setEnabled(false);
		findPreference("username").setEnabled(true);
		findPreference("password").setEnabled(true);
	}

}
