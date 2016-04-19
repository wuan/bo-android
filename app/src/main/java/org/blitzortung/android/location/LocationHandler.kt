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

import android.content.*
import android.location.Location
import android.location.LocationManager
import android.util.Log
import org.blitzortung.android.app.event.BackgroundModeEvent
import org.blitzortung.android.app.BackgroundModeHandler
import org.blitzortung.android.app.Main
import org.blitzortung.android.app.R
import org.blitzortung.android.app.view.PreferenceKey
import org.blitzortung.android.app.view.get
import org.blitzortung.android.location.provider.LocationProvider
import org.blitzortung.android.location.provider.ManagerLocationProvider
import org.blitzortung.android.location.provider.createLocationProvider
import org.blitzortung.android.protocol.ConsumerContainer
import org.jetbrains.anko.longToast

open class LocationHandler(
        private val context: Context,
        private val backgroundModeHandler: BackgroundModeHandler,
        private val sharedPreferences: SharedPreferences

)
: SharedPreferences.OnSharedPreferenceChangeListener {

    private var backgroundMode = true
    private var provider: LocationProvider? = null
    private val consumerContainer = object : ConsumerContainer<LocationEvent>() {
        override fun addedFirstConsumer() {
            provider?.run {
                if(!isRunning) {
                    start()
                    Log.d(Main.LOG_TAG, "LocationHandler: enable provider")
                }
            }
        }

        override fun removedLastConsumer() {
            provider?.run {
                if(isRunning) {
                    Log.d(Main.LOG_TAG, "LocationHandler: disable provider")
                    shutdown()
                }
            }
        }
    }

    private val backgroundModeConsumer = {backgroundModeEvent: BackgroundModeEvent ->
        backgroundMode = backgroundModeEvent.isInBackground

        updateProvider()
    }

    init {
        sharedPreferences.registerOnSharedPreferenceChangeListener(this)
        onSharedPreferenceChanged(sharedPreferences, PreferenceKey.LOCATION_MODE)

        //We need to know when a LocationProvider is enabled/disabled
        val iFilter = IntentFilter(android.location.LocationManager.PROVIDERS_CHANGED_ACTION)
        context.registerReceiver(LocationProviderChangedReceiver(), iFilter)

        backgroundModeHandler.requestUpdates(backgroundModeConsumer)
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, keyString: String) {
        onSharedPreferenceChanged(sharedPreferences, PreferenceKey.fromString(keyString))
    }

    private fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: PreferenceKey) {
        when (key) {
            PreferenceKey.LOCATION_MODE -> {
                var newProvider = createLocationProvider(context,
                        backgroundMode,
                        { location -> sendLocationUpdate(location) },
                        sharedPreferences.get(key, LocationManager.PASSIVE_PROVIDER)
                        )

                enableProvider(newProvider)
            }
        }
    }

    private fun enableProvider(newProvider: LocationProvider) {
        //If the current provider is not null and is Running, shut it down first
        provider?.let {
            if (it.isRunning) {
                it.shutdown()
            }
        }

        //TODO we need to tell the UI if the locationProvider is stopped/started
        //Now start the new provider if it is enabled
        this.provider = newProvider.apply {
            if (!this.isEnabled) {
                context.longToast(context.resources.getText(R.string.location_provider_disabled).toString().format(newProvider.type))
            } else
                start()
        }
    }

    private fun sendLocationUpdate(location: Location?) {
        sendLocationUpdateToListeners(if (location != null && location.isValid) location else null)
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

    private val Location.isValid: Boolean
        get() = !java.lang.Double.isNaN(longitude) && !java.lang.Double.isNaN(latitude)

    private fun updateProvider() {
        provider?.run {
            if (this is ManagerLocationProvider)
                this.backgroundMode = this@LocationHandler.backgroundMode

            restart()
        }
    }

    fun update(preferences: SharedPreferences) {
        onSharedPreferenceChanged(preferences, PreferenceKey.LOCATION_MODE)
    }

    private inner class LocationProviderChangedReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            //TODO we need to tell the UI if the locationProvider is stopped/started

            provider?.run {
                if (!this.isRunning && this.isEnabled) {
                    start()
                } else if (this.isRunning && !this.isEnabled) {
                    shutdown()
                }
            }
        }
    }

    companion object {
        val MANUAL_PROVIDER = "manual"
    }
}
