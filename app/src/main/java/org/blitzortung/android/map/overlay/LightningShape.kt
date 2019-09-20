package org.blitzortung.android.map.overlay

import android.graphics.Canvas
import android.graphics.Paint
import org.osmdroid.api.IGeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.Projection

interface LightningShape {
    fun draw(canvas: Canvas, mapView: MapView, paint: Paint)

    fun isPointInside(tappedGeoPoint: IGeoPoint, projection: Projection): Boolean
}