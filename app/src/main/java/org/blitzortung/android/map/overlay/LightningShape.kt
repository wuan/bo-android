package org.blitzortung.android.map.overlay

import android.graphics.Canvas
import android.graphics.Paint
import com.google.android.maps.GeoPoint
import com.google.android.maps.MapView
import com.google.android.maps.Projection

interface LightningShape {
    fun draw(canvas: Canvas, mapView: MapView, paint: Paint)

    fun isPointInside(tappedGeoPoint: GeoPoint, projection: Projection): Boolean
}