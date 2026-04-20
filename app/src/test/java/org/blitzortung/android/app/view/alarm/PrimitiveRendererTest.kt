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
import android.graphics.RectF
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test

class PrimitiveRendererTest {

    private lateinit var renderer: PrimitiveRenderer
    private lateinit var canvas: Canvas
    private lateinit var paint: Paint

    @Before
    fun setUp() {
        renderer = PrimitiveRenderer()
        canvas = mockk()
        paint = mockk(relaxed = true)
    }

    @Test
    fun shouldDrawCrossWithCorrectCoordinates() {
        val center = 100f
        val radius = 50f

        every { canvas.drawLine(any(), any(), any(), any(), any()) } returns Unit

        renderer.drawCross(center, radius, paint, canvas)

        // Verify horizontal line from (center - radius, center) to (center + radius, center)
        verify(exactly = 1) {
            canvas.drawLine(
                eq(50f),    // center - radius
                eq(100f),   // center
                eq(150f),   // center + radius
                eq(100f),   // center
                eq(paint)
            )
        }

        // Verify vertical line from (center, center - radius) to (center, center + radius)
        verify(exactly = 1) {
            canvas.drawLine(
                eq(100f),   // center
                eq(50f),    // center - radius
                eq(100f),   // center
                eq(150f),   // center + radius
                eq(paint)
            )
        }
    }

    @Test
    fun shouldDrawCrossWithZeroRadius() {
        val center = 100f
        val radius = 0f

        every { canvas.drawLine(any(), any(), any(), any(), any()) } returns Unit

        renderer.drawCross(center, radius, paint, canvas)

        // Should draw two lines from center to center (both lines are the same)
        verify(exactly = 2) {
            canvas.drawLine(
                eq(100f),
                eq(100f),
                eq(100f),
                eq(100f),
                eq(paint)
            )
        }
    }

    @Test
    fun shouldDrawCrossAtOrigin() {
        val center = 0f
        val radius = 25f

        every { canvas.drawLine(any(), any(), any(), any(), any()) } returns Unit

        renderer.drawCross(center, radius, paint, canvas)

        verify {
            canvas.drawLine(eq(-25f), eq(0f), eq(25f), eq(0f), eq(paint))
        }

        verify {
            canvas.drawLine(eq(0f), eq(-25f), eq(0f), eq(25f), eq(paint))
        }
    }

    @Test
    fun shouldDrawCircleAsArc() {
        val center = 100f
        val radius = 50f

        every { canvas.drawArc(any(), any(), any(), any(), any()) } returns Unit

        renderer.drawCircle(center, radius, paint, canvas)

        // Verify drawArc is called with a 360-degree arc
        verify(exactly = 1) {
            canvas.drawArc(
                any(),      // RectF bounds
                eq(0f),     // startAngle
                eq(360f),   // sweepAngle (full circle)
                eq(false),  // useCenter
                eq(paint)
            )
        }
    }

    @Test
    fun shouldDrawCircleWithZeroRadius() {
        val center = 100f
        val radius = 0f

        every { canvas.drawArc(any(), any(), any(), any(), any()) } returns Unit

        renderer.drawCircle(center, radius, paint, canvas)

        verify(exactly = 1) {
            canvas.drawArc(any(), eq(0f), eq(360f), eq(false), eq(paint))
        }
    }

    @Test
    fun shouldDrawCircleAtOrigin() {
        val center = 0f
        val radius = 30f

        every { canvas.drawArc(any(), any(), any(), any(), any()) } returns Unit

        renderer.drawCircle(center, radius, paint, canvas)

        verify {
            canvas.drawArc(any(), any(), any(), any(), any())
        }
    }

    @Test
    fun shouldDrawCircleWithLargeRadius() {
        val center = 500f
        val radius = 450f

        every { canvas.drawArc(any(), any(), any(), any(), any()) } returns Unit

        renderer.drawCircle(center, radius, paint, canvas)

        verify {
            canvas.drawArc(any(), any(), any(), any(), any())
        }
    }

    @Test
    fun shouldDrawCenteredTextAtCorrectPosition() {
        val text = "Test"
        val center = 200f
        val textBounds = mockk<android.graphics.Rect>()

        // Mock the paint to return text bounds
        every { paint.getTextBounds(text, 0, text.length, any()) } answers {
            val rect = arg<android.graphics.Rect>(3)
            rect.left = 0
            rect.top = -20
            rect.right = 40
            rect.bottom = 0
            Unit
        }

        every { canvas.drawText(any<String>(), any(), any(), any()) } returns Unit

        renderer.drawCenteredText(canvas, text, center, paint)

        // Text should be centered: x = center - right/2 = 200 - 40/2 = 180
        // y = center - top/2 = 200 - (-20)/2 = 200 + 10 = 210
        verify(exactly = 1) {
            canvas.drawText(
                eq(text),
                eq(180f),   // center - textBounds.right / 2
                eq(210f),   // center - textBounds.top / 2
                eq(paint)
            )
        }
    }

