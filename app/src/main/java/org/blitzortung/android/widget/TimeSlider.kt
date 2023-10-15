package org.blitzortung.android.widget

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatSeekBar
import org.blitzortung.android.app.R
import org.blitzortung.android.data.History
import org.blitzortung.android.data.Parameters
import org.blitzortung.android.util.TabletAwareView

class TimeSlider : AppCompatSeekBar {

    private val timeAxisPaint = Paint()
    private val timeAxisLinePaint = Paint()
    private var pastTimePaint = Paint()
    private var currentTextPaint = Paint()
    private var legendTextPaint = Paint()
    private var pastTextPaint = Paint()

    private val trianglePath = Path()

    init {
        timeAxisPaint.color = Color.LTGRAY
        timeAxisPaint.textSize = 24f
        timeAxisPaint.textAlign = Paint.Align.CENTER

        timeAxisLinePaint.color = Color.LTGRAY
        timeAxisLinePaint.style = Paint.Style.STROKE
        timeAxisLinePaint.strokeWidth = 6f

        pastTimePaint.color = Color.argb(255, 200, 100, 0)

        currentTextPaint.color = Color.LTGRAY
        currentTextPaint.textSize = 32f
        currentTextPaint.textAlign = Paint.Align.RIGHT

        legendTextPaint.color = Color.LTGRAY
        legendTextPaint.textSize = 32f
        legendTextPaint.textAlign = Paint.Align.CENTER

        pastTextPaint.color = Color.LTGRAY
        pastTextPaint.textSize = 32f
        pastTextPaint.textAlign = Paint.Align.LEFT
    }

    private var ticksPerHour: Int? = null

    fun update(parameters: Parameters, history: History) {
        updatePositionAndRange(parameters, history)
        updateSecondaryPosition(parameters, history)

        invalidate()
    }

    private fun updateSecondaryPosition(parameters: Parameters, history: History) {
        secondaryProgress = max - parameters.intervalPosition(history)
    }

    private fun updatePositionAndRange(parameters: Parameters, history: History) {
        ticksPerHour = 60 / history.timeIncrement
        val previousMax = max
        max = parameters.intervalMaxPosition(history)
        if (previousMax == 0 && max > 0) {
            progress = parameters.intervalMaxPosition(history) - parameters.intervalPosition(history)
        }
    }

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    override fun onDraw(c: Canvas) {
        val isTablet = TabletAwareView.isTablet(this.context)
        val border = width / if (isTablet) 60f else 25f
        val base = height / 2f
        if (secondaryProgress >= 0 && max > 0) {
            drawSecondaryProgress(c, base, border)
        }

        super.onDraw(c)

        drawAxis(c, base, border)
        drawAxisTics(c, base, border)

        val bottomOffset = 10f
        c.drawText(context.resources.getString(R.string.slider_current), width - border, height - bottomOffset, currentTextPaint)
        c.drawText(context.resources.getString(R.string.slider_legend), width / 2f, height - bottomOffset, legendTextPaint)
        c.drawText(context.resources.getString(R.string.slider_past), border, height - bottomOffset, pastTextPaint)
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
        val axisWidth = 5f
        c.drawRect(border, base - axisWidth / 2, width - border, base + axisWidth / 2, timeAxisPaint)
        val position = border + ((width - 2 * border) / max) * progress
        c.drawRect(position, base - axisWidth / 2, width - border, base + axisWidth / 2, pastTimePaint)
        c.drawCircle(position, base, 16f, pastTimePaint)
        c.drawCircle(position, base, 16f, timeAxisLinePaint)
    }

    private fun drawAxisTics(c: Canvas, base: Float, border: Float) {
        for (i in 0..max) {
            val position = border + ((width - 2 * border) / max) * i

            val ticksPerHour = this.ticksPerHour
            val tickLength = if (ticksPerHour != null && (max - i) % ticksPerHour == 0) {
                val hour = (max - i) / ticksPerHour
                c.drawText("${hour}", position, base + 38, timeAxisPaint)
                10f
            } else {
                6f
            }

            c.drawRect(position - 1, base - tickLength, position + 1, base + tickLength, timeAxisPaint)
        }
    }

}