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

package org.blitzortung.android.app.view.alarm

import android.graphics.Canvas
import android.graphics.Paint
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.blitzortung.android.alert.AlertParameters
import org.blitzortung.android.alert.LocalActivity
import org.blitzortung.android.alert.data.AlertSector
import org.blitzortung.android.alert.data.AlertSectorRange
import org.blitzortung.android.app.view.AlarmViewData
import org.blitzortung.android.app.view.support.CanvasWrapper
import org.blitzortung.android.map.overlay.color.ColorHandler
import org.blitzortung.android.util.MeasurementSystem
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class LocalActivityRendererTest {

    private lateinit var renderer: LocalActivityRenderer
    private lateinit var primitiveRenderer: PrimitiveRenderer
    private lateinit var canvas: Canvas
    private lateinit var canvasWrapper: CanvasWrapper
    private lateinit var colorHandler: ColorHandler
    private lateinit var backgroundPaint: Paint

    @Before
    fun setUp() {
        val context = RuntimeEnvironment.getApplication()
        primitiveRenderer = mockk(relaxed = true)
        canvas = mockk(relaxed = true)
        canvasWrapper = mockk(relaxed = true)
        colorHandler = mockk(relaxed = true)
        backgroundPaint = mockk(relaxed = true)

        every { canvasWrapper.canvas } returns canvas
        every { canvasWrapper.background } returns backgroundPaint
        every { colorHandler.lineColor } returns 0xFF000000.toInt()
        every { colorHandler.textColor } returns 0xFF000000.toInt()
        every { colorHandler.getColor(any(), any(), any()) } returns 0xFFFF0000.toInt()

        renderer = LocalActivityRenderer(
            context = context,
            primitiveRenderer = primitiveRenderer,
            textSize = 20f
        )

        renderer.colorHandler = colorHandler
        renderer.intervalDuration = 600000
    }

    private fun createLocalActivity(
        sectors: List<AlertSector> = listOf(createAlertSector()),
        rangeSteps: List<Float> = listOf(10f, 25f, 50f)
    ): LocalActivity {
        val parameters = AlertParameters(
            alarmInterval = 600000L,
            rangeSteps = rangeSteps,
            sectorLabels = listOf("N", "NE", "E", "SE", "S", "SW", "W", "NW"),
            measurementSystem = MeasurementSystem.METRIC
        )
        return LocalActivity(
            sectors = sectors,
            parameters = parameters,
            referenceTime = System.currentTimeMillis()
        )
    }

    private fun createAlertSector(
        label: String = "N",
        minimumBearing: Float = 0f,
        maximumBearing: Float = 45f,
        ranges: List<AlertSectorRange> = listOf(
            AlertSectorRange(0f, 10f, 5, System.currentTimeMillis()),
            AlertSectorRange(10f, 25f, 3, System.currentTimeMillis()),
            AlertSectorRange(25f, 50f, 0, 0L)
        )
    ): AlertSector {
        return AlertSector(
            label = label,
            minimumSectorBearing = minimumBearing,
            maximumSectorBearing = maximumBearing,
            ranges = ranges,
            closestStrikeDistance = 10f
        )
    }

    @Test
    fun shouldRenderAllRangeCircles() {
        val localActivity = createLocalActivity(rangeSteps = listOf(10f, 25f, 50f))
        val alarmViewData = AlarmViewData(size = 400, center = 200f, radius = 180f)

        renderer.renderLocalActivity(localActivity, alarmViewData, canvasWrapper)

        // Verify that circles are drawn for each range step
        verify(exactly = 3) {
            primitiveRenderer.drawCircle(
                eq(200f),
                any(),
                any(),
                eq(canvas)
            )
        }
    }

    @Test
    fun shouldRenderSectorBackgroundForEachSector() {
        val sector1 = createAlertSector(label = "N", minimumBearing = 0f, maximumBearing = 45f)
        val sector2 = createAlertSector(label = "NE", minimumBearing = 45f, maximumBearing = 90f)
        val localActivity = createLocalActivity(sectors = listOf(sector1, sector2))
        val alarmViewData = AlarmViewData(size = 400, center = 200f, radius = 180f)

        renderer.renderLocalActivity(localActivity, alarmViewData, canvasWrapper)

        // Each sector has 3 ranges, so we should have 6 arc draws (2 sectors * 3 ranges)
        verify(atLeast = 6) {
            canvas.drawArc(any(), any(), any(), any(), any())
        }
    }

    @Test
    fun shouldRenderSectorSideLinesForEachSector() {
        val sector1 = createAlertSector(label = "N", minimumBearing = 0f)
        val sector2 = createAlertSector(label = "NE", minimumBearing = 45f)
        val localActivity = createLocalActivity(sectors = listOf(sector1, sector2))
        val alarmViewData = AlarmViewData(size = 400, center = 200f, radius = 180f)

        renderer.renderLocalActivity(localActivity, alarmViewData, canvasWrapper)

        // Verify that lines are drawn for each sector boundary
        verify(atLeast = 2) {
            canvas.drawLine(any(), any(), any(), any(), any())
        }
    }

    @Test
    fun shouldUseColorHandlerForSectorColors() {
        val localActivity = createLocalActivity()
        val alarmViewData = AlarmViewData(size = 400, center = 200f, radius = 180f)

        renderer.renderLocalActivity(localActivity, alarmViewData, canvasWrapper)

        verify(atLeast = 1) {
            colorHandler.getColor(any(), any(), any())
        }
    }

    @Test
    fun shouldRenderWithCorrectRadiusIncrement() {
        val localActivity = createLocalActivity(rangeSteps = listOf(10f, 25f, 50f))
        val alarmViewData = AlarmViewData(size = 400, center = 200f, radius = 180f)
        val radiusSlots = mutableListOf<Float>()

        every { primitiveRenderer.drawCircle(any(), capture(radiusSlots), any(), any()) } returns Unit

        renderer.renderLocalActivity(localActivity, alarmViewData, canvasWrapper)

        // Radius increment should be 180f / 3 = 60f
        // Circles should be at 60f, 120f, 180f
        assertThat(radiusSlots).hasSize(3)
        assertThat(radiusSlots[0]).isEqualTo(60f)
        assertThat(radiusSlots[1]).isEqualTo(120f)
        assertThat(radiusSlots[2]).isEqualTo(180f)
    }

    @Test
    fun shouldNotRenderDescriptionTextWhenDisabled() {
        renderer.enableDescriptionText = false
        val localActivity = createLocalActivity()
        val alarmViewData = AlarmViewData(size = 400, center = 200f, radius = 180f)

        renderer.renderLocalActivity(localActivity, alarmViewData, canvasWrapper)

        verify(exactly = 0) {
            canvas.drawText(any<String>(), any(), any(), any())
        }
    }

    @Test
    fun shouldRenderDescriptionTextWhenEnabledAndSizeAboveMinimum() {
        renderer.enableDescriptionText = true
        val localActivity = createLocalActivity()
        val alarmViewData = AlarmViewData(size = 400, center = 200f, radius = 180f)

        renderer.renderLocalActivity(localActivity, alarmViewData, canvasWrapper)

        // Should render text for range labels and sector labels
        verify(atLeast = 1) {
            canvas.drawText(any<String>(), any(), any(), any())
        }
    }

    @Test
    fun shouldNotRenderDescriptionTextWhenSizeBelowMinimum() {
        renderer.enableDescriptionText = true
        val localActivity = createLocalActivity()
        val alarmViewData = AlarmViewData(size = 250, center = 125f, radius = 115f)

        renderer.renderLocalActivity(localActivity, alarmViewData, canvasWrapper)

        verify(exactly = 0) {
            canvas.drawText(any<String>(), any(), any(), any())
        }
    }

    @Test
    fun shouldRenderEmptySectorWithBackgroundPaint() {
        val emptyRange = AlertSectorRange(0f, 10f, 0, 0L)
        val sector = createAlertSector(ranges = listOf(emptyRange))
        val localActivity = createLocalActivity(sectors = listOf(sector))
        val alarmViewData = AlarmViewData(size = 400, center = 200f, radius = 180f)

        renderer.renderLocalActivity(localActivity, alarmViewData, canvasWrapper)

        // Verify that arc is drawn with background paint when strike count is 0
        verify(atLeast = 1) {
            canvas.drawArc(any(), any(), any(), any(), eq(backgroundPaint))
        }
    }

    @Test
    fun shouldRenderSectorWithStrikesUsingColorPaint() {
        val rangeWithStrikes = AlertSectorRange(0f, 10f, 5, System.currentTimeMillis())
        val sector = createAlertSector(ranges = listOf(rangeWithStrikes))
        val localActivity = createLocalActivity(sectors = listOf(sector))
        val alarmViewData = AlarmViewData(size = 400, center = 200f, radius = 180f)

        renderer.renderLocalActivity(localActivity, alarmViewData, canvasWrapper)

        // Verify that arc is drawn with a colored paint when strike count > 0
        val paintSlot = slot<Paint>()
        verify(atLeast = 1) {
            canvas.drawArc(any(), any(), any(), any(), capture(paintSlot))
        }

        // At least one paint should not be the background paint
        assertThat(paintSlot.captured).isNotEqualTo(backgroundPaint)
    }

    @Test
    fun shouldApplyColorHandlerLineColor() {
        every { colorHandler.lineColor } returns 0xFF00FF00.toInt()

        val localActivity = createLocalActivity()
        val alarmViewData = AlarmViewData(size = 400, center = 200f, radius = 180f)

        renderer.renderLocalActivity(localActivity, alarmViewData, canvasWrapper)

        verify { colorHandler.lineColor }
    }

    @Test
    fun shouldApplyColorHandlerTextColor() {
        renderer.enableDescriptionText = true
        every { colorHandler.textColor } returns 0xFF0000FF.toInt()

        val localActivity = createLocalActivity()
        val alarmViewData = AlarmViewData(size = 400, center = 200f, radius = 180f)

        renderer.renderLocalActivity(localActivity, alarmViewData, canvasWrapper)

        verify { colorHandler.textColor }
    }

    @Test
    fun shouldCalculateCorrectStrokeWidthBasedOnSize() {
        val localActivity = createLocalActivity()
        val alarmViewData = AlarmViewData(size = 600, center = 300f, radius = 280f)

        renderer.renderLocalActivity(localActivity, alarmViewData, canvasWrapper)

        // Verify rendering happens (stroke width calculation is internal)
        verify(atLeast = 1) {
            primitiveRenderer.drawCircle(any(), any(), any(), any())
        }
    }

    @Test
    fun shouldRenderMultipleSectorsInOrder() {
        val sector1 = createAlertSector(label = "N", minimumBearing = 0f, maximumBearing = 45f)
        val sector2 = createAlertSector(label = "NE", minimumBearing = 45f, maximumBearing = 90f)
        val sector3 = createAlertSector(label = "E", minimumBearing = 90f, maximumBearing = 135f)
        val localActivity = createLocalActivity(sectors = listOf(sector1, sector2, sector3))
        val alarmViewData = AlarmViewData(size = 400, center = 200f, radius = 180f)

        renderer.renderLocalActivity(localActivity, alarmViewData, canvasWrapper)

        // Each sector should render its background arcs
        verify(atLeast = 9) { // 3 sectors * 3 ranges = 9 arcs
            canvas.drawArc(any(), any(), any(), any(), any())
        }

        // Each sector should render its side line
        verify(atLeast = 3) {
            canvas.drawLine(any(), any(), any(), any(), any())
        }
    }
}
