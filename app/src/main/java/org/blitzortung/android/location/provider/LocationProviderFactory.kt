package org.blitzortung.android.location.provider

import android.content.Context
import android.location.Location
import android.location.LocationManager.GPS_PROVIDER
import android.location.LocationManager.NETWORK_PROVIDER
import android.location.LocationManager.PASSIVE_PROVIDER
import androidx.preference.PreferenceManager
import org.blitzortung.android.location.LocationHandler

internal fun createLocationProvider(
    context: Context,
    backgroundMode: Boolean,
    updateConsumer: (Location?) -> Unit,
    providerName: String,
): LocationProvider {
    return when (providerName) {
        GPS_PROVIDER -> GPSLocationProvider(context, backgroundMode, updateConsumer)
        NETWORK_PROVIDER -> NetworkLocationProvider(context, backgroundMode, updateConsumer)
        PASSIVE_PROVIDER -> PassiveLocationProvider(context, backgroundMode, updateConsumer)
        LocationHandler.MANUAL_PROVIDER ->
            ManualLocationProvider(
                updateConsumer,
                PreferenceManager.getDefaultSharedPreferences(context),
            )

        else -> null
    } ?: throw IllegalArgumentException("Cannot find provider for name $providerName")
}
