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

package org.blitzortung.android.app

import android.content.Intent
import android.content.SharedPreferences
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.location.LocationManager
import android.os.Bundle
import android.preference.PreferenceActivity
import android.preference.PreferenceManager
import android.provider.Settings

import org.blitzortung.android.app.view.PreferenceKey
import org.blitzortung.android.app.view.get
import org.blitzortung.android.data.provider.DataProviderType
import org.blitzortung.android.location.LocationHandler
import org.jetbrains.anko.locationManager

class Preferences : PreferenceActivity(), OnSharedPreferenceChangeListener {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        addPreferencesFromResource(R.xml.preferences)

        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        prefs.registerOnSharedPreferenceChangeListener(this)

        configureDataSourcePreferences(prefs)
        configureLocationProviderPreferences(prefs)
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
                configureDataSourcePreferences(sharedPreferences)
            }

            PreferenceKey.LOCATION_MODE -> {
                val provider = configureLocationProviderPreferences(sharedPreferences)

                if(provider != LocationHandler.MANUAL_PROVIDER && !this.locationManager.isProviderEnabled(provider)) {
                    startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                }
            }
        }
    }

    private fun configureDataSourcePreferences(sharedPreferences: SharedPreferences): DataProviderType {
        val providerTypeString = sharedPreferences.get(PreferenceKey.DATA_SOURCE, DataProviderType.HTTP.toString())
        val providerType = DataProviderType.valueOf(providerTypeString.toUpperCase())

        when (providerType) {
            DataProviderType.HTTP -> enableBlitzortungHttpMode()
            DataProviderType.RPC -> enableAppServiceMode()
        }

        return providerType
    }

    private fun configureLocationProviderPreferences(sharedPreferences: SharedPreferences) : String {
        val locationProvider = sharedPreferences.get(PreferenceKey.LOCATION_MODE, LocationManager.NETWORK_PROVIDER)
        enableManualLocationMode(locationProvider === LocationHandler.MANUAL_PROVIDER)

        return locationProvider
    }


    private fun enableAppServiceMode() {
        findPreference("raster_size").isEnabled = true
        findPreference("service_url").isEnabled = true
        findPreference("username").isEnabled = false
        findPreference("password").isEnabled = false
    }

    private fun enableBlitzortungHttpMode() {
        findPreference("raster_size").isEnabled = false
        findPreference("service_url").isEnabled = false
        findPreference("username").isEnabled = true
        findPreference("password").isEnabled = true
    }

    private fun enableManualLocationMode(enabled: Boolean) {
        findPreference("location_longitude").isEnabled = enabled
        findPreference("location_latitude").isEnabled = enabled
    }

}
