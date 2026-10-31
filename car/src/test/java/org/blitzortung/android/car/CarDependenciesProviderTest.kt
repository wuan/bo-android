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

import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.blitzortung.android.alert.AlertRepository
import org.blitzortung.android.data.repository.StrikeDataRepository
import org.blitzortung.android.location.LocationRepository
import org.blitzortung.android.map.overlay.color.StrikeColorHandler
import org.junit.After
import org.junit.Test

class CarDependenciesProviderTest {
    @After
    fun tearDown() {
        CarDependenciesProvider.dependencies = null
    }

    @Test
    fun `dependencies registered by the host are resolved through the provider`() {
        val strikeRepo = mockk<StrikeDataRepository>()
        val locationRepo = mockk<LocationRepository>()
        val alertRepo = mockk<AlertRepository>()
        val colorHandler = mockk<StrikeColorHandler>()

        CarDependenciesProvider.dependencies =
            object : CarDependencies {
                override val strikeDataRepository = strikeRepo
                override val locationRepository = locationRepo
                override val alertRepository = alertRepo
                override val strikeColorHandler = colorHandler
            }

        val resolved = CarDependenciesProvider.dependencies

        assertThat(resolved?.strikeDataRepository).isSameAs(strikeRepo)
        assertThat(resolved?.locationRepository).isSameAs(locationRepo)
        assertThat(resolved?.alertRepository).isSameAs(alertRepo)
        assertThat(resolved?.strikeColorHandler).isSameAs(colorHandler)
    }
}
