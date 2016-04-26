/*

   Copyright 2015 Andreas Würl

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.

*/

package org.blitzortung.android.app.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.util.Log
import android.view.View
import org.blitzortung.android.app.Main
import org.blitzortung.android.app.R
import org.blitzortung.android.data.provider.result.DataEvent
import org.blitzortung.android.map.overlay.StrikesOverlay
import org.blitzortung.android.util.TabletAwareView

class HistogramView @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyle: Int = 0
) : TabletAwareView(context, attrs, defStyle) {

    private val backgroundPaint: Paint
    private val foregroundPaint: Paint
    private val textPaint: Paint
    private val defaultForegroundColor: Int
    private val backgroundRect: RectF
    private var strikesOverlay: StrikesOverlay? = null
    private var histogram: IntArray? = null

    val dataConsumer = { event: DataEvent ->
        updateHistogram(event)
    }

    init {
        foregroundPaint = Paint(Paint.ANTI_ALIAS_FLAG)

        backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        backgroundPaint.color = context.resources.getColor(R.color.translucent_background)

        defaultForegroundColor = context.resources.getColor(R.color.text_foreground)
        textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = defaultForegroundColor
            textSize = this@HistogramView.textSize
            textAlign = Paint.Align.RIGHT
        }

        backgroundRect = RectF()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val getSize = fun(spec: Int) = View.MeasureSpec.getSize(spec)

        val parentWidth = getSize(widthMeasureSpec) * sizeFactor
        val parentHeight = getSize(heightMeasureSpec) * sizeFactor

        super.onMeasure(View.MeasureSpec.makeMeasureSpec(parentWidth.toInt(), View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(parentHeight.toInt(), View.MeasureSpec.EXACTLY))
    }

    override fun onDraw(canvas: Canvas) {
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
            val xd = (width - 2 * padding) / (histogram.size - 1)

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

    private fun updateHistogram(dataEvent: DataEvent) {
        if (dataEvent.failed) {
            visibility = View.INVISIBLE
            histogram = null
        } else {
            val histogram = dataEvent.histogram

            var viewShouldBeVisible = histogram != null && histogram.size > 0

            this.histogram = histogram

            if (!viewShouldBeVisible) {
                viewShouldBeVisible = createHistogram(dataEvent)
            }

            visibility = if (viewShouldBeVisible) View.VISIBLE else View.INVISIBLE

            if (viewShouldBeVisible) {
                invalidate()
            }
        }
    }

    private fun createHistogram(data: DataEvent): Boolean {
        data.parameters?.let { parameters ->
            if (data.totalStrikes == null) {
                return false
            }

            Log.v(Main.LOG_TAG, "HistogramView create histogram from ${data.totalStrikes.size} total strikes")
            val referenceTime = data.referenceTime

            val binInterval = 5
            val binCount = parameters.intervalDuration / binInterval
            val histogram = IntArray(binCount)

            data.totalStrikes.forEach { strike ->
                val binIndex = (binCount - 1) - ((referenceTime - strike.timestamp) / 1000 / 60 / binInterval).toInt()
                if (binIndex in 0 .. binCount - 1)
                    histogram[binIndex]++
            }
            this.histogram = histogram
            return true
        }
        return false
    }
}

data class Strike(val timestamp: Long)
