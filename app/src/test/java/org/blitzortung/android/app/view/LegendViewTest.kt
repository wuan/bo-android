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

import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.blitzortung.android.data.Parameters
import org.blitzortung.android.data.TimeInterval
import org.blitzortung.android.map.overlay.StrikeListOverlay
import org.blitzortung.android.map.overlay.color.StrikeColorHandler
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class LegendViewTest {
    private lateinit var legendView: LegendView
    private lateinit var canvas: Canvas
    private lateinit var strikesOverlay: StrikeListOverlay
    private lateinit var colorHandler: StrikeColorHandler

    @Before
    fun setUp() {
        val context = RuntimeEnvironment.getApplication()

        legendView = LegendView(context)
        legendView.measure(
            View.MeasureSpec.makeMeasureSpec(400, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(400, View.MeasureSpec.EXACTLY),
        )
        legendView.layout(0, 0, 400, 400)
        canvas = Canvas(Bitmap.createBitmap(400, 400, Bitmap.Config.ARGB_8888))

        colorHandler = mockk(relaxed = true)
        every { colorHandler.numberOfColors } returns 3
        every { colorHandler.colors } returns intArrayOf(0xFF0000, 0x00FF00, 0x0000FF)
        every { colorHandler.getColor(any()) } returns 0xFF0000

        strikesOverlay = mockk(relaxed = true)
        every { strikesOverlay.colorHandler } returns colorHandler
        every { strikesOverlay.parameters } returns Parameters(interval = TimeInterval(duration = 60))
    }

    @Test
    fun onDrawWithoutOverlayDoesNotThrow() {
        legendView.draw(canvas)
    }

    @Test
    fun onDrawDrawsColorLegend() {
        legendView.strikesOverlay = strikesOverlay

        legendView.draw(canvas)

        verify(atLeast = 1) { colorHandler.getColor(any()) }
    }

    @Test
    fun onMeasureWithOverlayMeasuresView() {
        legendView.strikesOverlay = strikesOverlay

        legendView.measure(
            View.MeasureSpec.makeMeasureSpec(500, View.MeasureSpec.AT_MOST),
            View.MeasureSpec.makeMeasureSpec(500, View.MeasureSpec.AT_MOST),
        )

        assertThat(legendView.measuredWidth).isGreaterThan(0)
    }

    @Test
    fun onDrawWithRegionAndGridDrawsAllSections() {
        val parameters =
            Parameters(
                region = 42,
                gridSize = 10000,
                interval = TimeInterval(duration = 60),
                countThreshold = 5,
            )
        every { strikesOverlay.parameters } returns parameters
        every { strikesOverlay.usesGrid() } returns true
        every { strikesOverlay.gridParameters } returns mockk(relaxed = true)
        legendView.strikesOverlay = strikesOverlay

        legendView.draw(canvas)

        verify(atLeast = 1) { strikesOverlay.usesGrid() }
    }
}
