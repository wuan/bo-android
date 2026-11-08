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
import org.assertj.core.api.Assertions.assertThat
import org.blitzortung.android.location.LocationHandler
import org.blitzortung.android.preferences.PreferenceKey
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class CarLocationProviderTest {
    private val context: Context = RuntimeEnvironment.getApplication()

    @Before
    fun clearLocationPreferences() {
        PreferenceManager
            .getDefaultSharedPreferences(context)
            .edit()
            .clear()
            .commit()
    }

    @Test
    fun `prefers the gps provider`() {
        val provider =
            CarLocationProvider.preferred(
                listOf(
                    LocationManager.PASSIVE_PROVIDER,
                    LocationManager.NETWORK_PROVIDER,
                    LocationManager.GPS_PROVIDER,
                ),
            )

        assertThat(provider).isEqualTo(LocationManager.GPS_PROVIDER)
    }

    @Test
    fun `falls back to network when there is no gps provider`() {
        val provider =
            CarLocationProvider.preferred(
                listOf(LocationManager.PASSIVE_PROVIDER, LocationManager.NETWORK_PROVIDER),
            )

        assertThat(provider).isEqualTo(LocationManager.NETWORK_PROVIDER)
    }

    @Test
    fun `falls back to passive when neither gps nor network is available`() {
        val provider = CarLocationProvider.preferred(listOf(LocationManager.PASSIVE_PROVIDER))

        assertThat(provider).isEqualTo(LocationManager.PASSIVE_PROVIDER)
    }

    @Test
    fun `a configured manual location wins over gps`() {
        storeManualLocation(latitude = "48.0", longitude = "11.0")

        assertThat(CarLocationProvider.preferred(context))
            .isEqualTo(LocationHandler.MANUAL_PROVIDER)
    }

    @Test
    fun `a configured manual location wins over an available gps provider`() {
        val provider =
            CarLocationProvider.preferred(
                listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER),
                manualLocationConfigured = true,
            )

        assertThat(provider).isEqualTo(LocationHandler.MANUAL_PROVIDER)
    }

    @Test
    fun `manual mode without coordinates falls back to a device provider`() {
        PreferenceManager
            .getDefaultSharedPreferences(context)
            .edit()
            .putString(PreferenceKey.LOCATION_MODE.key, LocationHandler.MANUAL_PROVIDER)
            .commit()

        assertThat(CarLocationProvider.preferred(context))
            .isNotEqualTo(LocationHandler.MANUAL_PROVIDER)
    }

    private fun storeManualLocation(
        latitude: String,
        longitude: String,
    ) {
        PreferenceManager
            .getDefaultSharedPreferences(context)
            .edit()
            .putString(PreferenceKey.LOCATION_MODE.key, LocationHandler.MANUAL_PROVIDER)
            .putString(PreferenceKey.LOCATION_LATITUDE.key, latitude)
            .putString(PreferenceKey.LOCATION_LONGITUDE.key, longitude)
            .commit()
    }
}
