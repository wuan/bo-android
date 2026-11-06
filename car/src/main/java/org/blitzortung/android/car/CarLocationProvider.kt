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

/**
 * Chooses the location provider for the car experience.
 *
 * Cars provide a GNSS/GPS fix, while the phone's stored location mode may be
 * network or passive. Android Automotive OS does not provide a network location
 * provider at all, so the car must not depend on the phone preference. GPS is
 * preferred; network is only a fallback for devices without a GPS provider.
 */
internal object CarLocationProvider {
    fun preferred(context: Context): String {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val available = locationManager.allProviders
        val enabled = available.filter { locationManager.isProviderEnabled(it) }
        return preferred(enabled.ifEmpty { available })
    }

    internal fun preferred(availableProviders: Collection<String>): String =
        when {
            LocationManager.GPS_PROVIDER in availableProviders -> LocationManager.GPS_PROVIDER
            LocationManager.NETWORK_PROVIDER in availableProviders -> LocationManager.NETWORK_PROVIDER
            else -> LocationManager.PASSIVE_PROVIDER
        }
}
