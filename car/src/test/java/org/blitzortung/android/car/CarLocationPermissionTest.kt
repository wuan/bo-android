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

import android.Manifest
import androidx.car.app.testing.TestCarContext
import androidx.preference.PreferenceManager
import org.assertj.core.api.Assertions.assertThat
import org.blitzortung.android.location.LocationHandler
import org.blitzortung.android.location.provider.ManualLocationProvider
import org.blitzortung.android.preferences.PreferenceKey
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class CarLocationPermissionTest {
    private val context = RuntimeEnvironment.getApplication()
    private val carContext = TestCarContext.createCarContext(context)

    @Test
    fun `requests fine and coarse location from the car host`() {
        CarLocationPermission(carContext).request { }

        val request = carContext.lastPermissionRequestInfo

        assertThat(request?.permissionsRequested)
            .containsExactlyInAnyOrder(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
            )
    }

    @Test
    fun `reports denied without crashing when the host rejects the request`() {
        var result: Boolean? = null
        CarLocationPermission(carContext).request { granted -> result = granted }

        carContext.lastPermissionRequestInfo?.listener?.onRequestPermissionsResult(
            emptyList(),
            CarLocationPermission.LOCATION_PERMISSIONS,
        )

        assertThat(result).isFalse()
    }

    @Test
    fun `configured manual location is available as the fallback`() {
        val preferences = PreferenceManager.getDefaultSharedPreferences(context)
        preferences
            .edit()
            .putString(PreferenceKey.LOCATION_MODE.key, LocationHandler.MANUAL_PROVIDER)
            .putString(PreferenceKey.LOCATION_LATITUDE.key, "48.0")
            .putString(PreferenceKey.LOCATION_LONGITUDE.key, "11.0")
            .apply()

        val fallback = ManualLocationProvider.getManualLocation(preferences)

        assertThat(fallback?.latitude).isEqualTo(48.0)
        assertThat(fallback?.longitude).isEqualTo(11.0)
    }
}
