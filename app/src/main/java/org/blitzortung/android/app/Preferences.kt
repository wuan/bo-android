package org.blitzortung.android.app

import android.content.SharedPreferences
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.os.Bundle
import android.preference.PreferenceActivity
import android.preference.PreferenceManager

import org.blitzortung.android.app.view.PreferenceKey
import org.blitzortung.android.data.provider.DataProviderType
import org.blitzortung.android.location.LocationHandler

class Preferences : PreferenceActivity(), OnSharedPreferenceChangeListener {


    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        addPreferencesFromResource(R.xml.preferences)

        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        prefs.registerOnSharedPreferenceChangeListener(this)

        onSharedPreferenceChanged(prefs, PreferenceKey.DATA_SOURCE, PreferenceKey.LOCATION_MODE)
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, keyString: String) {
        onSharedPreferenceChanged(sharedPreferences, PreferenceKey.fromString(keyString))
    }

    private fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, vararg keys: PreferenceKey) {
        for (key in keys) {
            onSharedPreferenceChanged(sharedPreferences, key)
        }
    }

    private fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: PreferenceKey) {
        when (key) {
            PreferenceKey.DATA_SOURCE -> {
                val providerTypeString = sharedPreferences.getString(PreferenceKey.DATA_SOURCE.toString(), DataProviderType.HTTP.toString())
                val providerType = DataProviderType.valueOf(providerTypeString.toUpperCase())

                when (providerType) {
                    DataProviderType.HTTP -> enableBlitzortungHttpMode()
                    DataProviderType.RPC -> enableAppServiceMode()
                }
            }

            PreferenceKey.LOCATION_MODE -> {
                val locationProvider = LocationHandler.Provider.fromString(sharedPreferences.getString(key.toString(), "NETWORK"))
                enableManualLocationMode(locationProvider === LocationHandler.Provider.MANUAL)
            }
        }
    }

    private fun enableAppServiceMode() {
        findPreference("raster_size").isEnabled = true
        findPreference("username").isEnabled = false
        findPreference("password").isEnabled = false
    }

    private fun enableBlitzortungHttpMode() {
        findPreference("raster_size").isEnabled = false
        findPreference("username").isEnabled = true
        findPreference("password").isEnabled = true
    }

    private fun enableManualLocationMode(enabled: Boolean) {
        findPreference("location_longitude").isEnabled = enabled
        findPreference("location_latitude").isEnabled = enabled
    }

}
