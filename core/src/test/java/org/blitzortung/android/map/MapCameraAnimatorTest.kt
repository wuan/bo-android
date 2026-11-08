package org.blitzortung.android.map

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
class MapCameraAnimatorTest {
    @Before
    fun pauseTheAnimationClock() {
        ShadowChoreographer.setPaused(true)
        ShadowChoreographer.setFrameDelay(Duration.ofMillis(16))
    }

    @After
    fun resumeTheAnimationClock() {
        ShadowChoreographer.setPaused(false)
    }

    @Test
    fun `returns null while the map is idle`() {
        assertThat(MapCameraAnimator.current(mapView())).isNull()
    }

    @Test
    fun `returns a live animator between start and end`() {
        val mapView = mapView()
        mapView.controller.setZoom(START_ZOOM)
        mapView.controller.setCenter(GeoPoint(START_LATITUDE, START_LONGITUDE))

        mapView.controller.animateTo(GeoPoint(TARGET_LATITUDE, TARGET_LONGITUDE), TARGET_ZOOM, DURATION)

        assertThat(mapView.isAnimating).isTrue()
        assertThat(MapCameraAnimator.current(mapView)).isNotNull

        advanceFrames(mapView)

        assertThat(mapView.isAnimating).isFalse()
        assertThat(MapCameraAnimator.current(mapView)).isNull()
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
        private const val START_ZOOM = 4.0
        private const val TARGET_ZOOM = 9.0
        private const val START_LATITUDE = 50.0
        private const val START_LONGITUDE = 10.0
        private const val TARGET_LATITUDE = 48.0
        private const val TARGET_LONGITUDE = 12.0
        private const val DURATION = 500L
        private const val FRAME_MILLIS = 16L
    }
}
