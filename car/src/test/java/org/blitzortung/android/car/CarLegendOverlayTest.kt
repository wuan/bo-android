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

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Rect
import androidx.preference.PreferenceManager
import org.assertj.core.api.Assertions.assertThat
import org.blitzortung.android.data.Parameters
import org.blitzortung.android.data.TimeInterval
import org.blitzortung.android.map.overlay.StrikeListOverlay
import org.blitzortung.android.map.overlay.color.ColorTarget
import org.blitzortung.android.map.overlay.color.StrikeColorHandler
import org.junit.Test
import org.junit.runner.RunWith
import org.osmdroid.views.MapView
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class CarLegendOverlayTest {
    private fun createOverlay(
        intervalDuration: Int = 60,
        palette: IntArray? = null,
    ): Pair<CarLegendOverlay, StrikeListOverlay> {
        val context = RuntimeEnvironment.getApplication()
        val colorHandler = StrikeColorHandler(PreferenceManager.getDefaultSharedPreferences(context))
        val effectiveHandler =
            if (palette == null) {
                colorHandler
            } else {
                object : StrikeColorHandler(PreferenceManager.getDefaultSharedPreferences(context)) {
                    override fun getColors(target: ColorTarget): IntArray = palette
                }
            }
        val mapView = MapView(context)
        val strikesOverlay =
            StrikeListOverlay(mapView, effectiveHandler).apply {
                parameters = Parameters(interval = TimeInterval(duration = intervalDuration))
            }
        return CarLegendOverlay(context, strikesOverlay) to strikesOverlay
    }

    @Test
    fun `band uses the active strike colours from newest to oldest`() {
        val (legend, strikesOverlay) = createOverlay()

        assertThat(legend.band()?.colors)
            .containsExactlyElementsOf(strikesOverlay.colorHandler.colors.toList())
    }

    @Test
    fun `labels a 60 minute interval at zero, half and full age`() {
        val (legend, _) = createOverlay(intervalDuration = 60)

        assertThat(legend.band()?.labels).containsExactly("0 min", "−30 min", "−60 min")
    }

    @Test
    fun `labels a 30 minute interval at zero, half and full age`() {
        val (legend, _) = createOverlay(intervalDuration = 30)

        assertThat(legend.band()?.labels).containsExactly("0 min", "−15 min", "−30 min")
    }

    @Test
    fun `band height follows the colour count`() {
        val (sixColourLegend, _) = createOverlay()
        val (fourColourLegend, _) =
            createOverlay(palette = intArrayOf(Color.RED, Color.YELLOW, Color.GREEN, Color.BLUE))

        assertThat(sixColourLegend.bandBounds()?.height()).isEqualTo(6 * 24f)
        assertThat(fourColourLegend.bandBounds()?.height()).isEqualTo(4 * 24f)
    }

    @Test
    fun `panel reserves half a text line above and below the band`() {
        val (legend, _) = createOverlay()
        val bandBounds = legend.bandBounds()!!

        val panelBounds = legend.panelBounds()

        assertThat(bandBounds.top - panelBounds.top).isGreaterThanOrEqualTo(legend.labelOverhangPx)
        assertThat(panelBounds.bottom - bandBounds.bottom).isGreaterThanOrEqualTo(legend.labelOverhangPx)
    }

    @Test
    fun `reuses the band gradient while the palette and bounds are unchanged`() {
        val (legend, _) = createOverlay()
        val canvas = Canvas(Bitmap.createBitmap(400, 400, Bitmap.Config.ARGB_8888))

        legend.draw(canvas, null, false)
        legend.draw(canvas, null, false)

        assertThat(legend.gradientBuilds).isEqualTo(1)
    }

    @Test
    fun `rebuilds the band gradient when the density changes`() {
        val (legend, _) = createOverlay()
        val canvas = Canvas(Bitmap.createBitmap(400, 400, Bitmap.Config.ARGB_8888))
        legend.draw(canvas, null, false)

        legend.densityDpi = 320
        legend.draw(canvas, null, false)

        assertThat(legend.gradientBuilds).isEqualTo(2)
    }

    @Test
    fun `uses the configured text size scaled by the surface density`() {
        val (legend, _) = createOverlay()
        val textSizeDp = RuntimeEnvironment.getApplication().resources.getInteger(R.integer.car_legend_text_size_dp)

        legend.densityDpi = 320

        assertThat(legend.textSizePx).isEqualTo(textSizeDp * 2f)
    }

    @Test
    fun `uses the phone legend text size by default`() {
        val textSizeDp = RuntimeEnvironment.getApplication().resources.getInteger(R.integer.car_legend_text_size_dp)

        assertThat(textSizeDp).isEqualTo(16)
    }

    @Test
    fun `draws the data timestamp above the legend`() {
        val (legend, _) = createOverlay()
        legend.onVisibleAreaChanged(Rect(0, 0, 1000, 900))
        legend.dataTime = "Data 12:34:56"

        val bounds = legend.panelBounds()
        legend.dataTime = ""
        val boundsWithoutDataTime = legend.panelBounds()

        assertThat(bounds.height()).isGreaterThan(boundsWithoutDataTime.height())
        assertThat(bounds.bottom).isEqualTo(boundsWithoutDataTime.bottom)
    }

    @Test
    fun `hugs the left edge`() {
        val (legend, _) = createOverlay()
        legend.onVisibleAreaChanged(Rect(300, 60, 1000, 900))

        val bounds = legend.panelBounds()

        assertThat(bounds.left).isLessThan(100f)
    }

    @Test
    fun `sits at the bottom-left of the map surface`() {
        val (legend, _) = createOverlay()
        legend.surfaceHeight = 1000

        val bounds = legend.panelBounds()

        assertThat(bounds.left).isEqualTo(PANEL_INSET)
        assertThat(1000f - bounds.bottom).isEqualTo(PANEL_INSET)
    }

    @Test
    fun `prefers the host's reported area bottom over the surface height`() {
        val (legend, _) = createOverlay()
        legend.surfaceHeight = 1000
        legend.onVisibleAreaChanged(Rect(300, 60, 1000, 900))

        val bounds = legend.panelBounds()

        assertThat(bounds.bottom).isEqualTo(900f - PANEL_INSET)
    }

    @Test
    fun `falls back to the host's map area bottom when the surface height is unknown`() {
        val (legend, _) = createOverlay()
        legend.onVisibleAreaChanged(Rect(300, 60, 1000, 900))

        val bounds = legend.panelBounds()

        assertThat(bounds.bottom).isEqualTo(900f - PANEL_INSET)
    }

    @Test
    fun `uses the surface bottom while no map area is known`() {
        val (legend, _) = createOverlay()
        legend.surfaceHeight = 1000

        val bounds = legend.panelBounds()

        assertThat(bounds.bottom).isEqualTo(1000f - PANEL_INSET)
    }

    @Test
    fun `prefers the stable area over the visible area`() {
        val (legend, _) = createOverlay()

        legend.onVisibleAreaChanged(Rect(280, 40, 1000, 850))
        legend.onStableAreaChanged(Rect(300, 90, 1000, 900))

        val bounds = legend.panelBounds()
        assertThat(bounds.bottom).isEqualTo(900f - PANEL_INSET)
    }

    @Test
    fun `falls back to the visible area while no stable area is known`() {
        val (legend, _) = createOverlay()

        legend.onVisibleAreaChanged(Rect(280, 40, 1000, 900))

        val bounds = legend.panelBounds()
        assertThat(bounds.bottom).isEqualTo(900f - PANEL_INSET)
    }

    @Test
    fun `ignores an empty stable area`() {
        val (legend, _) = createOverlay()
        legend.onVisibleAreaChanged(Rect(280, 40, 1000, 900))

        legend.onStableAreaChanged(Rect(0, 0, 0, 0))

        val bounds = legend.panelBounds()
        assertThat(bounds.bottom).isEqualTo(900f - PANEL_INSET)
    }

    private companion object {
        /** `CarLegendOverlay.PANEL_INSET_DP` at the default 160 dpi (scale 1). */
        const val PANEL_INSET = 8f
    }
}
