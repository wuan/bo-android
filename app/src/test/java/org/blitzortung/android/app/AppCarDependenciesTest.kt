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

package org.blitzortung.android.app

import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.blitzortung.android.alert.AlertRepository
import org.blitzortung.android.data.repository.StrikeDataRepository
import org.blitzortung.android.location.LocationRepository
import org.blitzortung.android.map.overlay.color.ColorTarget
import org.blitzortung.android.map.overlay.color.StrikeColorHandler
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class AppCarDependenciesTest {
    @Test
    fun `the car uses its own strike colour handler`() {
        val context = RuntimeEnvironment.getApplication()
        val preferences = PreferenceManager.getDefaultSharedPreferences(context)
        val phoneHandler = StrikeColorHandler(preferences)

        val dependencies = createDependencies(preferences)

        assertThat(dependencies.strikeColorHandler).isNotSameAs(phoneHandler)
    }

    @Test
    fun `car shading leaves the phone colour target unchanged`() {
        val context = RuntimeEnvironment.getApplication()
        val preferences = PreferenceManager.getDefaultSharedPreferences(context)
        val phoneHandler = StrikeColorHandler(preferences)
        val phonePalette = phoneHandler.colors
        val dependencies = createDependencies(preferences)

        dependencies.strikeColorHandler.targetOverride = ColorTarget.STREETMAP
        dependencies.strikeColorHandler.updateTarget()
        phoneHandler.updateTarget()

        assertThat(phoneHandler.targetOverride).isNull()
        assertThat(phoneHandler.colors).isEqualTo(phonePalette)
    }

    private fun createDependencies(preferences: SharedPreferences) =
        AppCarDependencies(
            strikeDataRepository = mockk<StrikeDataRepository>(relaxed = true),
            locationRepository = mockk<LocationRepository>(relaxed = true),
            alertRepository = mockk<AlertRepository>(relaxed = true),
            preferences = preferences,
            context = RuntimeEnvironment.getApplication(),
        )
}
