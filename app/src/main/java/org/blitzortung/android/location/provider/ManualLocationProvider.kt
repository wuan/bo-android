package org.blitzortung.android.location.provider

import android.content.SharedPreferences
import android.location.Location
import android.util.Log
import org.blitzortung.android.app.Main
import org.blitzortung.android.app.view.PreferenceKey
import org.blitzortung.android.app.view.getAndConvert
import org.blitzortung.android.location.LocationHandler

class ManualLocationProvider(locationUpdate: (Location?) -> Unit, private val sharedPreferences: SharedPreferences) : LocationProvider(locationUpdate), SharedPreferences.OnSharedPreferenceChangeListener {
    override val isEnabled: Boolean = true

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String) {
        onSharedPreferenceChanged(sharedPreferences, PreferenceKey.fromString(key))
    }

    private fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: PreferenceKey) {
        val doubleConverter = fun (x: String): Double? {
            try {
                return x.toDouble()
            } catch (e: NumberFormatException) {
                Log.e(Main.LOG_TAG, "bad longitude/latitude number format '$x'")
            }

            return null
        }

        when(key) {
            PreferenceKey.LOCATION_LONGITUDE -> {
                location.longitude = sharedPreferences.getAndConvert(key, "11.0", doubleConverter) ?: 11.0
                sendLocationUpdate()
            }
            PreferenceKey.LOCATION_LATITUDE -> {
                location.latitude = sharedPreferences.getAndConvert(key, "49.0", doubleConverter) ?: 49.0
                sendLocationUpdate()
            }
        }
    }

    override val type = LocationHandler.MANUAL_PROVIDER

    override fun start() {
        sharedPreferences.registerOnSharedPreferenceChangeListener(this)

        onSharedPreferenceChanged(sharedPreferences, PreferenceKey.LOCATION_LONGITUDE)
        onSharedPreferenceChanged(sharedPreferences, PreferenceKey.LOCATION_LATITUDE)
    }

    override fun shutdown() {
        sharedPreferences.unregisterOnSharedPreferenceChangeListener(this)
    }
}