package org.blitzortung.android.map.overlay

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Point
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.Projection
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ClusterShapeTest {
    private val canvas = mockk<Canvas>(relaxed = true)
    private val mapView = mockk<MapView>()
    private val projection = mockk<Projection>()

    init {
        every { mapView.projection } returns projection
        every { mapView.zoomLevelDouble } returns 5.0
        every { projection.worldMapSize } returns 720.0
        every { projection.toPixels(any(), any()) } answers {
            val geoPoint = firstArg<GeoPoint>()
            val point = secondArg<Point>()
            point.x = (geoPoint.longitude * 1000).toInt()
            point.y = (geoPoint.latitude * 1000).toInt()
            point
        }
    }

    @Test
    fun wrapPixelXShiftsVertexAcrossTheDateLineIntoTheSameWorldCopy() {
        // world width 720px; a vertex projected to the far left (x = 4) while the
        // previous vertex sits near the right edge (x = 716) is a date line
        // crossing: it must be shifted one world to the right instead of drawing a
        // line across the whole map.
        assertThat(wrapPixelX(x = 4.0, referenceX = 716.0, worldSize = 720.0)).isEqualTo(724.0)
    }

    @Test
    fun wrapPixelXLeavesNearbyVertexUnchanged() {
        assertThat(wrapPixelX(x = 718.0, referenceX = 716.0, worldSize = 720.0)).isEqualTo(718.0)
    }

    @Test
    fun wrapPixelXShiftsVertexLeftWhenItIsMoreThanHalfAWorldAhead() {
        assertThat(wrapPixelX(x = 1420.0, referenceX = 716.0, worldSize = 720.0)).isEqualTo(700.0)
    }

    @Test
    fun wrapPixelXReturnsInputWhenWorldSizeIsUnknown() {
        assertThat(wrapPixelX(x = 4.0, referenceX = 716.0, worldSize = 0.0)).isEqualTo(4.0)
    }

    @Test
    fun drawDoesNothingForDegenerateShape() {
        val shape = ClusterShape(listOf(11.0 to 51.0))

        shape.draw(canvas, mapView, Paint(), 3f)

        verify(exactly = 0) { canvas.drawPath(any(), any()) }
    }

    @Test
    fun drawStrokesOlderClusterWithoutFilling() {
        val shape = ClusterShape(listOf(11.0 to 51.0, 11.1 to 51.0, 11.1 to 51.1))
        shape.update(color = 0xFF0000, textColor = 0xFFFFFF, strikeCount = 5, current = false)

        shape.draw(canvas, mapView, Paint(), 3f)

        verify(exactly = 1) { canvas.drawPath(any(), any()) }
    }

    @Test
    fun drawFillsAndStrokesCurrentCluster() {
        val shape = ClusterShape(listOf(11.0 to 51.0, 11.1 to 51.0, 11.1 to 51.1))
        shape.update(color = 0xFF0000, textColor = 0xFFFFFF, strikeCount = 5, current = true)

        shape.draw(canvas, mapView, Paint(), 3f)

        // one fill pass + one stroke pass, no label below the label zoom level
        verify(exactly = 2) { canvas.drawPath(any(), any()) }
        verify(exactly = 0) { canvas.drawText(any(), any(), any(), any()) }
    }

    @Test
    fun drawAddsStrikeCountLabelForCurrentClusterAtHighZoom() {
        every { mapView.zoomLevelDouble } returns 10.0
        val shape = ClusterShape(listOf(11.0 to 51.0, 11.1 to 51.0, 11.1 to 51.1, 11.0 to 51.0))
        shape.update(color = 0xFF0000, textColor = 0xFFFFFF, strikeCount = 7, current = true)

        shape.draw(canvas, mapView, Paint(), 3f)

        verify(exactly = 1) { canvas.drawText("7", any(), any(), any()) }
    }
}
