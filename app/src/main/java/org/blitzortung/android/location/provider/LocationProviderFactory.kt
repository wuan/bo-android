package org.blitzortung.android.location.provider

import android.content.Context
import android.location.Location
import android.location.LocationManager
import org.blitzortung.android.location.LocationHandler
import org.jetbrains.anko.defaultSharedPreferences


internal fun createLocationProvider(context: Context, backgroundMode: Boolean, tmp: (Location?) -> Unit, providerName: String): LocationProvider {
    val provider = when(providerName) {
        LocationManager.GPS_PROVIDER -> GPSLocationProvider(context, backgroundMode, tmp)
        LocationManager.NETWORK_PROVIDER -> NetworkLocationProvider(context, backgroundMode, tmp)
        LocationManager.PASSIVE_PROVIDER -> PassiveLocationProvider(context, backgroundMode, tmp)
        LocationHandler.MANUAL_PROVIDER -> ManualLocationProvider(tmp, context.defaultSharedPreferences)
        else -> null
    } ?: throw IllegalArgumentException("Cannot find provider for name $providerName")

    return provider
}
