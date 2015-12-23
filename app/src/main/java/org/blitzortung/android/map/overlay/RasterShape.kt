package org.blitzortung.android.map.overlay

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Paint.Align
import android.graphics.Point
import android.graphics.RectF
import android.graphics.drawable.shapes.Shape

class RasterShape : Shape() {

    private val rect: RectF
    private var color: Int = 0
    private var alpha: Int = 0
    private var multiplicity: Int = 0
    private var textColor: Int = 0

    init {
        rect = RectF()
    }

    override fun draw(canvas: Canvas, paint: Paint) {
        paint.color = color
        paint.alpha = alpha
        canvas.drawRect(rect, paint)

        val textSize = rect.height() / 2.5f
        if (textSize >= 8f) {
            paint.color = textColor
            paint.alpha = 255
            paint.textAlign = Align.CENTER
            paint.textSize = textSize
            canvas.drawText(multiplicity.toString(), 0.0f, 0.4f * textSize, paint)
        }
    }

    private fun setAlphaValue() {
        var value = (rect.width() - 10) / 30
        value = Math.min(Math.max(value, 0.0f), 1.0f)
        alpha = 100 + (155 * (1.0 - value)).toInt()
    }

    override fun hasAlpha(): Boolean {
        return alpha != 255
    }

    fun update(topLeft: Point, bottomRight: Point, color: Int, multiplicity: Int, textColor: Int) {
        val x1 = Math.min(topLeft.x.toFloat(), -MIN_SIZE)
        val y1 = Math.min(topLeft.y.toFloat(), -MIN_SIZE)
        val x2 = Math.max(bottomRight.x.toFloat(), MIN_SIZE)
        val y2 = Math.max(bottomRight.y.toFloat(), MIN_SIZE)
        rect.set(x1, y1, x2, y2)
        resize(rect.width(), rect.height())

        this.multiplicity = multiplicity
        this.color = color
        this.textColor = textColor

        setAlphaValue()
    }

    companion object {

        private val MIN_SIZE = 1.5f
    }
}
