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
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import org.blitzortung.android.app.R
import org.blitzortung.android.map.overlay.StrikesOverlay
import org.blitzortung.android.util.TabletAwareView

class LegendView(context: Context, attrs: AttributeSet?, defStyle: Int) : TabletAwareView(context, attrs, defStyle) {
    private val colorFieldSize: Float
    private val textPaint: Paint
    private val rasterTextPaint: Paint
    private val regionTextPaint: Paint
    private val countThresholdTextPaint: Paint
    private val backgroundPaint: Paint
    private val foregroundPaint: Paint
    private val backgroundRect: RectF
    private val legendColorRect: RectF
    var strikesOverlay: StrikesOverlay? = null

    @SuppressWarnings("unused")
    constructor(context: Context, attrs: AttributeSet) : this(context, attrs, 0) {
    }

    @SuppressWarnings("unused")
    constructor(context: Context) : this(context, null, 0) {
    }

    init {
        colorFieldSize = textSize

        foregroundPaint = Paint(Paint.ANTI_ALIAS_FLAG)

        backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = context.resources.getColor(R.color.translucent_background)
        }

        backgroundRect = RectF()
        legendColorRect = RectF()

        textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = -1
            textSize = textSize
        }

        rasterTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = -1
            textSize = textSize * RASTER_HEIGHT
            textAlign = Paint.Align.CENTER
        }

        regionTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = -1
            textSize = textSize * REGION_HEIGHT
            textAlign = Paint.Align.CENTER
        }

        countThresholdTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = -1
            textSize = textSize * COUNT_THRESHOLD_HEIGHT
            textAlign = Paint.Align.CENTER
        }

        setBackgroundColor(Color.TRANSPARENT)
    }

    private fun determineWidth(intervalDuration: Int): Float {
        var innerWidth = colorFieldSize + padding + textPaint.measureText(if (intervalDuration > 100) "< 100min" else "< 10min").toFloat()

        if (hasRegion()) {
            innerWidth = Math.max(innerWidth, regionTextPaint.measureText(regionName).toFloat())
        }

        return padding + innerWidth + padding
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val getSize = fun (spec: Int) = View.MeasureSpec.getSize(spec)

        val parentWidth = getSize(widthMeasureSpec)
        val parentHeight = getSize(heightMeasureSpec)

        val width = Math.min(determineWidth(strikesOverlay?.parameters?.intervalDuration ?: 0), parentWidth.toFloat())

        val colorHandler = strikesOverlay!!.getColorHandler()

        var height = Math.min((colorFieldSize + padding) * colorHandler.colors.size + padding, parentHeight.toFloat())

        if (hasRegion()) {
            height += colorFieldSize * REGION_HEIGHT + padding
        }

        if (hasRaster()) {
            height += colorFieldSize * RASTER_HEIGHT + padding

            if (hasCountThreshold()) {
                height += colorFieldSize * COUNT_THRESHOLD_HEIGHT + padding
            }
        }

        super.onMeasure(View.MeasureSpec.makeMeasureSpec(width.toInt(), View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(height.toInt(), View.MeasureSpec.EXACTLY))
    }

    override fun onDraw(canvas: Canvas) {
        strikesOverlay?.let { strikesOverlay ->
            val colorHandler = strikesOverlay.getColorHandler()
            val minutesPerColor = strikesOverlay.parameters.intervalDuration / colorHandler.numberOfColors

            backgroundRect.set(0f, 0f, width.toFloat(), height.toFloat())
            canvas.drawRect(backgroundRect, backgroundPaint)

            val numberOfColors = colorHandler.numberOfColors

            var topCoordinate = padding

            for (index in 0..numberOfColors - 1) {
                foregroundPaint.color = colorHandler.getColor(index)
                legendColorRect.set(padding, topCoordinate, padding + colorFieldSize, topCoordinate + colorFieldSize)
                canvas.drawRect(legendColorRect, foregroundPaint)

                val isLastValue = index == numberOfColors - 1
                val text = "%c %dmin".format(if (isLastValue) '>' else '<', (index + (if (isLastValue) 0 else 1)) * minutesPerColor)

                canvas.drawText(text, 2 * padding + colorFieldSize, topCoordinate + colorFieldSize / 1.1f, textPaint)

                topCoordinate += colorFieldSize + padding
            }

            if (hasRegion()) {
                canvas.drawText(regionName, width / 2.0f, topCoordinate + colorFieldSize * REGION_HEIGHT / 1.1f, regionTextPaint)
                topCoordinate += colorFieldSize * REGION_HEIGHT + padding
            }

            if (hasRaster()) {
                canvas.drawText("Raster: " + rasterString, width / 2.0f, topCoordinate + colorFieldSize * RASTER_HEIGHT / 1.1f, rasterTextPaint)
                topCoordinate += colorFieldSize * RASTER_HEIGHT + padding

                if (hasCountThreshold()) {
                    val countThreshold = strikesOverlay.parameters.countThreshold
                    canvas.drawText("# > " + countThreshold, width / 2.0f, topCoordinate + colorFieldSize * COUNT_THRESHOLD_HEIGHT / 1.1f, countThresholdTextPaint)
                    topCoordinate += colorFieldSize * COUNT_THRESHOLD_HEIGHT + padding
                }
            }
        }
    }

    private val regionName: String
        get() {
            val regionNumber = strikesOverlay!!.parameters.region

            var index = 0
            for (region_number in resources.getStringArray(R.array.regions_values)) {
                if (regionNumber == Integer.parseInt(region_number)) {
                    return resources.getStringArray(R.array.regions)[index]
                }
                index++
            }

            return "n/a"
        }

    fun setAlpha(alpha: Int) {
        foregroundPaint.alpha = alpha
    }

    private fun hasRaster(): Boolean {
        return strikesOverlay?.hasRasterParameters() ?: false
    }

    private fun hasRegion(): Boolean {
        return strikesOverlay?.parameters?.region ?: 0 != 0
    }

    val rasterString: String
        get() = strikesOverlay?.rasterParameters?.info ?: "n/a"

    private fun hasCountThreshold(): Boolean {
        return strikesOverlay?.parameters?.countThreshold ?: 0 > 0
    }

    companion object {
        val REGION_HEIGHT = 1.1f
        val RASTER_HEIGHT = 0.8f
        val COUNT_THRESHOLD_HEIGHT = 0.8f
    }
}
