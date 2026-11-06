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

import androidx.car.app.Screen
import androidx.car.app.model.Action
import androidx.car.app.model.Header
import androidx.car.app.model.ListTemplate
import androidx.car.app.model.Row
import androidx.car.app.testing.TestCarContext
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.blitzortung.android.alert.AlertRepository
import org.blitzortung.android.data.repository.StrikeDataRepository
import org.blitzortung.android.location.LocationRepository
import org.blitzortung.android.map.overlay.color.StrikeColorHandler
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class BlitzortungSettingsScreenTest {
    private val carSettings by lazy { CarSettings(RuntimeEnvironment.getApplication()) }

    @After
    fun tearDown() {
        CarDependenciesProvider.dependencies = null
    }

    private fun carContext() = TestCarContext.createCarContext(RuntimeEnvironment.getApplication())

    private fun registerDependencies() {
        CarDependenciesProvider.dependencies =
            object : CarDependencies {
                override val strikeDataRepository = mockk<StrikeDataRepository>(relaxed = true)
                override val locationRepository = mockk<LocationRepository>(relaxed = true)
                override val alertRepository = mockk<AlertRepository>(relaxed = true)
                override val strikeColorHandler = mockk<StrikeColorHandler>(relaxed = true)
                override val carSettings = this@BlitzortungSettingsScreenTest.carSettings
            }
    }

    @Test
    fun `settings list shows the map shading setting with its current value`() {
        registerDependencies()
        carSettings.mapShadingMode = MapShadingMode.DARK
        val screen = BlitzortungSettingsScreen(carContext())

        val rows = rowsOf(screen)

        assertThat(rows).hasSize(1)
        assertThat(rows.first().title?.toString()).isEqualTo("Map shading")
        assertThat(rows.first().texts.map { it.toString() }).contains("Dark map")
    }

    @Test
    fun `the shading choice screen offers all three modes`() {
        registerDependencies()
        val screen = BlitzortungMapShadingSettingScreen(carContext())

        val titles = rowsOf(screen).map { it.title?.toString() }

        assertThat(titles).containsExactly("Follow car", "Dark map", "Bright map")
    }

    @Test
    fun `selecting a mode stores it`() {
        registerDependencies()
        val screen = BlitzortungMapShadingSettingScreen(carContext())

        screen.selectMode(MapShadingMode.BRIGHT)

        assertThat(carSettings.mapShadingMode).isEqualTo(MapShadingMode.BRIGHT)
    }

    @Test
    fun `the settings header offers a back action`() {
        registerDependencies()
        val screen = BlitzortungSettingsScreen(carContext())

        assertThat(headerOf(screen).startHeaderAction).isEqualTo(Action.BACK)
    }

    @Test
    fun `the shading choice header offers a back action`() {
        registerDependencies()
        val screen = BlitzortungMapShadingSettingScreen(carContext())

        assertThat(headerOf(screen).startHeaderAction).isEqualTo(Action.BACK)
    }

    private fun rowsOf(screen: Screen): List<Row> {
        val template = screen.onGetTemplate() as ListTemplate
        return template.singleList?.items.orEmpty().filterIsInstance<Row>()
    }

    private fun headerOf(screen: Screen): Header {
        val template = screen.onGetTemplate() as ListTemplate
        return template.header!!
    }
}
