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

import android.location.LocationManager
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class CarLocationProviderTest {
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
}
