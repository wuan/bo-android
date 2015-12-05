package org.blitzortung.android.map.overlay

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.drawable.shapes.Shape

class StrikeShape : Shape() {

    private var size: Float = 0.toFloat()
    private var color: Int = 0

    override fun draw(canvas: Canvas, paint: Paint) {
        paint.color = color
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = size / 4
        canvas.drawLine(-size / 2, 0.0f, size / 2, 0.0f, paint)
        canvas.drawLine(0.0f, -size / 2, 0.0f, size / 2, paint)
    }

    fun update(size: Float, color: Int) {
        this.size = size
        this.color = color
    }
}
