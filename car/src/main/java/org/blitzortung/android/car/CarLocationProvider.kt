/*

   Copyright 2026 Andreas Würl

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

package org.blitzortung.android.car

import android.content.Context
import android.location.LocationManager
import androidx.preference.PreferenceManager
import org.blitzortung.android.location.LocationHandler
import org.blitzortung.android.location.provider.ManualLocationProvider
import org.blitzortung.android.preferences.PreferenceKey

/**
 * Chooses the location provider for the car experience.
 *
 * Cars provide a GNSS/GPS fix, while the phone's stored location mode may be
 * network or passive. Android Automotive OS does not provide a network location
 * provider at all, so the car must not depend on the phone preference. GPS is
 * preferred; network is only a fallback for devices without a GPS provider.
 *
 * A configured manual location is an explicit user choice and wins over the
 * device provider, so the car can be pinned to a known position.
 */
internal object CarLocationProvider {
    fun preferred(context: Context): String {
        val manualLocationConfigured = isManualLocationConfigured(context)
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val available = locationManager.allProviders
        val enabled = available.filter { locationManager.isProviderEnabled(it) }
        return preferred(enabled.ifEmpty { available }, manualLocationConfigured)
    }

    private fun isManualLocationConfigured(context: Context): Boolean {
        val preferences = PreferenceManager.getDefaultSharedPreferences(context)
        return preferences.getString(PreferenceKey.LOCATION_MODE.key, null) == LocationHandler.MANUAL_PROVIDER &&
            ManualLocationProvider.getManualLocation(preferences) != null
    }

    internal fun preferred(
        availableProviders: Collection<String>,
        manualLocationConfigured: Boolean = false,
    ): String =
        when {
            manualLocationConfigured -> LocationHandler.MANUAL_PROVIDER
            LocationManager.GPS_PROVIDER in availableProviders -> LocationManager.GPS_PROVIDER
            LocationManager.NETWORK_PROVIDER in availableProviders -> LocationManager.NETWORK_PROVIDER
            else -> LocationManager.PASSIVE_PROVIDER
        }
}
