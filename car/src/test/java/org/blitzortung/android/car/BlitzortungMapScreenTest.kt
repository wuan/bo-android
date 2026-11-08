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
import android.graphics.Color
import android.os.Looper
import androidx.car.app.model.Action
import androidx.car.app.model.ListTemplate
import androidx.car.app.model.Row
import androidx.car.app.navigation.model.MapWithContentTemplate
import androidx.car.app.testing.TestCarContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleRegistry
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.within
import org.blitzortung.android.alert.AlertParameters
import org.blitzortung.android.alert.AlertRepository
import org.blitzortung.android.alert.LocalActivity
import org.blitzortung.android.alert.NoData
import org.blitzortung.android.alert.data.AlertSector
import org.blitzortung.android.alert.data.AlertSectorRange
import org.blitzortung.android.data.Flags
import org.blitzortung.android.data.Parameters
import org.blitzortung.android.data.provider.result.DataReceived
import org.blitzortung.android.data.provider.result.StatusUpdate
import org.blitzortung.android.data.repository.StrikeDataRepository
import org.blitzortung.android.location.LocationRepository
import org.blitzortung.android.map.overlay.color.StrikeColorHandler
import org.blitzortung.android.util.MeasurementSystem
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
class BlitzortungMapScreenTest {
    private val alertRepository = mockk<AlertRepository>(relaxed = true)

    private val strikeDataRepository = mockk<StrikeDataRepository>(relaxed = true)

    private val carSettings by lazy { CarSettings(RuntimeEnvironment.getApplication()) }

    @Before
    fun grantLocationPermission() {
        shadowOf(RuntimeEnvironment.getApplication())
            .grantPermissions(Manifest.permission.ACCESS_FINE_LOCATION)
    }

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
    fun `map action strip offers pan, location and zoom actions`() {
        val screen = createScreen()
        val template = screen.onGetTemplate() as MapWithContentTemplate

        val actions = template.mapController?.mapActionStrip?.actions.orEmpty()

        assertThat(actions.map { it.type })
            .containsExactly(Action.TYPE_PAN, Action.TYPE_CUSTOM, Action.TYPE_CUSTOM, Action.TYPE_CUSTOM)
        // The location action sits directly above the zoom out/in actions.
        assertThat(actions[1]).isSameAs(screen.locationAction)
    }

