package org.blitzortung.android.widget

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatSeekBar
import org.blitzortung.android.app.R
import org.blitzortung.android.app.helper.ViewHelper
import org.blitzortung.android.data.History
import org.blitzortung.android.data.Parameters

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
        timeAxisPaint.textSize = 28f
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
        secondaryProgress = parameters.intervalPosition(history)
    }

    private fun updatePositionAndRange(parameters: Parameters, history: History) {
        ticksPerHour = 60 / history.timeIncrement
        val previousMax = max
        max = parameters.intervalMaxPosition(history)
        if (previousMax == 0 && max > 0) {
            progress = parameters.intervalPosition(history)
        }
    }

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    override fun onDraw(c: Canvas) {
        if (secondaryProgress >= 0 && max > 0) {
            drawSecondaryProgress(c)
        }

        super.onDraw(c)

        drawAxis(c)
        drawAxisTics(c)

        val bottomOffset = ViewHelper.pxFromDp(context, 6f)
        c.drawText(
            context.resources.getString(R.string.slider_current),
            width - paddingRight.toFloat(),
            height - bottomOffset,
            currentTextPaint
        )
        c.drawText(
            context.resources.getString(R.string.slider_legend),
            width / 2f,
            height - bottomOffset,
            legendTextPaint
        )
        c.drawText(
            context.resources.getString(R.string.slider_past),
            paddingLeft.toFloat(),
            height - bottomOffset,
            pastTextPaint
        )
    }

    private fun drawSecondaryProgress(c: Canvas) {
        val base: Float  = (height - paddingBottom) /2f

        val distance = ViewHelper.pxFromDp(context, 8f)
        val triangleSize = ViewHelper.pxFromDp(context, 5f)
        val secondPosition = calculateX(secondaryProgress)
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

    private fun drawAxis(c: Canvas) {
        val base: Float  = (height - paddingBottom) /2f

        val axisWidth = ViewHelper.pxFromDp(context, 2f)
        c.drawRect(
            paddingLeft.toFloat(),
            base - axisWidth / 2,
            width - paddingRight.toFloat(),
            base + axisWidth / 2,
            timeAxisPaint
        )
        val position = calculateX(progress)
        c.drawRect(position, base - axisWidth / 2, width - paddingRight.toFloat(), base + axisWidth / 2, pastTimePaint)
        c.drawCircle(position, base, 16f, pastTimePaint)
        c.drawCircle(position, base, 16f, timeAxisLinePaint)
    }

    private fun drawAxisTics(c: Canvas) {
        val base: Float  = (height - paddingBottom) /2f

        val textPosition = base + ViewHelper.pxFromDp(context, 18f)
        val shortTick = ViewHelper.pxFromDp(context, 3f)
        val longTick = ViewHelper.pxFromDp(context, 5f)
        val unit = ViewHelper.pxFromDp(context, 0.5f)
        for (i in 0..max) {
            val position = calculateX(i)

            val ticksPerHour = this.ticksPerHour
            val tickLength = if (ticksPerHour != null && (max - i) % ticksPerHour == 0) {
                val hour = (max - i) / ticksPerHour
                c.drawText("$hour", position, textPosition, timeAxisPaint)
                longTick
            } else {
                shortTick
            }

            c.drawRect(position - unit, base - tickLength, position + unit, base + tickLength, timeAxisPaint)
        }
    }

    private fun calculateX(progress: Int): Float {
        return paddingLeft.toFloat() + (width - (paddingLeft.toFloat() + paddingRight.toFloat())) / max * progress
    }

}