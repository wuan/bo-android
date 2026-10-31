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

import androidx.preference.PreferenceManager
import androidx.car.app.testing.TestCarContext
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.within
import org.blitzortung.android.map.overlay.color.StrikeColorHandler
import org.junit.Test
import org.junit.runner.RunWith
import org.osmdroid.util.GeoPoint
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class OsmdroidCarMapRendererTest {
    private fun createRenderer(): OsmdroidCarMapRenderer {
        val context = RuntimeEnvironment.getApplication()
        val carContext = TestCarContext.createCarContext(context)
        val colorHandler = StrikeColorHandler(PreferenceManager.getDefaultSharedPreferences(context))
        return OsmdroidCarMapRenderer(carContext, colorHandler)
    }

    @Test
    fun `projects the map centre to the middle of the viewport`() {
        val renderer = createRenderer()
        renderer.mapView.layout(0, 0, VIEWPORT_WIDTH, VIEWPORT_HEIGHT)
        renderer.mapView.controller.setZoom(ZOOM)
        renderer.mapView.controller.setCenter(GeoPoint(CENTER_LATITUDE, CENTER_LONGITUDE))

        val point = renderer.screenPositionOf(CENTER_LATITUDE, CENTER_LONGITUDE)

        assertThat(point.x).isCloseTo(VIEWPORT_WIDTH / 2, within(2))
        assertThat(point.y).isCloseTo(VIEWPORT_HEIGHT / 2, within(2))
    }

    @Test
    fun `reuses the shared strike colour handler`() {
        val context = RuntimeEnvironment.getApplication()
        val carContext = TestCarContext.createCarContext(context)
        val colorHandler = StrikeColorHandler(PreferenceManager.getDefaultSharedPreferences(context))

        val renderer = OsmdroidCarMapRenderer(carContext, colorHandler)

        assertThat(renderer.strikeListOverlay.colorHandler).isSameAs(colorHandler)
    }

    @Test
    fun `projects north of the centre above it`() {
        val renderer = createRenderer()
        renderer.mapView.layout(0, 0, VIEWPORT_WIDTH, VIEWPORT_HEIGHT)
        renderer.mapView.controller.setZoom(ZOOM)
        renderer.mapView.controller.setCenter(GeoPoint(CENTER_LATITUDE, CENTER_LONGITUDE))

        val centre = renderer.screenPositionOf(CENTER_LATITUDE, CENTER_LONGITUDE)
        val north = renderer.screenPositionOf(CENTER_LATITUDE + 1.0, CENTER_LONGITUDE)
        val east = renderer.screenPositionOf(CENTER_LATITUDE, CENTER_LONGITUDE + 1.0)

        assertThat(north.y).isLessThan(centre.y)
        assertThat(east.x).isGreaterThan(centre.x)
    }

    private companion object {
        private const val VIEWPORT_WIDTH = 800
        private const val VIEWPORT_HEIGHT = 480
        private const val ZOOM = 8.0
        private const val CENTER_LATITUDE = 50.0
        private const val CENTER_LONGITUDE = 10.0
    }
}
