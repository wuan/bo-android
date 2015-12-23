package org.blitzortung.android.map.overlay

import android.graphics.Point
import android.graphics.drawable.Drawable
import android.graphics.drawable.ShapeDrawable
import android.graphics.drawable.shapes.Shape
import android.location.Location

import com.google.android.maps.GeoPoint
import com.google.android.maps.OverlayItem
import com.google.android.maps.Projection

import org.blitzortung.android.data.Coordsys
import org.blitzortung.android.data.beans.RasterParameters
import org.blitzortung.android.data.beans.Strike

class StrikeOverlayItem(strike: Strike) : OverlayItem(Coordsys.toMapCoords(strike.longitude, strike.latitude), "", ""), Strike {
    public override val timestamp: Long
    public override val multiplicity: Int

    init {
        super.setMarker(ShapeDrawable())

        timestamp = strike.timestamp
        multiplicity = strike.multiplicity
    }

    public override fun setMarker(drawable: Drawable?) {
        throw IllegalStateException("cannot overwrite marker of strike overlay item")
    }

    private val drawable: ShapeDrawable
        get() {
            return getMarker(0) as ShapeDrawable
        }

    var shape: Shape?
        get() {
            return drawable.shape
        }
        set(shape) {
            drawable.shape = shape
        }

    fun updateShape(rasterParameters: RasterParameters?, projection: Projection, color: Int, textColor: Int, zoomLevel: Int) {
        var shape: Shape? = shape
        if (rasterParameters != null) {
            if (shape == null && shape !is RasterShape) {
                shape = RasterShape()
            }

            val lon_delta = rasterParameters.longitudeDelta / 2.0f * 1e6f
            val lat_delta = rasterParameters.latitudeDelta / 2.0f * 1e6f
            val geoPoint = point
            projection.toPixels(geoPoint, center)
            projection.toPixels(GeoPoint(
                    (geoPoint.latitudeE6 + lat_delta).toInt(),
                    (geoPoint.longitudeE6 - lon_delta).toInt()), topLeft)
            projection.toPixels(GeoPoint(
                    (geoPoint.latitudeE6 - lat_delta).toInt(),
                    (geoPoint.longitudeE6 + lon_delta).toInt()), bottomRight)
            topLeft.offset(-center.x, -center.y)
            bottomRight.offset(-center.x, -center.y)
            if (shape is RasterShape) {
                shape.update(topLeft, bottomRight, color, multiplicity, textColor)
            }
        } else {
            if (shape == null) {
                shape = StrikeShape()
            }
            if (shape is StrikeShape) {
                shape.update((zoomLevel + 1).toFloat(), color)
            }
        }
        this.shape = shape
    }

    public override val longitude: Float
        get() {
            return point.longitudeE6 / 1e6f
        }

    public override val latitude: Float
        get() {
            return point.latitudeE6 / 1e6f
        }

    companion object {
        private val center = Point()
        private val topLeft = Point()
        private val bottomRight = Point()
    }
}
