package org.blitzortung.android.map.overlay

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.drawable.shapes.Shape

class ParticipantShape : Shape() {

    private val rect: RectF
    private var color: Int = 0

    init {
        rect = RectF()
        color = 0
    }

    override fun draw(canvas: Canvas, paint: Paint) {
        paint.color = color
        paint.alpha = 255
        paint.style = Paint.Style.FILL
        canvas.drawRect(rect, paint)
    }

    fun update(size: Float, color: Int) {
        val halfSize = size / 2f
        rect.set(-halfSize, -halfSize, halfSize, halfSize)
        resize(rect.width(), rect.width())

        this.color = color
    }
}
