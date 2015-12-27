package org.blitzortung.android.app.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import org.blitzortung.android.app.R
import org.blitzortung.android.data.provider.result.ResultEvent
import org.blitzortung.android.map.overlay.StrikesOverlay
import org.blitzortung.android.protocol.Event
import org.blitzortung.android.util.TabletAwareView

class HistogramView(context: Context, attrs: AttributeSet?, defStyle: Int) : TabletAwareView(context, attrs, defStyle) {

    private val backgroundPaint: Paint
    private val foregroundPaint: Paint
    private val textPaint: Paint
    private val defaultForegroundColor: Int
    private val backgroundRect: RectF
    private var strikesOverlay: StrikesOverlay? = null
    private var histogram: IntArray? = null
    val dataConsumer = { event: Event ->
        if (event is ResultEvent) {
            updateHistogram(event)
        }
    }

    @SuppressWarnings("unused")
    constructor(context: Context, attrs: AttributeSet) : this(context, attrs, 0) {
    }

    @SuppressWarnings("unused")
    constructor(context: Context) : this(context, null, 0) {
    }

    init {
        foregroundPaint = Paint(Paint.ANTI_ALIAS_FLAG)

        backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        backgroundPaint.color = context.resources.getColor(R.color.translucent_background)

        defaultForegroundColor = context.resources.getColor(R.color.text_foreground)
        textPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        textPaint.color = defaultForegroundColor
        textPaint.textSize = textSize
        textPaint.textAlign = Paint.Align.RIGHT

        backgroundRect = RectF()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val parentWidth = View.MeasureSpec.getSize(widthMeasureSpec) * sizeFactor
        val parentHeight = View.MeasureSpec.getSize(heightMeasureSpec) * sizeFactor

        super.onMeasure(View.MeasureSpec.makeMeasureSpec(parentWidth.toInt(), View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(parentHeight.toInt(), View.MeasureSpec.EXACTLY))
    }

    public override fun onDraw(canvas: Canvas) {
        val strikesOverlay = strikesOverlay
        val histogram = histogram
        if (strikesOverlay != null && histogram != null && histogram.size > 0) {
            val colorHandler = strikesOverlay.getColorHandler()
            val minutesPerColor = strikesOverlay.parameters.intervalDuration / colorHandler.numberOfColors
            val minutesPerBin = 5
            val ratio = minutesPerColor / minutesPerBin
            if (ratio == 0) {
                return
            }

            backgroundRect.set(0f, 0f, width.toFloat(), height.toFloat())
            canvas.drawRect(backgroundRect, backgroundPaint)

            val maximumCount = histogram.max() ?: 0

            canvas.drawText("%.1f/min _".format(maximumCount.toFloat() / minutesPerBin), width - 2 * padding, padding + textSize / 1.2f, textPaint)

            val ymax = if (maximumCount == 0) 1 else maximumCount

            val x0 = padding
            val xd = (width - 2 * padding) / (histogram!!.size - 1)

            val y0 = height - padding
            val yd = (height - 2 * padding - textSize) / ymax

            foregroundPaint.strokeWidth = 2f
            for (i in 0..histogram.size - 1 - 1) {
                foregroundPaint.color = colorHandler.getColor((histogram.size - 1 - i) / ratio)
                canvas.drawLine(x0 + xd * i, y0 - yd * histogram[i], x0 + xd * (i + 1), y0 - yd * histogram[i + 1], foregroundPaint)
            }

            foregroundPaint.strokeWidth = 1f
            foregroundPaint.color = defaultForegroundColor

            canvas.drawLine(padding, height - padding, width - padding, height - padding, foregroundPaint)
            canvas.drawLine(width - padding, padding, width - padding, height - padding, foregroundPaint)
        }
    }

    fun setStrikesOverlay(strikesOverlay: StrikesOverlay) {
        this.strikesOverlay = strikesOverlay
    }

    private fun updateHistogram(dataEvent: ResultEvent) {
        if (dataEvent.failed) {
            visibility = View.INVISIBLE
        } else {
            histogram = dataEvent.histogram

            val viewShouldBeVisible = histogram != null && histogram!!.size > 0

            visibility = if (viewShouldBeVisible) View.VISIBLE else View.INVISIBLE

            if (viewShouldBeVisible) {
                invalidate()
            }
        }
    }

    fun clearHistogram() {
        histogram = IntArray(0)

        visibility = View.INVISIBLE
    }

}
