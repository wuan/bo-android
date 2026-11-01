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

import android.location.Location
import androidx.car.app.testing.TestCarContext
import androidx.preference.PreferenceManager
import kotlin.math.abs
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.within
import org.blitzortung.android.location.LocationUpdate
import org.blitzortung.android.location.NoLocation
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

    @Test
    fun `frames the map on the first location fix`() {
        val renderer = createRenderer()
        renderer.mapView.layout(0, 0, VIEWPORT_WIDTH, VIEWPORT_HEIGHT)

        renderer.onLocationEvent(fix(FIX_LATITUDE, FIX_LONGITUDE))

        assertCentredOn(renderer, FIX_LATITUDE, FIX_LONGITUDE)
        assertThat(renderer.mapView.zoomLevelDouble).isEqualTo(OsmdroidCarMapRenderer.DEFAULT_ZOOM_LEVEL)
    }

    @Test
    fun `does not reframe after the map has framed the location`() {
        val renderer = createRenderer()
        renderer.mapView.layout(0, 0, VIEWPORT_WIDTH, VIEWPORT_HEIGHT)
        renderer.onLocationEvent(fix(FIX_LATITUDE, FIX_LONGITUDE))
        renderer.onScroll(-SCROLL_DISTANCE, -SCROLL_DISTANCE)

        val centerAfterScroll = GeoPoint(renderer.mapView.mapCenter.latitude, renderer.mapView.mapCenter.longitude)
        val zoomAfterScroll = renderer.mapView.zoomLevelDouble

        renderer.onLocationEvent(fix(FIX_LATITUDE + 5.0, FIX_LONGITUDE + 5.0))

        assertThat(renderer.mapView.mapCenter.latitude).isCloseTo(centerAfterScroll.latitude, within(0.001))
        assertThat(renderer.mapView.mapCenter.longitude).isCloseTo(centerAfterScroll.longitude, within(0.001))
        assertThat(renderer.mapView.zoomLevelDouble).isEqualTo(zoomAfterScroll)
    }

    @Test
    fun `frames only once even without user interaction`() {
        val renderer = createRenderer()
        renderer.mapView.layout(0, 0, VIEWPORT_WIDTH, VIEWPORT_HEIGHT)
        renderer.onLocationEvent(fix(FIX_LATITUDE, FIX_LONGITUDE))

        renderer.onLocationEvent(fix(FIX_LATITUDE + 5.0, FIX_LONGITUDE + 5.0))

        assertCentredOn(renderer, FIX_LATITUDE, FIX_LONGITUDE)
    }

    @Test
    fun `does not frame when the user moved the map before the first fix`() {
        val renderer = createRenderer()
        renderer.mapView.layout(0, 0, VIEWPORT_WIDTH, VIEWPORT_HEIGHT)
        renderer.mapView.controller.setZoom(INITIAL_ZOOM)
        renderer.mapView.controller.setCenter(GeoPoint(INITIAL_LATITUDE, INITIAL_LONGITUDE))
        renderer.onScroll(-SCROLL_DISTANCE, -SCROLL_DISTANCE)

        val centerBefore = GeoPoint(renderer.mapView.mapCenter.latitude, renderer.mapView.mapCenter.longitude)

        renderer.onLocationEvent(fix(FIX_LATITUDE, FIX_LONGITUDE))

        assertThat(renderer.mapView.mapCenter.latitude).isCloseTo(centerBefore.latitude, within(0.001))
        assertThat(renderer.mapView.mapCenter.longitude).isCloseTo(centerBefore.longitude, within(0.001))
        assertThat(renderer.mapView.zoomLevelDouble).isEqualTo(INITIAL_ZOOM)
    }

    @Test
    fun `does not frame when the action strip zoom is used before the first fix`() {
        val renderer = createRenderer()
        renderer.mapView.layout(0, 0, VIEWPORT_WIDTH, VIEWPORT_HEIGHT)
        renderer.mapView.controller.setCenter(GeoPoint(INITIAL_LATITUDE, INITIAL_LONGITUDE))
        renderer.zoomIn()

        renderer.onLocationEvent(fix(FIX_LATITUDE, FIX_LONGITUDE))

        assertNotCentredOn(renderer, FIX_LATITUDE, FIX_LONGITUDE)
    }

    @Test
    fun `leaves the camera unchanged when no location is available`() {
        val renderer = createRenderer()
        renderer.mapView.layout(0, 0, VIEWPORT_WIDTH, VIEWPORT_HEIGHT)
        renderer.mapView.controller.setZoom(INITIAL_ZOOM)
        renderer.mapView.controller.setCenter(GeoPoint(INITIAL_LATITUDE, INITIAL_LONGITUDE))

        renderer.onLocationEvent(NoLocation)

        assertThat(renderer.mapView.zoomLevelDouble).isEqualTo(INITIAL_ZOOM)
        assertCentredOn(renderer, INITIAL_LATITUDE, INITIAL_LONGITUDE)
    }

    @Test
    fun `frames on the first fix that follows a no-location event`() {
        val renderer = createRenderer()
        renderer.mapView.layout(0, 0, VIEWPORT_WIDTH, VIEWPORT_HEIGHT)
        renderer.onLocationEvent(NoLocation)

        renderer.onLocationEvent(fix(FIX_LATITUDE, FIX_LONGITUDE))

        assertCentredOn(renderer, FIX_LATITUDE, FIX_LONGITUDE)
        assertThat(renderer.mapView.zoomLevelDouble).isEqualTo(OsmdroidCarMapRenderer.DEFAULT_ZOOM_LEVEL)
    }

    private fun assertCentredOn(
        renderer: OsmdroidCarMapRenderer,
        latitude: Double,
        longitude: Double,
    ) {
        val point = renderer.screenPositionOf(latitude, longitude)
        assertThat(point.x).isCloseTo(VIEWPORT_WIDTH / 2, within(2))
        assertThat(point.y).isCloseTo(VIEWPORT_HEIGHT / 2, within(2))
    }

    private fun assertNotCentredOn(
        renderer: OsmdroidCarMapRenderer,
        latitude: Double,
        longitude: Double,
    ) {
        val point = renderer.screenPositionOf(latitude, longitude)
        val offCentre =
            abs(point.x - VIEWPORT_WIDTH / 2) > 5 || abs(point.y - VIEWPORT_HEIGHT / 2) > 5
        assertThat(offCentre).describedAs("coordinate should not be framed at the viewport centre").isTrue()
    }

    private fun fix(
        latitude: Double,
        longitude: Double,
    ): LocationUpdate =
        LocationUpdate(
            Location("test").apply {
                this.latitude = latitude
                this.longitude = longitude
            },
        )

    private companion object {
        private const val VIEWPORT_WIDTH = 800
        private const val VIEWPORT_HEIGHT = 480
        private const val ZOOM = 8.0
        private const val CENTER_LATITUDE = 50.0
        private const val CENTER_LONGITUDE = 10.0
        private const val FIX_LATITUDE = 48.5
        private const val FIX_LONGITUDE = 9.0
        private const val INITIAL_ZOOM = 4.0
        private const val INITIAL_LATITUDE = 52.0
        private const val INITIAL_LONGITUDE = 13.0
        private const val SCROLL_DISTANCE = 50f
    }
}
