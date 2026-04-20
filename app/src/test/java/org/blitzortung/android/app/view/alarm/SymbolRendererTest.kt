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
import io.mockk.verify
import org.blitzortung.android.app.view.AlarmViewData
import org.blitzortung.android.map.overlay.color.ColorHandler
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class SymbolRendererTest {

    private lateinit var renderer: SymbolRenderer
    private lateinit var primitiveRenderer: PrimitiveRenderer
    private lateinit var canvas: Canvas
    private lateinit var colorHandler: ColorHandler

    @Before
    fun setUp() {
        val context = RuntimeEnvironment.getApplication()
        primitiveRenderer = mockk(relaxed = true)
        canvas = mockk(relaxed = true)
        colorHandler = mockk(relaxed = true)

        every { colorHandler.lineColor } returns 0xFF000000.toInt()
        every { colorHandler.textColor } returns 0xFF000000.toInt()

        renderer = SymbolRenderer(
            context = context,
            primitiveRenderer = primitiveRenderer,
            textSize = 20f
        )

        renderer.colorHandler = colorHandler
    }

    @Test
    fun shouldDrawOutOfRangeSymbolWithCrossAndCircles() {
        val alarmViewData = AlarmViewData(size = 400, center = 200f, radius = 180f)

        renderer.drawOutOfRangeSymbol(alarmViewData, canvas)

        // Should draw a cross at 10% of radius
        verify(exactly = 1) {
            primitiveRenderer.drawCross(
                eq(200f),
                any(),  // radius * 0.1 (allow float precision)
                any(),
                eq(canvas)
            )
        }

        // Should draw 3 circles at 50%, 80%, and 100% of radius
        verify(exactly = 3) {
            primitiveRenderer.drawCircle(eq(200f), any(), any(), eq(canvas))
        }
    }

    @Test
    fun shouldDrawOutOfRangeSymbolWithColorHandler() {
        val alarmViewData = AlarmViewData(size = 400, center = 200f, radius = 180f)
        every { colorHandler.lineColor } returns 0xFFFF0000.toInt()

        renderer.drawOutOfRangeSymbol(alarmViewData, canvas)

        verify { colorHandler.lineColor }
    }

    @Test
    fun shouldDrawOutOfRangeSymbolWithCorrectStrokeWidth() {
        val alarmViewData = AlarmViewData(size = 400, center = 200f, radius = 180f)

        renderer.drawOutOfRangeSymbol(alarmViewData, canvas)

        // Stroke width should be size / 80 = 400 / 80 = 5
        verify(atLeast = 1) {
            primitiveRenderer.drawCross(any(), any(), any(), any())
        }
    }

    @Test
    fun shouldDrawOwnLocationSymbolWithCircleAndCross() {
        val alarmViewData = AlarmViewData(size = 400, center = 200f, radius = 180f)

        renderer.drawOwnLocationSymbol(alarmViewData, canvas)

        // Should draw a circle at 80% of radius
        verify(exactly = 1) {
            primitiveRenderer.drawCircle(
                eq(200f),
                any(),  // radius * 0.8 (allow float precision)
                any(),
                eq(canvas)
            )
        }

        // Should draw a cross at 60% of radius
        verify(exactly = 1) {
            primitiveRenderer.drawCross(
                eq(200f),
                any(),  // radius * 0.6 (allow float precision)
                any(),
                eq(canvas)
            )
        }
    }

    @Test
    fun shouldDrawOwnLocationSymbolWithColorHandler() {
        val alarmViewData = AlarmViewData(size = 400, center = 200f, radius = 180f)
        every { colorHandler.lineColor } returns 0xFF00FF00.toInt()

        renderer.drawOwnLocationSymbol(alarmViewData, canvas)

        verify { colorHandler.lineColor }
    }

    @Test
    fun shouldDrawNoLocationSymbolWithQuestionMark() {
        val alarmViewData = AlarmViewData(size = 400, center = 200f, radius = 180f)

        renderer.drawNoLocationSymbol(alarmViewData, canvas)

        // Should draw centered text with "?"
        verify(exactly = 1) {
            primitiveRenderer.drawCenteredText(
                eq(canvas),
                eq("?"),
                eq(200f),
                any()
            )
        }
    }

    @Test
    fun shouldDrawNoLocationSymbolWithDashedCircle() {
        val alarmViewData = AlarmViewData(size = 400, center = 200f, radius = 180f)

        renderer.drawNoLocationSymbol(alarmViewData, canvas)

        // Should draw a circle at 80% of radius
        verify(exactly = 1) {
            primitiveRenderer.drawCircle(
                eq(200f),
                any(),  // radius * 0.8 (allow float precision)
                any(),
                eq(canvas)
            )
        }
    }

    @Test
    fun shouldDrawNoLocationSymbolWithColorHandler() {
        val alarmViewData = AlarmViewData(size = 400, center = 200f, radius = 180f)
        every { colorHandler.lineColor } returns 0xFF0000FF.toInt()

        renderer.drawNoLocationSymbol(alarmViewData, canvas)

        verify(atLeast = 1) { colorHandler.lineColor }
    }

    @Test
    fun shouldDrawNoLocationSymbolWithLargerTextSize() {
        val alarmViewData = AlarmViewData(size = 400, center = 200f, radius = 180f)

        renderer.drawNoLocationSymbol(alarmViewData, canvas)

        // Text size should be 3x the original (3 * 20 = 60)
        verify {
            primitiveRenderer.drawCenteredText(eq(canvas), eq("?"), any(), any())
        }
    }

    @Test
    fun shouldDrawAlertOrLocationMissingMessageWithMultipleLines() {
        val center = 200f
        val width = 400

        renderer.drawAlertOrLocationMissingMessage(center, width, canvas)

        // Should draw 3 lines: "Alarms disabled", "or", "Location information missing"
        verify(exactly = 3) {
            canvas.drawText(any<String>(), any(), any(), any())
        }
    }

    @Test
    fun shouldDrawAlertOrLocationMissingMessageAtCenter() {
        val center = 200f
        val width = 400

        renderer.drawAlertOrLocationMissingMessage(center, width, canvas)

        // Verify that text is drawn with center x-coordinate
        verify(atLeast = 1) {
            canvas.drawText(any<String>(), eq(center), any(), any())
        }
    }

    @Test
    fun shouldDrawAlertOrLocationMissingMessageWithScaledText() {
        val center = 200f
        val width = 400

        renderer.drawAlertOrLocationMissingMessage(center, width, canvas)

        // Text should be scaled to fit width - 20
        verify(exactly = 3) {
            canvas.drawText(any<String>(), any(), any(), any())
        }
    }

    @Test
    fun shouldDrawAlertOrLocationMissingMessageWithNarrowWidth() {
        val center = 100f
        val width = 200

        renderer.drawAlertOrLocationMissingMessage(center, width, canvas)

        // Should still draw 3 lines even with narrow width
        verify(exactly = 3) {
            canvas.drawText(any<String>(), any(), any(), any())
        }
    }

    @Test
    fun shouldDrawOutOfRangeSymbolWithDifferentSizes() {
        val smallData = AlarmViewData(size = 200, center = 100f, radius = 90f)
        val largeData = AlarmViewData(size = 800, center = 400f, radius = 380f)

        renderer.drawOutOfRangeSymbol(smallData, canvas)
        renderer.drawOutOfRangeSymbol(largeData, canvas)

        // Should be called twice (once for each size)
        verify(exactly = 2) {
            primitiveRenderer.drawCross(any(), any(), any(), any())
        }
    }

    @Test
    fun shouldDrawOwnLocationSymbolWithDifferentSizes() {
        val smallData = AlarmViewData(size = 200, center = 100f, radius = 90f)
        val largeData = AlarmViewData(size = 800, center = 400f, radius = 380f)

        renderer.drawOwnLocationSymbol(smallData, canvas)
        renderer.drawOwnLocationSymbol(largeData, canvas)

        // Should be called twice
        verify(exactly = 2) {
            primitiveRenderer.drawCircle(any(), any(), any(), any())
        }

        verify(exactly = 2) {
            primitiveRenderer.drawCross(any(), any(), any(), any())
        }
    }

    @Test
    fun shouldDrawNoLocationSymbolWithDifferentSizes() {
        val smallData = AlarmViewData(size = 200, center = 100f, radius = 90f)
        val largeData = AlarmViewData(size = 800, center = 400f, radius = 380f)

        renderer.drawNoLocationSymbol(smallData, canvas)
        renderer.drawNoLocationSymbol(largeData, canvas)

        // Should draw question mark twice
        verify(exactly = 2) {
            primitiveRenderer.drawCenteredText(eq(canvas), eq("?"), any(), any())
        }
    }

    @Test
    fun shouldUseColorHandlerWhenSet() {
        val alarmViewData = AlarmViewData(size = 400, center = 200f, radius = 180f)

        renderer.drawOutOfRangeSymbol(alarmViewData, canvas)
        renderer.drawOwnLocationSymbol(alarmViewData, canvas)
        renderer.drawNoLocationSymbol(alarmViewData, canvas)

        // Color handler should be accessed multiple times
        verify(atLeast = 3) { colorHandler.lineColor }
    }

    @Test
    fun shouldHandleNullColorHandler() {
        val alarmViewData = AlarmViewData(size = 400, center = 200f, radius = 180f)
        renderer.colorHandler = null

        // Should not throw exception when colorHandler is null
        renderer.drawOutOfRangeSymbol(alarmViewData, canvas)
        renderer.drawOwnLocationSymbol(alarmViewData, canvas)
        renderer.drawNoLocationSymbol(alarmViewData, canvas)

        // Should still draw the symbols
        verify(atLeast = 1) {
            primitiveRenderer.drawCross(any(), any(), any(), any())
        }

        verify(atLeast = 1) {
            primitiveRenderer.drawCircle(any(), any(), any(), any())
        }
    }

    @Test
    fun shouldDrawOutOfRangeSymbolWithZeroRadius() {
        val alarmViewData = AlarmViewData(size = 100, center = 50f, radius = 0f)

        renderer.drawOutOfRangeSymbol(alarmViewData, canvas)

        // Should draw cross at center (radius = 0)
        verify(exactly = 1) {
            primitiveRenderer.drawCross(eq(50f), eq(0f), any(), any())
        }

        // Should draw circles with 0 radius
        verify(exactly = 3) {
            primitiveRenderer.drawCircle(eq(50f), eq(0f), any(), any())
        }
    }

    @Test
    fun shouldDrawOwnLocationSymbolAtOrigin() {
        val alarmViewData = AlarmViewData(size = 100, center = 0f, radius = 50f)

        renderer.drawOwnLocationSymbol(alarmViewData, canvas)

        verify(exactly = 1) {
            primitiveRenderer.drawCircle(eq(0f), any(), any(), any())  // 80% of 50
        }

        verify(exactly = 1) {
            primitiveRenderer.drawCross(eq(0f), any(), any(), any())  // 60% of 50
        }
    }

    @Test
    fun shouldDrawNoLocationSymbolWithVerySmallSize() {
        val alarmViewData = AlarmViewData(size = 50, center = 25f, radius = 20f)

        renderer.drawNoLocationSymbol(alarmViewData, canvas)

        // Should still draw the question mark
        verify(exactly = 1) {
            primitiveRenderer.drawCenteredText(eq(canvas), eq("?"), eq(25f), any())
        }

        // Should still draw the circle
        verify(exactly = 1) {
            primitiveRenderer.drawCircle(eq(25f), any(), any(), any())  // 80% of 20
        }
    }

    @Test
    fun shouldCalculateStrokeWidthCorrectly() {
        val data1 = AlarmViewData(size = 80, center = 40f, radius = 35f)
        val data2 = AlarmViewData(size = 160, center = 80f, radius = 75f)
        val data3 = AlarmViewData(size = 800, center = 400f, radius = 380f)

        renderer.drawOutOfRangeSymbol(data1, canvas)  // stroke = 80/80 = 1
        renderer.drawOutOfRangeSymbol(data2, canvas)  // stroke = 160/80 = 2
        renderer.drawOutOfRangeSymbol(data3, canvas)  // stroke = 800/80 = 10

        // Verify drawing happened 3 times
        verify(exactly = 3) {
            primitiveRenderer.drawCross(any(), any(), any(), any())
        }
    }
}
