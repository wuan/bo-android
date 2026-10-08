package org.blitzortung.android.map

import android.view.MotionEvent
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class OwnMapViewTest {

    @Test
    fun `performClick delegates to super and returns true`() {
        val mapView = OwnMapView(RuntimeEnvironment.getApplication())

        assertThat(mapView.performClick()).isTrue()
    }
    @Test
    fun `onTouchEvent handles an ACTION_DOWN gesture`() {
        val mapView = OwnMapView(RuntimeEnvironment.getApplication())
        val down = MotionEvent.obtain(0L, 0L, MotionEvent.ACTION_DOWN, 10f, 10f, 0)

        // Should process the event and delegate to the gesture detector without throwing.
        mapView.onTouchEvent(down)

        down.recycle()
    }

    @Test
    fun `onTouchEvent handles an ACTION_UP gesture`() {
        val mapView = OwnMapView(RuntimeEnvironment.getApplication())
        val up = MotionEvent.obtain(0L, 10L, MotionEvent.ACTION_UP, 10f, 10f, 0)

        // An ACTION_UP must invoke performClick() (accessibility convention).
        mapView.onTouchEvent(up)

        up.recycle()
    }
}
