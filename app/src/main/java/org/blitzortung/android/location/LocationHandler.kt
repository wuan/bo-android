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

package org.blitzortung.android.location

import android.Manifest
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.location.GpsStatus
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import android.support.v4.content.PermissionChecker.checkSelfPermission
import android.util.Log
import android.widget.Toast
import org.blitzortung.android.app.Main
import org.blitzortung.android.app.R
import org.blitzortung.android.app.view.PreferenceKey
import org.blitzortung.android.protocol.ConsumerContainer
import java.util.*

class LocationHandler(
        private val context: Context,
        sharedPreferences: SharedPreferences,
        private val locationManager: LocationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
) : SharedPreferences.OnSharedPreferenceChangeListener, android.location.LocationListener, GpsStatus.Listener {
    private var location: Location
    private var backgroundMode = true
    private var provider: Provider? = null
    private val consumerContainer = object : ConsumerContainer<LocationEvent>() {
        override fun addedFirstConsumer() {
            enableProvider(provider)
            Log.d(Main.LOG_TAG, "LocationHandler enable provider")
        }

        override fun removedLastConsumer() {
            locationManager.removeUpdates(this@LocationHandler)
            Log.d(Main.LOG_TAG, "LocationHandler disable provider")
        }
    }

    init {
        location = Location("")
        invalidateLocation()

        onSharedPreferenceChanged(sharedPreferences, PreferenceKey.LOCATION_MODE)
        onSharedPreferenceChanged(sharedPreferences, PreferenceKey.BACKGROUND_QUERY_PERIOD)

        sharedPreferences.registerOnSharedPreferenceChangeListener(this)
    }

    override fun onLocationChanged(location: android.location.Location) {
        this.location.set(location)
        sendLocationUpdate()
    }

    override fun onStatusChanged(s: String, i: Int, bundle: Bundle) {
    }

    override fun onProviderEnabled(s: String) {
    }

    override fun onProviderDisabled(s: String) {
        invalidateLocation()
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, keyString: String) {
        onSharedPreferenceChanged(sharedPreferences, PreferenceKey.fromString(keyString))
    }

    private fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: PreferenceKey) {
        when (key) {
            PreferenceKey.LOCATION_MODE -> {
                var newProvider = Provider.fromString(sharedPreferences.getString(key.toString(), Provider.PASSIVE.type))!!
                if (newProvider == Provider.PASSIVE || newProvider == Provider.GPS) {
                    if (checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                        newProvider = Provider.MANUAL
                    }
                }
                if (newProvider == Provider.NETWORK) {
                    if (checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                        newProvider = Provider.MANUAL
                    }
                }
                if (newProvider != provider) {
                    updateProvider(newProvider, sharedPreferences)
                }
            }

            PreferenceKey.LOCATION_LONGITUDE -> updateManualLongitude(sharedPreferences)

            PreferenceKey.LOCATION_LATITUDE -> updateManualLatitude(sharedPreferences)
        }
    }

    private fun updateManualLatitude(sharedPreferences: SharedPreferences) {
        val latitudeString = sharedPreferences.getString(PreferenceKey.LOCATION_LATITUDE.toString(), "49.0")
        try {
            location.latitude = java.lang.Double.valueOf(latitudeString)
            sendLocationUpdate()
        } catch (e: NumberFormatException) {
            Log.e(Main.LOG_TAG, "bad latitude number format '$latitudeString'")
        }
    }

    private fun updateManualLongitude(sharedPreferences: SharedPreferences) {
        val longitudeString = sharedPreferences.getString(PreferenceKey.LOCATION_LONGITUDE.toString(), "11.0")
        try {
            location.longitude = java.lang.Double.valueOf(longitudeString)
            sendLocationUpdate()
        } catch (e: NumberFormatException) {
            Log.e(Main.LOG_TAG, "bad longitude number format '$longitudeString'")
        }
    }

    private fun updateProvider(newProvider: Provider, sharedPreferences: SharedPreferences) {
        if (newProvider == Provider.MANUAL) {
            locationManager.removeUpdates(this)
            updateManualLongitude(sharedPreferences)
            updateManualLatitude(sharedPreferences)
            location.provider = newProvider.type
            sendLocationUpdate()
        } else {
            invalidateLocationAndSendLocationUpdate()
        }
        enableProvider(newProvider)
    }

    private fun invalidateLocationAndSendLocationUpdate() {
        sendLocationUpdateToListeners(null)
        invalidateLocation()
    }

    private fun invalidateLocation() {
        location = Location("")
    }

    private fun enableProvider(newProvider: Provider?) {
        locationManager.removeUpdates(this)
        locationManager.removeGpsStatusListener(this)
        if (newProvider != null && newProvider != Provider.MANUAL) {
            if (!locationManager.allProviders.contains(newProvider.type)) {
                val toast = Toast.makeText(context, context.resources.getText(R.string.location_provider_not_available).toString().format(newProvider.toString()), Toast.LENGTH_LONG)
                toast.show()
                return
            }

            if (!locationManager.isProviderEnabled(newProvider.type)) {
                val toast = Toast.makeText(context, context.resources.getText(R.string.location_provider_disabled).toString().format(newProvider.toString()), Toast.LENGTH_LONG)
                toast.show()
                return
            }

            val minTime = when {
                backgroundMode -> 120000
                provider == Provider.GPS -> 1000
                else -> 20000
            }

            val minDistance = if (backgroundMode)
                200
            else
                50
            Log.v(Main.LOG_TAG, "LocationHandler.enableProvider() $newProvider, minTime: $minTime, minDist: $minDistance")
            if (newProvider == Provider.GPS) {
                // TODO check for enabled service here
                locationManager.addGpsStatusListener(this)
            }

            locationManager.requestLocationUpdates(newProvider.type, minTime.toLong(), minDistance.toFloat(), this)
        }
        provider = newProvider
    }

    private fun sendLocationUpdate() {
        sendLocationUpdateToListeners(if (locationIsValid()) location else null)
    }

    private fun sendLocationUpdateToListeners(location: Location?) {
        consumerContainer.storeAndBroadcast(LocationEvent(location))
    }

    fun requestUpdates(locationConsumer: (LocationEvent) -> Unit) {
        consumerContainer.addConsumer(locationConsumer)
    }

    fun removeUpdates(locationEventConsumer: (LocationEvent) -> Unit) {
        consumerContainer.removeConsumer(locationEventConsumer)
    }

    private fun locationIsValid(): Boolean {
        return !java.lang.Double.isNaN(location.longitude) && !java.lang.Double.isNaN(location.latitude)
    }

    override fun onGpsStatusChanged(event: Int) {
        if (provider == Provider.GPS) {
            when (event) {
                GpsStatus.GPS_EVENT_SATELLITE_STATUS -> {
                    val lastKnownGpsLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                    if (lastKnownGpsLocation != null) {
                        val secondsElapsedSinceLastFix = (System.currentTimeMillis() - lastKnownGpsLocation.time) / 1000

                        if (secondsElapsedSinceLastFix < 10) {
                            if (!locationIsValid()) {
                                location.set(lastKnownGpsLocation)
                                onLocationChanged(location)
                            }
                        }
                    }
                    if (locationIsValid()) {
                        invalidateLocationAndSendLocationUpdate()
                    }
                }
            }
        }
    }

    fun enableBackgroundMode() {
        backgroundMode = true
    }

    fun disableBackgroundMode() {
        backgroundMode = false
    }

    fun updateProvider() {
        enableProvider(provider)
    }

    fun update(preferences: SharedPreferences) {
        onSharedPreferenceChanged(preferences, PreferenceKey.LOCATION_MODE)
    }

    enum class Provider internal constructor(val type: String) {
        NETWORK(LocationManager.NETWORK_PROVIDER),
        GPS(LocationManager.GPS_PROVIDER),
        PASSIVE(LocationManager.PASSIVE_PROVIDER),
        MANUAL("manual");

        companion object {

            private val stringToValueMap = HashMap<String, Provider>()

            init {
                for (key in Provider.values()) {
                    val keyString = key.type
                    if (stringToValueMap.containsKey(keyString)) {
                        throw IllegalStateException("key value '%s' already defined".format(keyString))
                    }
                    stringToValueMap.put(keyString, key)
                }
            }

            fun fromString(string: String): Provider? {
                return stringToValueMap[string]
            }
        }
    }

}
