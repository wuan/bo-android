package org.blitzortung.android.widget

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.util.Log
import androidx.appcompat.widget.AppCompatSeekBar
import org.blitzortung.android.app.Main.Companion.LOG_TAG
import org.blitzortung.android.data.Parameters

class SeekBar : AppCompatSeekBar {

    private val timeAxisPaint = Paint()
    private val trianglePath = Path()

    init {
        timeAxisPaint.color = Color.LTGRAY
        timeAxisPaint.textSize = 24f
        timeAxisPaint.textAlign = Paint.Align.CENTER
    }

    private var ticksPerHour: Int? = null

    fun update(parameters: Parameters) {
        updatePositionAndRange(parameters)
        updateSecondaryPosition(parameters)
        
        Log.d(LOG_TAG, "update TimeSlider: position ${progress}, max: ${max}")
        invalidate()
    }

    private fun updateSecondaryPosition(parameters: Parameters) {
        secondaryProgress = max - parameters.intervalPosition
    }

    private fun updatePositionAndRange(parameters: Parameters) {
        ticksPerHour = 60 / parameters.timeIncrement
        val previousMax = max
        max = parameters.intervalMaxPosition
        if (previousMax == 0 && max > 0) {
            progress = parameters.intervalMaxPosition - parameters.intervalPosition
        }
    }

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    override fun onDraw(c: Canvas) {
        val border = 43f
        val base = height / 2f
        if (secondaryProgress >= 0 && max > 0) {
            drawSecondaryProgress(c, base, border)
        }

        drawAxis(c, base, border)

        super.onDraw(c)
    }

    private fun drawSecondaryProgress(c: Canvas, base: Float, border: Float) {
        val distance = 20f
        val triangleSize = 10f
        val secondPosition = border + ((width - 2 * border) / max) * secondaryProgress
        trianglePath.reset()
        c.drawPath(
            trianglePath.apply {
                moveTo(secondPosition - triangleSize, base - distance - 2 * triangleSize)
                lineTo(secondPosition + triangleSize, base - distance - 2 * triangleSize)
                lineTo(secondPosition, base - distance)
                close()
            },
            timeAxisPaint
        )
    }

    private fun drawAxis(c: Canvas, base: Float, border: Float) {
        for (i in 0..max) {
            val position = border + ((width - 2 * border) / max) * i

            val ticksPerHour = this.ticksPerHour
            val tickLength = if (ticksPerHour != null && (max - i) % ticksPerHour == 0) {
                val hour = (max - i) / ticksPerHour
                c.drawText("${hour}", position, base + 38, timeAxisPaint)
                14f
            } else {
                8f
            }

            c.drawRect(position - 1, base - tickLength, position + 1, base + tickLength, timeAxisPaint)
        }
    }

}