package org.blitzortung.android.app.view.alarm

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF

class PrimitiveRenderer {
    private val arcArea = RectF()

    fun drawCross(center: Float, smallRadius: Float, lines: Paint, temporaryCanvas: Canvas) {
        temporaryCanvas.drawLine(center - smallRadius, center, center + smallRadius, center, lines)
        temporaryCanvas.drawLine(center, center - smallRadius, center, center + smallRadius, lines)
    }

    fun drawCircle(
        center: Float,
        radius: Float,
        paint: Paint,
        temporaryCanvas: Canvas
    ) {
        arcArea.set(center - radius, center - radius, center + radius, center + radius)
        temporaryCanvas.drawArc(arcArea, 0f, 360f, false, paint)
    }

    fun drawCenteredText(
        temporaryCanvas: Canvas,
        text: String,
        center: Float,
        textPaint: Paint
    ) {
        val textBound = Rect()
        textPaint.getTextBounds(text, 0, text.length, textBound)
        temporaryCanvas.drawText(text, center - textBound.right / 2, center - textBound.top / 2, textPaint)
    }
}
