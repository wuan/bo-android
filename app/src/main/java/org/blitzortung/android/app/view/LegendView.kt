package org.blitzortung.android.app.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View

import org.blitzortung.android.app.R
import org.blitzortung.android.app.helper.ViewHelper
import org.blitzortung.android.data.beans.RasterParameters
import org.blitzortung.android.map.overlay.StrikesOverlay
import org.blitzortung.android.map.overlay.color.ColorHandler

class LegendView(context: Context, attrs: AttributeSet?, defStyle: Int) : View(context, attrs, defStyle) {
    private val padding: Float
    private val colorFieldSize: Float
    private val textPaint: Paint
    private val rasterTextPaint: Paint
    private val regionTextPaint: Paint
    private val countThresholdTextPaint: Paint
    private val backgroundPaint: Paint
    private val foregroundPaint: Paint
    private val backgroundRect: RectF
    private val legendColorRect: RectF
    private var width: Float = 0.toFloat()
    private var height: Float = 0.toFloat()
    private var textWidth: Float = 0.toFloat()
    private var strikesOverlay: StrikesOverlay? = null

    @SuppressWarnings("unused")
    constructor(context: Context, attrs: AttributeSet) : this(context, attrs, 0) {
    }

    @SuppressWarnings("unused")
    constructor(context: Context) : this(context, null, 0) {
    }

    init {

        padding = ViewHelper.pxFromSp(this, 5f)
        colorFieldSize = ViewHelper.pxFromSp(this, 12f)

        foregroundPaint = Paint(Paint.ANTI_ALIAS_FLAG)

        backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        backgroundPaint.color = context.resources.getColor(R.color.translucent_background)

        backgroundRect = RectF()
        legendColorRect = RectF()

        textPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        textPaint.color = -1
        textPaint.textSize = colorFieldSize

        rasterTextPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        rasterTextPaint.color = -1
        rasterTextPaint.textSize = colorFieldSize * RASTER_HEIGHT
        rasterTextPaint.textAlign = Paint.Align.CENTER

        regionTextPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        regionTextPaint.color = -1
        regionTextPaint.textSize = colorFieldSize * REGION_HEIGHT
        regionTextPaint.textAlign = Paint.Align.CENTER

        countThresholdTextPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        countThresholdTextPaint.color = -1
        countThresholdTextPaint.textSize = colorFieldSize * COUNT_THRESHOLD_HEIGHT
        countThresholdTextPaint.textAlign = Paint.Align.CENTER

        updateTextWidth(0)

        setBackgroundColor(Color.TRANSPARENT)
    }

    private fun updateTextWidth(intervalDuration: Int) {
        textWidth = Math.ceil(textPaint.measureText(if (intervalDuration > 100) "< 100min" else "< 10min").toDouble()).toFloat()
    }

    public override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val parentWidth = View.MeasureSpec.getSize(widthMeasureSpec)
        val parentHeight = View.MeasureSpec.getSize(heightMeasureSpec)

        updateTextWidth(strikesOverlay!!.parameters.intervalDuration)
        width = Math.min(3 * padding + colorFieldSize + textWidth, parentWidth.toFloat())

        val colorHandler = strikesOverlay!!.getColorHandler()

        height = Math.min((colorFieldSize + padding) * colorHandler.colors.size + padding, parentHeight.toFloat())

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

    public override fun onDraw(canvas: Canvas) {
        if (strikesOverlay != null) {
            val colorHandler = strikesOverlay!!.getColorHandler()
            val minutesPerColor = strikesOverlay!!.parameters.intervalDuration / colorHandler.numberOfColors

            backgroundRect.set(0f, 0f, width, height)
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
                    val countThreshold = strikesOverlay!!.parameters.countThreshold
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

    fun setStrikesOverlay(strikesOverlay: StrikesOverlay) {
        this.strikesOverlay = strikesOverlay
    }

    fun setAlpha(alpha: Int) {
        foregroundPaint.alpha = alpha
    }

    private fun hasRaster(): Boolean {
        return strikesOverlay!!.hasRasterParameters()
    }

    private fun hasRegion(): Boolean {
        return strikesOverlay!!.parameters.region != 0
    }

    val rasterString: String
        get() = strikesOverlay?.rasterParameters?.info ?: "n/a"

    private fun hasCountThreshold(): Boolean {
        return strikesOverlay!!.parameters.countThreshold > 0
    }

    companion object {
        val REGION_HEIGHT = 1.1f
        val RASTER_HEIGHT = 0.8f
        val COUNT_THRESHOLD_HEIGHT = 0.8f
    }
}
