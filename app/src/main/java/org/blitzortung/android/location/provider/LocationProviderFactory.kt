package org.blitzortung.android.location.provider

import android.content.Context
import android.location.Location
import android.location.LocationManager
import org.blitzortung.android.app.BOApplication
import org.blitzortung.android.location.LocationHandler
import org.jetbrains.anko.defaultSharedPreferences


internal fun createLocationProvider(context: Context, backgroundMode: Boolean, locationUpdateConsumer: (Location?) -> Unit, providerName: String): LocationProvider {
    val provider = when(providerName) {
        LocationManager.GPS_PROVIDER -> GPSLocationProvider(context, backgroundMode, locationUpdateConsumer)
        LocationManager.NETWORK_PROVIDER -> NetworkLocationProvider(context, backgroundMode, locationUpdateConsumer)
        LocationManager.PASSIVE_PROVIDER -> PassiveLocationProvider(context, backgroundMode, locationUpdateConsumer)
        LocationHandler.MANUAL_PROVIDER -> ManualLocationProvider(locationUpdateConsumer, BOApplication.sharedPreferences)
        else -> null
    } ?: throw IllegalArgumentException("Cannot find provider for name $providerName")

    return provider
}
