package org.blitzortung.android.map.overlay

import android.graphics.Canvas
import android.graphics.Point
import android.graphics.Rect
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.blitzortung.android.data.Parameters
import org.blitzortung.android.data.beans.GridParameters
import org.blitzortung.android.map.overlay.color.StrikeColorHandler
import org.junit.Test
import org.junit.runner.RunWith
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.Projection
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class StrikeListOverlayTest {
    private val colorHandler = mockk<StrikeColorHandler>(relaxed = true)
    private val mapView = mockk<MapView>()
    private val projection = mockk<Projection>()
    private val canvas = mockk<Canvas>(relaxed = true)

    init {
        every { mapView.zoomLevelDouble } returns 5.0
        every { mapView.projection } returns projection
        every { canvas.clipBounds } returns Rect(0, 0, 1000, 1000)
        every { projection.toPixels(any(), any()) } answers {
            val geoPoint = firstArg<GeoPoint>()
            val point = secondArg<Point>()
            point.x = (geoPoint.longitude * 10).toInt()
            point.y = (geoPoint.latitude * 10).toInt()
            point
        }
    }

    @Test
    fun `draws the data-area outline by default`() {
        val overlay = localDataOverlay(showDataArea = true)

        overlay.draw(canvas, mapView, false)

        verify(exactly = 4) { canvas.drawLine(any(), any(), any(), any(), any()) }
    }

    @Test
    fun `skips the data-area outline when the option is off`() {
        val overlay = localDataOverlay(showDataArea = false)

        overlay.draw(canvas, mapView, false)

        verify(exactly = 0) { canvas.drawLine(any(), any(), any(), any(), any()) }
    }

    @Test
    fun `keeps the data-area outline enabled by default`() {
        val overlay = StrikeListOverlay(mapView, colorHandler)

        assertThat(overlay.showDataArea).isTrue()
    }

    private fun localDataOverlay(showDataArea: Boolean): StrikeListOverlay =
        StrikeListOverlay(mapView, colorHandler, showDataArea = showDataArea).apply {
            gridParameters = localGridParameters()
            parameters = Parameters(region = LOCAL_REGION_FOR_TEST)
        }

    /** A local grid area whose outline falls inside the mocked canvas clip bounds. */
    private fun localGridParameters(): GridParameters =
        GridParameters(
            longitudeStart = 10.0,
            latitudeStart = 50.0,
            longitudeDelta = 1.0,
            latitudeDelta = 1.0,
            longitudeBins = 2,
            latitudeBins = 2,
        )

    private companion object {
        private const val LOCAL_REGION_FOR_TEST = -1
    }
}
