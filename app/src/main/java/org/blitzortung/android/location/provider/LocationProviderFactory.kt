package org.blitzortung.android.location.provider

import android.content.Context
import android.location.Location
import android.location.LocationManager
import org.blitzortung.android.app.BOApplication
import org.blitzortung.android.location.LocationHandler


internal fun createLocationProvider(context: Context, backgroundMode: Boolean, updateConsumer: (Location?) -> Unit, providerName: String): LocationProvider {

    return when(providerName) {
        LocationManager.GPS_PROVIDER -> GPSLocationProvider(context, backgroundMode, updateConsumer)
        LocationManager.NETWORK_PROVIDER -> NetworkLocationProvider(context, backgroundMode, updateConsumer)
        LocationManager.PASSIVE_PROVIDER -> PassiveLocationProvider(context, backgroundMode, updateConsumer)
        LocationHandler.MANUAL_PROVIDER -> ManualLocationProvider(updateConsumer, BOApplication.sharedPreferences)
        else -> null
    } ?: throw IllegalArgumentException("Cannot find provider for name $providerName")
}
