package org.blitzortung.android.map.overlay

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Point
import org.blitzortung.android.data.Coordsys
import org.blitzortung.android.data.beans.RasterParameters
import org.blitzortung.android.data.beans.Strike
import org.osmdroid.api.IGeoPoint
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.Projection

class StrikeOverlay(strike: Strike) {
    val timestamp: Long = strike.timestamp
    val multiplicity = strike.multiplicity
    val center: GeoPoint = Coordsys.toMapCoords(strike.longitude, strike.latitude)

    var shape: LightningShape? = null

    fun draw(canvas: Canvas, mapView: MapView, paint: Paint) {
        shape?.run {
            draw(canvas, mapView, paint)
        }
    }

    fun updateShape(rasterParameters: RasterParameters?, projection: Projection, color: Int, textColor: Int, zoomLevel: Double) {
        var shape: LightningShape? = shape
        if (rasterParameters != null) {
            if (shape !is RasterShape) {
                shape = RasterShape(center)
            }

            val lonDelta = rasterParameters.longitudeDelta / 2.0f
            val latDelta = rasterParameters.latitudeDelta / 2.0f

            projection.toPixels(center, centerPoint)
            projection.toPixels(GeoPoint(
                    center.latitude + latDelta,
                    center.longitude - lonDelta), topLeft)
            projection.toPixels(GeoPoint(
                    center.latitude - latDelta,
                    center.longitude + lonDelta), bottomRight)
            topLeft.offset(-centerPoint.x, -centerPoint.y)
            bottomRight.offset(-centerPoint.x, -centerPoint.y)
            if (shape is RasterShape) {
                shape.update(topLeft, bottomRight, color, multiplicity, textColor)
            }
        } else {
            if (shape == null) {
                shape = StrikeShape(center)
            }
            if (shape is StrikeShape) {
                shape.update(2 * (zoomLevel + 1.0).toFloat(), color)
            }
        }
        this.shape = shape
    }

    fun pointIsInside(point: IGeoPoint, projection: Projection): Boolean {
        return shape?.isPointInside(point, projection)
                ?: false
    }

    companion object {
        private val centerPoint: Point = Point()
        private val topLeft: Point = Point()
        private val bottomRight: Point = Point()
    }
}