package org.blitzortung.android.location.provider

import android.content.Context
import android.location.Location
import android.location.LocationManager.*
import org.blitzortung.android.location.LocationHandler
import org.jetbrains.anko.defaultSharedPreferences


internal fun createLocationProvider(context: Context, backgroundMode: Boolean, updateConsumer: (Location?) -> Unit, providerName: String): LocationProvider {

    return when (providerName) {
        GPS_PROVIDER -> GPSLocationProvider(context, backgroundMode, updateConsumer)
        NETWORK_PROVIDER -> NetworkLocationProvider(context, backgroundMode, updateConsumer)
        PASSIVE_PROVIDER -> PassiveLocationProvider(context, backgroundMode, updateConsumer)
        LocationHandler.MANUAL_PROVIDER -> ManualLocationProvider(updateConsumer, context.defaultSharedPreferences)
        else -> null
    } ?: throw IllegalArgumentException("Cannot find provider for name $providerName")
}
