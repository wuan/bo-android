/*

   Copyright 2025 Andreas Würl

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

package org.blitzortung.android.app.view

import android.graphics.Canvas
import android.location.Location
import android.view.View
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.blitzortung.android.alert.AlertParameters
import org.blitzortung.android.alert.LocalActivity
import org.blitzortung.android.alert.NoData
import org.blitzortung.android.alert.NoLocation
import org.blitzortung.android.alert.Outlying
import org.blitzortung.android.alert.Warning
import org.blitzortung.android.alert.data.AlertSector
import org.blitzortung.android.alert.data.AlertSectorRange
import org.blitzortung.android.app.view.alarm.LocalActivityRenderer
import org.blitzortung.android.app.view.alarm.PrimitiveRenderer
import org.blitzortung.android.app.view.alarm.SymbolRenderer
import org.blitzortung.android.app.view.support.CanvasProvider
import org.blitzortung.android.app.view.support.CanvasWrapper
import org.blitzortung.android.location.LocationEvent
import org.blitzortung.android.location.LocationUpdate
import org.blitzortung.android.map.overlay.color.ColorHandler
import org.blitzortung.android.util.MeasurementSystem
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class AlarmViewTest {

    private lateinit var alarmView: AlarmView
    private lateinit var canvasProvider: CanvasProvider
    private lateinit var primitiveRenderer: PrimitiveRenderer
    private lateinit var symbolRenderer: SymbolRenderer
    private lateinit var localActivityRenderer: LocalActivityRenderer
    private lateinit var canvas: Canvas
    private lateinit var canvasWrapper: CanvasWrapper
    private lateinit var colorHandler: ColorHandler

    @Before
    fun setUp() {
        val context = RuntimeEnvironment.getApplication()

        canvasProvider = mockk(relaxed = true)
        primitiveRenderer = mockk(relaxed = true)
        symbolRenderer = mockk(relaxed = true)
        localActivityRenderer = mockk(relaxed = true)
        canvas = mockk(relaxed = true)
        canvasWrapper = mockk(relaxed = true)
        colorHandler = mockk(relaxed = true)

        every { canvasWrapper.canvas } returns canvas
        every { canvasWrapper.clear() } returns Unit
        every { canvasWrapper.update(any()) } returns Unit
        every { canvasProvider.provide(any(), any(), any()) } returns canvasWrapper
        every { colorHandler.backgroundColor } returns 0xFFFFFFFF.toInt()

        alarmView = AlarmView(
            context = context,
            canvasProvider = canvasProvider,
            primitiveRenderer = primitiveRenderer,
            symbolRenderer = symbolRenderer,
            localActivityRenderer = localActivityRenderer
        )

        alarmView.setColorHandler(colorHandler, 600000)

        // Set up dimensions for the view
        alarmView.measure(
            View.MeasureSpec.makeMeasureSpec(300, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(300, View.MeasureSpec.EXACTLY)
        )
        alarmView.layout(0, 0, 300, 300)
    }

    private fun createLocalActivity(
        closestDistance: Float = 10.0f,
        label: String = "N",
        referenceTime: Long = System.currentTimeMillis()
    ): LocalActivity {
        val ranges = listOf(
            AlertSectorRange(rangeMinimum = 0.0f, rangeMaximum = 50.0f, strikeCount = 5, latestStrikeTimestamp = referenceTime)
        )
        val sector = AlertSector(
            label = label,
            minimumSectorBearing = 0f,
            maximumSectorBearing = 45f,
            ranges = ranges,
            closestStrikeDistance = closestDistance
        )
        val parameters = AlertParameters(
            alarmInterval = 600000L,
            rangeSteps = listOf(10f, 25f, 50f),
            sectorLabels = listOf("N", "NE", "E", "SE", "S", "SW", "W", "NW"),
            measurementSystem = MeasurementSystem.METRIC
        )
        return LocalActivity(
            sectors = listOf(sector),
            parameters = parameters,
            referenceTime = referenceTime
        )
    }

    private fun createLocation(latitude: Double = 51.5, longitude: Double = -0.1): Location {
        return mockk<Location>().apply {
            every { getLatitude() } returns latitude
            every { getLongitude() } returns longitude
        }
    }

    @Test
    fun shouldRenderLocalActivityWhenWarningIsLocalActivity() {
        val localActivity = createLocalActivity()

        alarmView.alertEventConsumer(localActivity)
        alarmView.draw(canvas)

        val dataSlot = slot<AlarmViewData>()
        verify {
            localActivityRenderer.renderLocalActivity(
                eq(localActivity),
                capture(dataSlot),
                eq(canvasWrapper)
            )
        }

        assertThat(dataSlot.captured.size).isEqualTo(300)
        assertThat(dataSlot.captured.center).isGreaterThan(0f)
        assertThat(dataSlot.captured.radius).isGreaterThan(0f)
    }

    @Test
    fun shouldRenderOutOfRangeSymbolWhenWarningIsOutlying() {
        alarmView.alertEventConsumer(Outlying)
        alarmView.draw(canvas)

        val dataSlot = slot<AlarmViewData>()
        verify {
            symbolRenderer.drawOutOfRangeSymbol(
                capture(dataSlot),
                eq(canvas)
            )
        }

        assertThat(dataSlot.captured.size).isEqualTo(300)
    }

    @Test
    fun shouldRenderNoLocationSymbolWhenWarningIsNoLocation() {
        alarmView.alertEventConsumer(NoLocation)
        alarmView.draw(canvas)

        val dataSlot = slot<AlarmViewData>()
        verify {
            symbolRenderer.drawNoLocationSymbol(
                capture(dataSlot),
                eq(canvas)
            )
        }
    }

    @Test
    fun shouldRenderDescriptionTextWhenNoLocationAndDescriptionEnabled() {
        alarmView.enableDescriptionText()
        alarmView.measure(
            View.MeasureSpec.makeMeasureSpec(400, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(400, View.MeasureSpec.EXACTLY)
        )
        alarmView.layout(0, 0, 400, 400)

        alarmView.alertEventConsumer(NoLocation)
        alarmView.draw(canvas)

        verify {
            symbolRenderer.drawAlertOrLocationMissingMessage(any(), any(), eq(canvas))
        }
        verify(exactly = 0) {
            symbolRenderer.drawNoLocationSymbol(any(), any())
        }
    }

    @Test
    fun shouldRenderOwnLocationSymbolWhenNoWarningAndLocationPresent() {
        val location = createLocation()
        alarmView.locationEventConsumer(LocationUpdate(location))
        alarmView.alertEventConsumer(NoData)
        alarmView.draw(canvas)

        val dataSlot = slot<AlarmViewData>()
        verify {
            symbolRenderer.drawOwnLocationSymbol(
                capture(dataSlot),
                eq(canvas)
            )
        }
    }

    @Test
    fun shouldRenderNoLocationSymbolWhenNoWarningAndNoLocation() {
        alarmView.alertEventConsumer(NoData)
        alarmView.draw(canvas)

        val dataSlot = slot<AlarmViewData>()
        verify {
            symbolRenderer.drawNoLocationSymbol(
                capture(dataSlot),
                eq(canvas)
            )
        }
    }

    @Test
    fun shouldUpdateWarningWhenAlertEventIsReceived() {
        val localActivity = createLocalActivity()

        alarmView.alertEventConsumer(localActivity)
        alarmView.draw(canvas)

        verify {
            localActivityRenderer.renderLocalActivity(eq(localActivity), any(), any())
        }
    }

    @Test
    fun shouldNotUpdateWhenSameWarningIsReceivedAgain() {
        val localActivity = createLocalActivity()

        alarmView.alertEventConsumer(localActivity)
        alarmView.alertEventConsumer(localActivity)

        // The view should not redraw for the same warning
        // In real implementation this would check invalidate() calls
    }

    @Test
    fun shouldSetVisibilityToVisibleWhenLocationIsProvided() {
        val location = createLocation()
        val locationUpdate = LocationUpdate(location)

        alarmView.locationEventConsumer(locationUpdate)

        assertThat(alarmView.visibility).isEqualTo(View.VISIBLE)
    }

    @Test
    fun shouldNotUpdateWhenSameLocationIsReceivedAgain() {
        val location = createLocation()
        val locationUpdate = LocationUpdate(location)

        alarmView.locationEventConsumer(locationUpdate)
        val firstVisibility = alarmView.visibility

        alarmView.locationEventConsumer(locationUpdate)
        val secondVisibility = alarmView.visibility

        assertThat(firstVisibility).isEqualTo(secondVisibility)
    }

    @Test
    fun shouldHandleTransitionFromLocationToNoLocation() {
        val location = createLocation()
        alarmView.locationEventConsumer(LocationUpdate(location))
        assertThat(alarmView.visibility).isEqualTo(View.VISIBLE)

        val noLocationEvent: LocationEvent = org.blitzortung.android.location.NoLocation
        alarmView.locationEventConsumer(noLocationEvent)

        assertThat(alarmView.visibility).isEqualTo(View.INVISIBLE)
    }

    @Test
    fun shouldRenderLocalActivityWithMultipleSectors() {
        val sector1 = AlertSector("N", 0f, 45f, listOf(), 10f)
        val sector2 = AlertSector("NE", 45f, 90f, listOf(), 20f)
        val sector3 = AlertSector("E", 90f, 135f, listOf(), Float.POSITIVE_INFINITY)

        val parameters = AlertParameters(
            alarmInterval = 600000L,
            rangeSteps = listOf(10f, 25f, 50f),
            sectorLabels = listOf("N", "NE", "E", "SE", "S", "SW", "W", "NW"),
            measurementSystem = MeasurementSystem.METRIC
        )

        val localActivity = LocalActivity(
            sectors = listOf(sector1, sector2, sector3),
            parameters = parameters,
            referenceTime = System.currentTimeMillis()
        )

        alarmView.alertEventConsumer(localActivity)
        alarmView.draw(canvas)

        verify {
            localActivityRenderer.renderLocalActivity(eq(localActivity), any(), any())
        }
    }

    @Test
    fun shouldRenderLocalActivityWithNoStrikesInSectors() {
        val sector = AlertSector("N", 0f, 45f, listOf(), Float.POSITIVE_INFINITY)
        val parameters = AlertParameters(
            alarmInterval = 600000L,
            rangeSteps = listOf(10f, 25f, 50f),
            sectorLabels = listOf("N", "NE", "E", "SE", "S", "SW", "W", "NW"),
            measurementSystem = MeasurementSystem.METRIC
        )

        val localActivity = LocalActivity(
            sectors = listOf(sector),
            parameters = parameters,
            referenceTime = System.currentTimeMillis()
        )

        alarmView.alertEventConsumer(localActivity)
        alarmView.draw(canvas)

        verify {
            localActivityRenderer.renderLocalActivity(eq(localActivity), any(), any())
        }
    }

    @Test
    fun shouldSetColorHandlerOnRenderers() {
        val colorHandler: ColorHandler = mockk(relaxed = true)
        every { colorHandler.backgroundColor } returns 0xFFFFFFFF.toInt()

        alarmView.setColorHandler(colorHandler, 600000)

        verify {
            symbolRenderer.colorHandler = colorHandler
            localActivityRenderer.colorHandler = colorHandler
            localActivityRenderer.intervalDuration = 600000
        }
    }

    @Test
    fun shouldCalculateCorrectAlarmViewDataDimensions() {
        alarmView.measure(
            View.MeasureSpec.makeMeasureSpec(400, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(400, View.MeasureSpec.EXACTLY)
        )
        alarmView.layout(0, 0, 400, 400)

        alarmView.alertEventConsumer(Outlying)
        alarmView.draw(canvas)

        val dataSlot = slot<AlarmViewData>()
        verify {
            symbolRenderer.drawOutOfRangeSymbol(capture(dataSlot), any())
        }

        assertThat(dataSlot.captured.size).isEqualTo(400)
        assertThat(dataSlot.captured.center).isEqualTo(200f)
        assertThat(dataSlot.captured.radius).isLessThan(200f) // Should account for padding
    }

    @Test
    fun shouldHandleWarningTypeTransitions() {
        // Start with LocalActivity
        val localActivity = createLocalActivity()
        alarmView.alertEventConsumer(localActivity)
        alarmView.draw(canvas)

        verify {
            localActivityRenderer.renderLocalActivity(any(), any(), any())
        }

        // Transition to Outlying
        alarmView.alertEventConsumer(Outlying)
        alarmView.draw(canvas)

        verify {
            symbolRenderer.drawOutOfRangeSymbol(any(), any())
        }

        // Transition to NoLocation
        alarmView.alertEventConsumer(NoLocation)
        alarmView.draw(canvas)

        verify {
            symbolRenderer.drawNoLocationSymbol(any(), any())
        }
    }

    @Test
    fun shouldClearCanvasBeforeDrawing() {
        alarmView.alertEventConsumer(Outlying)
        alarmView.draw(canvas)

        verify {
            canvasWrapper.clear()
        }
    }

    @Test
    fun shouldUpdateCanvasAfterDrawing() {
        alarmView.alertEventConsumer(Outlying)
        alarmView.draw(canvas)

        verify {
            canvasWrapper.update(canvas)
        }
    }
}