    @Test
    fun shouldDrawCenteredTextWithNegativeBounds() {
        val text = "Negative"
        val center = 150f

        every { paint.getTextBounds(text, 0, text.length, any()) } answers {
            val rect = arg<android.graphics.Rect>(3)
            rect.left = -5
            rect.top = -30
            rect.right = 50
            rect.bottom = 5
            Unit
        }

        every { canvas.drawText(any<String>(), any(), any(), any()) } returns Unit

        renderer.drawCenteredText(canvas, text, center, paint)

        // x = center - right/2 = 150 - 50/2 = 125
        // y = center - top/2 = 150 - (-30)/2 = 150 + 15 = 165
        verify(exactly = 1) {
            canvas.drawText(
                eq(text),
                eq(125f),
                eq(165f),
                eq(paint)
            )
        }
    }

    @Test
    fun shouldDrawCenteredTextWithZeroBounds() {
        val text = ""
        val center = 100f

        every { paint.getTextBounds(text, 0, text.length, any()) } answers {
            val rect = arg<android.graphics.Rect>(3)
            rect.left = 0
            rect.top = 0
            rect.right = 0
            rect.bottom = 0
            Unit
        }

        every { canvas.drawText(any<String>(), any(), any(), any()) } returns Unit

        renderer.drawCenteredText(canvas, text, center, paint)

        // With zero bounds, text should be at center
        verify(exactly = 1) {
            canvas.drawText(
                eq(text),
                eq(100f),
                eq(100f),
                eq(paint)
            )
        }
    }

    @Test
    fun shouldDrawCenteredTextAtOrigin() {
        val text = "Origin"
        val center = 0f

        every { paint.getTextBounds(text, 0, text.length, any()) } answers {
            val rect = arg<android.graphics.Rect>(3)
            rect.left = 0
            rect.top = -15
            rect.right = 30
            rect.bottom = 0
            Unit
        }

        every { canvas.drawText(any<String>(), any(), any(), any()) } returns Unit

        renderer.drawCenteredText(canvas, text, center, paint)

        // Just verify that drawText is called with the correct text
        verify(exactly = 1) {
            canvas.drawText(eq(text), any(), any(), eq(paint))
        }
    }

    @Test
    fun shouldHandleMultipleDrawCallsIndependently() {
        every { canvas.drawLine(any(), any(), any(), any(), any()) } returns Unit
        every { canvas.drawArc(any(), any(), any(), any(), any()) } returns Unit
        every { canvas.drawText(any<String>(), any(), any(), any()) } returns Unit

        // Draw a cross
        renderer.drawCross(100f, 50f, paint, canvas)

        // Draw a circle
        renderer.drawCircle(200f, 75f, paint, canvas)

        // Draw centered text
        every { paint.getTextBounds(any<String>(), any<Int>(), any<Int>(), any<android.graphics.Rect>()) } answers {
            val rect = arg<android.graphics.Rect>(3)
            rect.left = 0
            rect.top = -10
            rect.right = 20
            rect.bottom = 0
            Unit
        }
        renderer.drawCenteredText(canvas, "Test", 300f, paint)

        // Verify all methods were called
        verify(exactly = 2) { canvas.drawLine(any(), any(), any(), any(), any()) }
        verify(exactly = 1) { canvas.drawArc(any(), any(), any(), any(), any()) }
        verify(exactly = 1) { canvas.drawText(any<String>(), any(), any(), any()) }
    }

    @Test
    fun shouldDrawCrossWithNegativeCenter() {
        val center = -50f
        val radius = 25f

        every { canvas.drawLine(any(), any(), any(), any(), any()) } returns Unit

        renderer.drawCross(center, radius, paint, canvas)

        verify {
            canvas.drawLine(eq(-75f), eq(-50f), eq(-25f), eq(-50f), eq(paint))
        }

        verify {
            canvas.drawLine(eq(-50f), eq(-75f), eq(-50f), eq(-25f), eq(paint))
        }
    }

    @Test
    fun shouldDrawCircleWithNegativeCenter() {
        val center = -100f
        val radius = 50f

        every { canvas.drawArc(any(), any(), any(), any(), any()) } returns Unit

        renderer.drawCircle(center, radius, paint, canvas)

        verify {
            canvas.drawArc(any(), any(), any(), any(), any())
        }
    }

    @Test
    fun shouldUseSamePaintObjectForAllDrawCalls() {
        val customPaint = mockk<Paint>(relaxed = true)

        every { canvas.drawLine(any(), any(), any(), any(), any()) } returns Unit
        every { canvas.drawArc(any(), any(), any(), any(), any()) } returns Unit

        renderer.drawCross(100f, 50f, customPaint, canvas)
        renderer.drawCircle(200f, 75f, customPaint, canvas)

        verify { canvas.drawLine(any(), any(), any(), any(), eq(customPaint)) }
        verify { canvas.drawArc(any(), any(), any(), any(), eq(customPaint)) }
    }

    @Test
    fun shouldDrawCircleWithFractionalValues() {
        val center = 123.45f
        val radius = 67.89f

        every { canvas.drawArc(any(), any(), any(), any(), any()) } returns Unit

        renderer.drawCircle(center, radius, paint, canvas)

        verify {
            canvas.drawArc(any(), any(), any(), any(), any())
        }
    }

    @Test
    fun shouldDrawCrossWithFractionalValues() {
        val center = 98.76f
        val radius = 43.21f

        every { canvas.drawLine(any(), any(), any(), any(), any()) } returns Unit

        renderer.drawCross(center, radius, paint, canvas)

        // Just verify that drawLine is called twice (horizontal and vertical)
        verify(exactly = 2) {
            canvas.drawLine(any(), any(), any(), any(), eq(paint))
        }
    }
}
