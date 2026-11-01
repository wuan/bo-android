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

import androidx.car.app.model.Action
import androidx.car.app.model.ListTemplate
import androidx.car.app.navigation.model.MapWithContentTemplate
import androidx.car.app.testing.TestCarContext
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.blitzortung.android.alert.AlertRepository
import org.blitzortung.android.data.Flags
import org.blitzortung.android.data.Parameters
import org.blitzortung.android.data.provider.result.DataReceived
import org.blitzortung.android.data.provider.result.StatusUpdate
import org.blitzortung.android.data.repository.StrikeDataRepository
import org.blitzortung.android.location.LocationRepository
import org.blitzortung.android.map.overlay.color.StrikeColorHandler
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class BlitzortungMapScreenTest {
    private val alertRepository = mockk<AlertRepository>(relaxed = true)

    @After
    fun tearDown() {
        CarDependenciesProvider.dependencies = null
    }

    private fun createScreen(): BlitzortungMapScreen {
        val carContext = TestCarContext.createCarContext(RuntimeEnvironment.getApplication())
        return BlitzortungMapScreen(carContext)
    }

    @Test
    fun `start screen renders a map with content`() {
        val template = createScreen().onGetTemplate()

        assertThat(template).isInstanceOf(MapWithContentTemplate::class.java)
    }

    @Test
    fun `map action strip offers pan and zoom actions`() {
        val template = createScreen().onGetTemplate() as MapWithContentTemplate

        val actionTypes = template.actionStrip?.actions.orEmpty().map { it.type }

        assertThat(actionTypes).contains(Action.TYPE_PAN)
        // Two additional icon-only actions for zooming in and out.
        assertThat(actionTypes).hasSize(3)
    }

    @Test
    fun `forwards a data event to the alert repository and marks the data as loaded`() {
        registerDependencies()
        val screen = createScreen()
        val event = DataReceived(parameters = Parameters(), flags = Flags(), failed = false)

        screen.handleDataEvent(event, activeRenderer = null)

        verify { alertRepository.onDataEvent(event) }
        assertThat(screen.alertListState.hasData).isTrue()
    }

    @Test
    fun `a status update sets the refresh countdown`() {
        registerDependencies()
        val screen = createScreen()

        screen.handleDataEvent(StatusUpdate("40/60"), activeRenderer = null)

        assertThat(screen.alertListState.refreshCountdown).isEqualTo(CarRefreshCountdown(40, 60))
    }

    @Test
    fun `header shows the app name and the compact countdown when a status is reported`() {
        val screen = createScreen()

        screen.handleDataEvent(StatusUpdate("40/60"), activeRenderer = null)

        assertThat(headerTitle(screen)).isEqualTo("Blitzortung Lightning · 40 / 60s")
    }

    @Test
    fun `header shows only the app name before a status is reported`() {
        val screen = createScreen()

        assertThat(headerTitle(screen)).isEqualTo("Blitzortung Lightning")
    }

    private fun headerTitle(screen: BlitzortungMapScreen): String? {
        val mapTemplate = screen.onGetTemplate() as MapWithContentTemplate
        val listTemplate = mapTemplate.contentTemplate as ListTemplate
        return listTemplate.header?.title?.toCharSequence()?.toString()
    }

    private fun registerDependencies() {
        CarDependenciesProvider.dependencies =
            object : CarDependencies {
                override val strikeDataRepository = mockk<StrikeDataRepository>(relaxed = true)
                override val locationRepository = mockk<LocationRepository>(relaxed = true)
                override val alertRepository = this@BlitzortungMapScreenTest.alertRepository
                override val strikeColorHandler = mockk<StrikeColorHandler>(relaxed = true)
            }
    }
}
