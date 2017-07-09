package org.blitzortung.android.map.overlay

import android.graphics.Canvas
import android.graphics.Paint
import com.google.android.maps.MapView

interface LightningShape {
    fun draw(canvas: Canvas, mapView: MapView, paint: Paint)
}