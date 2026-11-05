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

import android.graphics.Rect
import androidx.preference.PreferenceManager
import org.assertj.core.api.Assertions.assertThat
import org.blitzortung.android.data.Parameters
import org.blitzortung.android.data.TimeInterval
import org.blitzortung.android.map.overlay.StrikeListOverlay
import org.blitzortung.android.map.overlay.color.StrikeColorHandler
import org.junit.Test
import org.junit.runner.RunWith
import org.osmdroid.views.MapView
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class CarLegendOverlayTest {
    private fun createOverlay(intervalDuration: Int = 60): Pair<CarLegendOverlay, StrikeListOverlay> {
        val context = RuntimeEnvironment.getApplication()
        val colorHandler = StrikeColorHandler(PreferenceManager.getDefaultSharedPreferences(context))
        val mapView = MapView(context)
        val strikesOverlay =
            StrikeListOverlay(mapView, colorHandler).apply {
                parameters = Parameters(interval = TimeInterval(duration = intervalDuration))
            }
        return CarLegendOverlay(context, strikesOverlay) to strikesOverlay
    }

    @Test
    fun `shows one row per strike colour`() {
        val (legend, strikesOverlay) = createOverlay()

        assertThat(legend.entries()).hasSize(strikesOverlay.colorHandler.numberOfColors)
    }

    @Test
    fun `rows use the active strike colours`() {
        val (legend, strikesOverlay) = createOverlay()

        assertThat(legend.entries().map { it.color })
            .containsExactlyElementsOf(strikesOverlay.colorHandler.colors.toList())
    }

    @Test
    fun `labels each band with the age it represents`() {
        val (legend, _) = createOverlay(intervalDuration = 60)

        assertThat(legend.entries().map { it.label })
            .containsExactly("< 10 min", "< 20 min", "< 30 min", "< 40 min", "< 50 min", "> 50 min")
    }

    @Test
    fun `labels follow the active interval`() {
        val (legend, _) = createOverlay(intervalDuration = 30)

        assertThat(legend.entries().map { it.label })
            .containsExactly("< 5 min", "< 10 min", "< 15 min", "< 20 min", "< 25 min", "> 25 min")
    }

    @Test
    fun `draws the countdown above the legend rows`() {
        val (legend, _) = createOverlay()
        legend.onVisibleAreaChanged(Rect(0, 0, 1000, 900))
        legend.countdown = "40 / 60s"

        val bounds = legend.panelBounds()
        legend.countdown = ""
        val boundsWithoutCountdown = legend.panelBounds()

        assertThat(bounds.height()).isGreaterThan(boundsWithoutCountdown.height())
        assertThat(bounds.bottom).isEqualTo(boundsWithoutCountdown.bottom)
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
    fun `uses the surface bottom instead of the host's reported area`() {
        val (legend, _) = createOverlay()
        legend.surfaceHeight = 1000
        legend.onVisibleAreaChanged(Rect(300, 60, 1000, 900))

        val bounds = legend.panelBounds()

        assertThat(bounds.bottom).isEqualTo(1000f - PANEL_INSET)
    }

    @Test
    fun `falls back to the host's map area bottom when the surface height is unknown`() {
        val (legend, _) = createOverlay()
        legend.onVisibleAreaChanged(Rect(300, 60, 1000, 900))

        val bounds = legend.panelBounds()

        assertThat(bounds.bottom).isEqualTo(900f - PANEL_INSET)
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
