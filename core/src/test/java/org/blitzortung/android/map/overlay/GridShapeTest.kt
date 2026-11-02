package org.blitzortung.android.map.overlay

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Point
import android.graphics.RectF
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test
import org.junit.runner.RunWith
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.Projection
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class GridShapeTest {
    private val canvas = mockk<Canvas>(relaxed = true)
    private val mapView = mockk<MapView>()
    private val projection = mockk<Projection>()

    @Test
    @Suppress("DEPRECATION")
    fun drawRendersRectangleAndMultiplicityLabel() {
        every { mapView.projection } returns projection
        every { projection.toPixels(any(), any()) } answers {
            secondArg<Point>().apply {
                x = 0
                y = 0
            }
        }
        every { canvas.quickReject(any<RectF>(), any()) } returns false

        val shape = GridShape(GeoPoint(51.0, 11.0))
        shape.update(Point(0, 0), Point(40, 40), color = 0xFF0000, multiplicity = 5, textColor = 0)

        shape.draw(canvas, mapView, Paint())

        verify(exactly = 1) { canvas.drawRect(any<RectF>(), any()) }
        verify(exactly = 1) { canvas.drawText("5", any(), any(), any()) }
    }

    @Test
    @Suppress("DEPRECATION")
    fun drawSkipsRejectedShapes() {
        every { mapView.projection } returns projection
        every { projection.toPixels(any(), any()) } answers { secondArg<Point>() }
        every { canvas.quickReject(any<RectF>(), any()) } returns true

        val shape = GridShape(GeoPoint(51.0, 11.0))
        shape.update(Point(0, 0), Point(40, 40), color = 0xFF0000, multiplicity = 5, textColor = 0)

        shape.draw(canvas, mapView, Paint())

        verify(exactly = 0) { canvas.drawRect(any<RectF>(), any()) }
    }
}
