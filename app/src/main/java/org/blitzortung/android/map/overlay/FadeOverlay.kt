package org.blitzortung.android.map.overlay

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect

import com.google.android.maps.MapView
import com.google.android.maps.Overlay

import org.blitzortung.android.map.overlay.color.ColorHandler

class FadeOverlay(private val colorHandler: ColorHandler) : Overlay() {

    private var alphaValue = 0

    override fun draw(canvas: Canvas?, mapView: MapView?, shadow: Boolean) {
        if (!shadow) {
            val rect = canvas!!.clipBounds
            val paint = Paint()
            paint.color = colorHandler.backgroundColor
            paint.alpha = alphaValue
            canvas.drawRect(rect, paint)
        }
    }

    fun setAlpha(alphaValue: Int) {
        this.alphaValue = alphaValue
    }
}
