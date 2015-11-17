package org.blitzortung.android.app;

import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;

import org.blitzortung.android.app.view.PreferenceKey;
import org.blitzortung.android.data.provider.DataProviderType;
import org.blitzortung.android.location.LocationHandler;

public class Preferences extends PreferenceActivity implements OnSharedPreferenceChangeListener {


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.preferences);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        prefs.registerOnSharedPreferenceChangeListener(this);

        onSharedPreferenceChanged(prefs, PreferenceKey.DATA_SOURCE, PreferenceKey.LOCATION_MODE);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String keyString) {
        onSharedPreferenceChanged(sharedPreferences, PreferenceKey.fromString(keyString));
    }

    private void onSharedPreferenceChanged(SharedPreferences sharedPreferences, PreferenceKey... keys) {
        for (PreferenceKey key : keys) {
            onSharedPreferenceChanged(sharedPreferences, key);
        }
    }

    private void onSharedPreferenceChanged(SharedPreferences sharedPreferences, PreferenceKey key) {
        switch (key) {
            case DATA_SOURCE:
                String providerTypeString = sharedPreferences.getString(PreferenceKey.DATA_SOURCE.toString(), DataProviderType.HTTP.toString());
                DataProviderType providerType = DataProviderType.valueOf(providerTypeString.toUpperCase());

                switch (providerType) {
                    case HTTP:
                        enableBlitzortungHttpMode();
                        break;
                    case RPC:
                        enableAppServiceMode();
                        break;
                }
                break;

            case LOCATION_MODE:
                LocationHandler.Provider locationProvider = LocationHandler.Provider.fromString(sharedPreferences.getString(key.toString(), "NETWORK"));
                enableManualLocationMode(locationProvider == LocationHandler.Provider.MANUAL);
                break;

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

    private void enableManualLocationMode(boolean enabled) {
        findPreference("location_longitude").setEnabled(enabled);
        findPreference("location_latitude").setEnabled(enabled);
    }

}
