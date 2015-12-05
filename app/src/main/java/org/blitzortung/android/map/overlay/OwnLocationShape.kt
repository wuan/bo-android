package org.blitzortung.android.map.overlay

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.drawable.shapes.Shape

class OwnLocationShape(private val size: Float) : Shape() {

    override fun draw(canvas: Canvas, paint: Paint) {
        paint.style = Paint.Style.FILL
        paint.color = -2007673515
        canvas.drawCircle(0.0f, 0.0f, size / 1.3f, paint)

        paint.style = Paint.Style.STROKE
        paint.strokeWidth = size / 4
        paint.color = -1
        canvas.drawLine(-size / 2, 0.0f, size / 2, 0.0f, paint)
        canvas.drawLine(0.0f, -size / 2, 0.0f, size / 2, paint)
        paint.color = -13391105
        canvas.drawCircle(0.0f, 0.0f, size / 1.3f, paint)
    }

}
