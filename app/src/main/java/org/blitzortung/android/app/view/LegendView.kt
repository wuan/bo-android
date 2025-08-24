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
import org.blitzortung.android.app.R
import org.blitzortung.android.map.overlay.StrikeListOverlay
import org.blitzortung.android.util.TabletAwareView
import kotlin.math.max
import kotlin.math.min

class LegendView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : TabletAwareView(context, attrs, defStyle) {
    private val colorFieldSize: Float = textSize
    private val textPaint: Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = -1
        textSize = this@LegendView.textSize
    }
    private val gridTextPaint: Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = -1
        textSize = this@LegendView.textSize * GRID_HEIGHT
        textAlign = Paint.Align.CENTER
    }
    private val regionTextPaint: Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = -1
        textSize = this@LegendView.textSize * REGION_HEIGHT
        textAlign = Paint.Align.CENTER
    }
    private val countThresholdTextPaint: Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = -1
        textSize = this@LegendView.textSize * COUNT_THRESHOLD_HEIGHT
        textAlign = Paint.Align.CENTER
    }
    private val backgroundPaint: Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = context.resources.getColor(R.color.translucent_background)
    }
    private val foregroundPaint: Paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val backgroundRect: RectF = RectF()
    private val legendColorRect: RectF = RectF()
    var strikesOverlay: StrikeListOverlay? = null

    init {
        setBackgroundColor(Color.TRANSPARENT)
    }

    private fun determineWidth(intervalDuration: Int): Float {
        val numberOfColors = strikesOverlay?.colorHandler?.numberOfColors ?: 1
        var innerWidth = colorFieldSize + padding + textPaint.measureText(
            LEGEND_FORMAT.format(
                '<',
                intervalDuration / numberOfColors * (numberOfColors - 1),
                context.resources.getString(R.string.unit_minute)
            )
        )

        if (hasRegion()) {
            innerWidth = max(innerWidth, regionTextPaint.measureText(regionName))
        }

        return padding + innerWidth + padding
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val getSize = fun(spec: Int) = MeasureSpec.getSize(spec)

        val parentWidth = getSize(widthMeasureSpec)
        val parentHeight = getSize(heightMeasureSpec)

        val width = min(
            determineWidth(
                strikesOverlay?.parameters?.intervalDuration
                    ?: 0
            ), parentWidth.toFloat()
        )

        val colorHandler = strikesOverlay?.colorHandler

        var height = 0.0f
        if (colorHandler != null) {
            height = min((colorFieldSize + padding) * colorHandler.colors.size + padding, parentHeight.toFloat())

            if (hasRegion()) {
                height += colorFieldSize * REGION_HEIGHT + padding
            }

            if (usesGrid()) {
                height += colorFieldSize * GRID_HEIGHT + padding

                if (hasCountThreshold()) {
                    height += colorFieldSize * COUNT_THRESHOLD_HEIGHT + padding
                }
            }
        }

        super.onMeasure(
            MeasureSpec.makeMeasureSpec(width.toInt(), MeasureSpec.EXACTLY),
            MeasureSpec.makeMeasureSpec(height.toInt(), MeasureSpec.EXACTLY)
        )
    }

    override fun onDraw(canvas: Canvas) {
        strikesOverlay?.let { strikesOverlay ->
            val colorHandler = strikesOverlay.colorHandler
            val minutesPerColor = strikesOverlay.parameters.intervalDuration / colorHandler.numberOfColors

            backgroundRect.set(0f, 0f, width.toFloat(), height.toFloat())
            canvas.drawRect(backgroundRect, backgroundPaint)

            val numberOfColors = colorHandler.numberOfColors

            var topCoordinate = padding

            for (index in 0 until numberOfColors) {
                foregroundPaint.color = colorHandler.getColor(index)
                legendColorRect.set(padding, topCoordinate, padding + colorFieldSize, topCoordinate + colorFieldSize)
                canvas.drawRect(legendColorRect, foregroundPaint)

                val isLastValue = index == numberOfColors - 1
                val minuteUnit = context.getString(R.string.unit_minute)
                val text = LEGEND_FORMAT.format(
                    if (isLastValue) '>' else '<',
                    (index + (if (isLastValue) 0 else 1)) * minutesPerColor,
                    minuteUnit
                )

                canvas.drawText(text, 2 * padding + colorFieldSize, topCoordinate + colorFieldSize / 1.1f, textPaint)

                topCoordinate += colorFieldSize + padding
            }

            if (hasRegion()) {
                canvas.drawText(
                    regionName,
                    width / 2.0f,
                    topCoordinate + colorFieldSize * REGION_HEIGHT / 1.1f,
                    regionTextPaint
                )
                topCoordinate += colorFieldSize * REGION_HEIGHT + padding
            }

            if (usesGrid()) {
                canvas.drawText(
                    context.getString(R.string.legend_grid) + ": " + gridInfo,
                    width / 2.0f,
                    topCoordinate + colorFieldSize * GRID_HEIGHT / 1.1f,
                    gridTextPaint
                )
                topCoordinate += colorFieldSize * GRID_HEIGHT + padding

                if (hasCountThreshold()) {
                    val countThreshold = strikesOverlay.parameters.countThreshold
                    canvas.drawText(
                        "# > $countThreshold",
                        width / 2.0f,
                        topCoordinate + colorFieldSize * COUNT_THRESHOLD_HEIGHT / 1.1f,
                        countThresholdTextPaint
                    )
                    topCoordinate += colorFieldSize * COUNT_THRESHOLD_HEIGHT + padding
                }
            }
        }
    }

    private val regionName: String
        get() {

            val strikesOverlay = strikesOverlay

            if (strikesOverlay != null) {
                val regionNumber = strikesOverlay.parameters.region
                val localReference = strikesOverlay.parameters.dataArea

                for ((index, regionNumberString) in resources.getStringArray(R.array.regions_values).withIndex()) {
                    if (regionNumber == Integer.parseInt(regionNumberString)) {
                        val detail = if (regionNumber < 0 && localReference != null) {
                            " [${localReference.scale}]"
                        } else {
                            ""
                        }
                        return resources.getStringArray(R.array.regions)[index] + detail
                    }
                }
            }

            return resources.getString(R.string.not_available)
        }

    fun setAlpha(alpha: Int) {
        foregroundPaint.alpha = alpha
    }

    private fun usesGrid(): Boolean {
        return strikesOverlay?.usesGrid() == true
    }

    private fun hasRegion(): Boolean {
        return (strikesOverlay?.parameters?.region ?: 0) != 0
    }

    private val gridInfo: String
        get() {
            val gridSize = strikesOverlay?.gridParameters?.size
            return if (gridSize != null) {
                "%.0f %s".format(gridSize / 1000.0, context.getString(R.string.unit_km))
            } else {
                context.getString(R.string.not_available)
            }
        }

    private fun hasCountThreshold(): Boolean {
        return (strikesOverlay?.parameters?.countThreshold ?: 0) > 0
    }

    companion object {
        const val REGION_HEIGHT = 1.1f
        const val GRID_HEIGHT = 0.8f
        const val COUNT_THRESHOLD_HEIGHT = 0.8f
        const val LEGEND_FORMAT = "%c %d %s"
    }
}
