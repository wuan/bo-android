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

import java.time.Duration
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadows.ShadowChoreographer

@RunWith(RobolectricTestRunner::class)
class CarMapViewportTest {
    @Before
    fun pauseTheAnimationClock() {
        ShadowChoreographer.setPaused(true)
        ShadowChoreographer.setFrameDelay(Duration.ofMillis(FRAME_MILLIS))
    }

    @After
    fun resumeTheAnimationClock() {
        ShadowChoreographer.setPaused(false)
    }

    @Test
    fun `reflects the map zoom and bounding box`() {
        val mapView = mapView()
        mapView.controller.setZoom(ZOOM)
        mapView.controller.setCenter(GeoPoint(CENTER_LATITUDE, CENTER_LONGITUDE))

        val viewport = CarMapViewport(mapView)

        assertThat(viewport.viewportZoomLevel).isEqualTo(ZOOM)
        assertThat(viewport.viewportBoundingBox).isNotNull
        assertThat(viewport.viewportBoundingBox!!.contains(CENTER_LATITUDE, CENTER_LONGITUDE)).isTrue()
    }

    @Test
    fun `reports an idle to animating to idle camera transition`() {
        val mapView = mapView()
        mapView.controller.setZoom(ZOOM)
        mapView.controller.setCenter(GeoPoint(CENTER_LATITUDE, CENTER_LONGITUDE))
        val viewport = CarMapViewport(mapView)

        assertThat(viewport.viewportIsAnimating).isFalse()
        assertThat(viewport.animator()).isNull()

        mapView.controller.animateTo(GeoPoint(TARGET_LATITUDE, TARGET_LONGITUDE), TARGET_ZOOM, DURATION)

        assertThat(viewport.viewportIsAnimating).isTrue()
        assertThat(viewport.animator()).isNotNull

        advanceFrames(mapView)

        assertThat(viewport.viewportIsAnimating).isFalse()
        assertThat(viewport.animator()).isNull()
    }

    private fun mapView(): MapView =
        MapView(RuntimeEnvironment.getApplication()).apply {
            layout(0, 0, VIEWPORT_WIDTH, VIEWPORT_HEIGHT)
        }

    private fun advanceFrames(mapView: MapView) {
        val looper = shadowOf(mapView.context.mainLooper)
        repeat((DURATION / FRAME_MILLIS).toInt() + 2) {
            looper.idleFor(Duration.ofMillis(FRAME_MILLIS))
        }
    }

    private companion object {
        private const val VIEWPORT_WIDTH = 800
        private const val VIEWPORT_HEIGHT = 480
        private const val ZOOM = 8.0
        private const val TARGET_ZOOM = 6.5
        private const val CENTER_LATITUDE = 50.0
        private const val CENTER_LONGITUDE = 10.0
        private const val TARGET_LATITUDE = 48.0
        private const val TARGET_LONGITUDE = 12.0
        private const val DURATION = 500L
        private const val FRAME_MILLIS = 16L
    }
}