    @Test
    fun `map controls are not placed in the content action strip`() {
        val template = createScreen().onGetTemplate() as MapWithContentTemplate

        val actionTypes = template.actionStrip?.actions.orEmpty().map { it.type }

        assertThat(actionTypes).doesNotContain(Action.TYPE_PAN)
        assertThat(actionTypes).doesNotContain(Action.TYPE_APP_ICON)
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
    fun `status line shows only the compact countdown when a status is reported`() {
        val screen = createScreen()

        screen.handleDataEvent(StatusUpdate("40/60"), activeRenderer = null)

        assertThat(screen.buildCountdownText()).isEqualTo("40 / 60s")
    }

    @Test
    fun `status line is empty before a status is reported`() {
        val screen = createScreen()

        assertThat(screen.buildCountdownText()).isEmpty()
    }

    @Test
    fun `countdown is drawn on the map surface instead of the host header`() {
        registerDependencies()
        val screen = createScreen()
        val renderer = screen.renderer as OsmdroidCarMapRenderer

        screen.handleDataEvent(StatusUpdate("40/60"))

        assertThat(renderer.legendOverlay.countdown).isEqualTo("40 / 60s")
        assertThat(headerTitle(screen)).isNull()
    }

    @Test
    fun `alert list shows only the closest activity as a single status line`() {
        val screen = createScreen()
        screen.alertListState =
            CarAlertListState(
                warning =
                    LocalActivity(
                        sectors =
                            listOf(
                                AlertSector(
                                    label = "NE",
                                    minimumSectorBearing = 45f,
                                    maximumSectorBearing = 90f,
                                    ranges = listOf(AlertSectorRange(0f, 10f, 3, 0L)),
                                    closestStrikeDistance = 8f,
                                ),
                            ),
                        parameters = alertParameters(),
                        referenceTime = 0L,
                    ),
                hasData = true,
            )

        val listTemplate = (screen.onGetTemplate() as MapWithContentTemplate).contentTemplate as ListTemplate
        val rows =
            listTemplate
                .singleList
                ?.items
                ?.map { it as Row }
                .orEmpty()

        assertThat(rows.map { it.title?.toCharSequence()?.toString() })
            .containsExactly("Closest strike: 8km NE")
        assertThat(rows).allSatisfy { assertThat(it.texts).isEmpty() }
    }

    @Test
    fun `the content template has no header so the countdown stays small`() {
        val screen = createScreen()

        val listTemplate = (screen.onGetTemplate() as MapWithContentTemplate).contentTemplate as ListTemplate

        assertThat(listTemplate.header).isNull()
    }

    @Test
    fun `the content action strip exposes the settings action`() {
        val screen = createScreen()

        val template = screen.onGetTemplate() as MapWithContentTemplate

        assertThat(template.actionStrip?.actions).hasSize(1)
    }

    @Test
    fun `follow car shades dark when the car reports dark mode`() {
        registerDependencies()
        carSettings.mapShadingMode = MapShadingMode.FOLLOW_CAR
        RuntimeEnvironment.setQualifiers("+night")

        assertThat(createScreen().currentShading()).isEqualTo(MapShading.DARK)
    }

    @Test
    fun `follow car shades bright when the car reports light mode`() {
        registerDependencies()
        carSettings.mapShadingMode = MapShadingMode.FOLLOW_CAR
        RuntimeEnvironment.setQualifiers("+notnight")

        assertThat(createScreen().currentShading()).isEqualTo(MapShading.BRIGHT)
    }

    @Test
    fun `dark map shades dark regardless of the car theme`() {
        registerDependencies()
        carSettings.mapShadingMode = MapShadingMode.DARK
        RuntimeEnvironment.setQualifiers("+notnight")

        assertThat(createScreen().currentShading()).isEqualTo(MapShading.DARK)
    }

    @Test
    fun `bright map shades bright regardless of the car theme`() {
        registerDependencies()
        carSettings.mapShadingMode = MapShadingMode.BRIGHT
        RuntimeEnvironment.setQualifiers("+night")

        assertThat(createScreen().currentShading()).isEqualTo(MapShading.BRIGHT)
    }

    @Test
    fun `the shading is applied to the renderer when the template is built`() {
        registerDependencies()
        carSettings.mapShadingMode = MapShadingMode.DARK
        val screen = createScreen()
        val renderer = screen.renderer as OsmdroidCarMapRenderer

        screen.onGetTemplate()

        assertThat(renderer.shadeOverlay.color).isEqualTo(Color.BLACK)
        assertThat(renderer.shadeOverlay.alpha).isGreaterThan(0)
    }

    @Test
    fun `a shading change is applied to the map on the next template`() {
        registerDependencies()
        val screen = createScreen()
        val renderer = screen.renderer as OsmdroidCarMapRenderer

        carSettings.mapShadingMode = MapShadingMode.BRIGHT
        screen.onGetTemplate()

        assertThat(renderer.shadeOverlay.color).isEqualTo(Color.WHITE)
        assertThat(renderer.shadeOverlay.alpha).isGreaterThan(0)
    }

    @Test
    fun `a renderer camera change reaches the repository`() {
        registerDependencies()
        val screen = createScreen()
        val renderer = screen.renderer as OsmdroidCarMapRenderer

        renderer.mapView.layout(0, 0, VIEWPORT_WIDTH, VIEWPORT_HEIGHT)

        verify { strikeDataRepository.updateViewport(any()) }
    }

    @Test
    fun `no data request is issued before the first viewport update`() {
        registerDependencies()
        val screen = createScreen()

        startScreen(screen)

        verify(exactly = 0) { strikeDataRepository.updateData() }
    }

    @Test
    fun `the first viewport update issues a data request`() {
        registerDependencies()
        val screen = createScreen()
        startScreen(screen)

        layoutScreen(screen)

        verify(exactly = 1) { strikeDataRepository.updateData() }
    }

    @Test
    fun `returning from another screen resumes data updates`() {
        registerDependencies()
        val screen = createScreen()
        startScreen(screen)
        layoutScreen(screen)
        verify(exactly = 1) { strikeDataRepository.start() }

        stopScreen(screen)
        startScreen(screen)

        verify(exactly = 1) { strikeDataRepository.stop() }
        verify(exactly = 2) { strikeDataRepository.start() }
        verify(exactly = 2) { strikeDataRepository.updateData() }
    }

    @Test
    fun `the location action frames the outermost alert range`() {
        registerDependencies()
        every { alertRepository.getAlertParameters() } returns alertParameters()
        val screen = createScreen()
        screen.alertListState = screen.alertListState.copy(warning = NoData)

        assertThat(screen.locationTargetSpanMeters()).isEqualTo(500_000f)
    }

    @Test
    fun `the location action frames the closest strike with the shared margin`() {
        registerDependencies()
        every { alertRepository.getAlertParameters() } returns alertParameters()
        val screen = createScreen()
        screen.alertListState =
            screen.alertListState.copy(
                warning =
                    LocalActivity(
                        sectors = listOf(AlertSector("N", 0f, 45f, emptyList(), 100f)),
                        parameters = alertParameters(),
                        referenceTime = 0L,
                    ),
            )

        assertThat(screen.locationTargetSpanMeters()).isCloseTo(200_000f, within(0.01f))
    }

    @Test
    fun `the location action has no span without alert parameters`() {
        val screen = createScreen()

        assertThat(screen.locationTargetSpanMeters()).isNull()
    }

    private fun startScreen(screen: BlitzortungMapScreen) {
        (screen.lifecycle as LifecycleRegistry).handleLifecycleEvent(Lifecycle.Event.ON_START)
        shadowOf(Looper.getMainLooper()).idle()
    }

    private fun stopScreen(screen: BlitzortungMapScreen) {
        (screen.lifecycle as LifecycleRegistry).handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        shadowOf(Looper.getMainLooper()).idle()
    }

    private fun layoutScreen(screen: BlitzortungMapScreen) {
        val renderer = screen.renderer as OsmdroidCarMapRenderer
        renderer.mapView.layout(0, 0, VIEWPORT_WIDTH, VIEWPORT_HEIGHT)
    }

    private fun headerTitle(screen: BlitzortungMapScreen): String? {
        val mapTemplate = screen.onGetTemplate() as MapWithContentTemplate
        val listTemplate = mapTemplate.contentTemplate as ListTemplate
        return listTemplate.header?.title?.toCharSequence()?.toString()
    }

    private fun registerDependencies() {
        CarDependenciesProvider.dependencies =
            object : CarDependencies {
                override val strikeDataRepository = this@BlitzortungMapScreenTest.strikeDataRepository
                override val locationRepository = mockk<LocationRepository>(relaxed = true)
                override val alertRepository = this@BlitzortungMapScreenTest.alertRepository
                override val strikeColorHandler = mockk<StrikeColorHandler>(relaxed = true)
                override val carSettings = this@BlitzortungMapScreenTest.carSettings
            }
    }

    private fun alertParameters() =
        AlertParameters(
            alarmInterval = 600_000L,
            rangeSteps = listOf(10f, 25f, 50f, 100f, 250f, 500f),
            sectorLabels = listOf("N", "E", "S", "W"),
            measurementSystem = MeasurementSystem.METRIC,
        )

    private companion object {
        private const val VIEWPORT_WIDTH = 800
        private const val VIEWPORT_HEIGHT = 480
    }
}
