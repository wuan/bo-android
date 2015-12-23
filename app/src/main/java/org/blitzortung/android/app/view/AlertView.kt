package org.blitzortung.android.app.view

import android.content.Context
import android.graphics.*
import android.graphics.Paint.Align
import android.graphics.Paint.Style
import android.util.AttributeSet
import android.view.View
import org.blitzortung.android.alert.data.AlertContext
import org.blitzortung.android.alert.data.AlertSector
import org.blitzortung.android.alert.event.AlertEvent
import org.blitzortung.android.alert.event.AlertResultEvent
import org.blitzortung.android.app.R
import org.blitzortung.android.app.helper.ViewHelper
import org.blitzortung.android.location.LocationEvent
import org.blitzortung.android.map.overlay.color.ColorHandler

class AlertView(context: Context, attrs: AttributeSet?, defStyle: Int) : View(context, attrs, defStyle) {
    private val arcArea = RectF()
    private val background = Paint()
    private val sectorPaint = Paint()
    private val lines = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textStyle = Paint(Paint.ANTI_ALIAS_FLAG)
    private val warnText = Paint(Paint.ANTI_ALIAS_FLAG)
    private val transfer = Paint()
    private val alarmNotAvailableTextLines: Array<String>
    private var colorHandler: ColorHandler? = null
    private var intervalDuration: Int = 0
    private var temporaryBitmap: Bitmap? = null
    private var temporaryCanvas: Canvas? = null
    private var alertContext: AlertContext? = null

    val alertEventConsumer: (AlertEvent?) -> Unit = { event ->
        if (event is AlertResultEvent) {
            alertContext = event.alertContext
        } else {
            alertContext = null
        }
        invalidate()
    }

    val locationEventConsumer: (LocationEvent) -> Unit = { locationEvent ->
        val location = locationEvent.location
        val visibility = if (location != null) View.VISIBLE else View.INVISIBLE
        setVisibility(visibility)
        invalidate()
    }

    @SuppressWarnings("unused")
    constructor(context: Context, attrs: AttributeSet) : this(context, attrs, 0) {
    }

    @SuppressWarnings("unused")
    constructor(context: Context) : this(context, null, 0) {
    }

    init {
        alarmNotAvailableTextLines = context.getString(R.string.alarms_not_available)
                .split("\n".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()

        lines.color = 0xff404040.toInt()
        lines.style = Style.STROKE

        textStyle.color = 0xff404040.toInt()
        textStyle.textSize = ViewHelper.pxFromSp(this, 10f)

        background.color = 0xffb0b0b0.toInt()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val parentWidth = View.MeasureSpec.getSize(widthMeasureSpec)
        val parentHeight = View.MeasureSpec.getSize(heightMeasureSpec)

        val size = Math.min(parentWidth, parentHeight)

        super.onMeasure(View.MeasureSpec.makeMeasureSpec(size, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(size, View.MeasureSpec.EXACTLY))
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
    }

    override fun onDraw(canvas: Canvas) {
        val size = Math.max(width, height)
        val pad = 4

        val center = size / 2.0f
        val radius = center - pad

        prepareTemporaryBitmap(size)

        val alertContext = alertContext
        if (alertContext != null && intervalDuration != 0) {
            val alertParameters = alertContext!!.alertParameters
            val rangeSteps = alertParameters.rangeSteps
            val rangeStepCount = rangeSteps.size
            val radiusIncrement = radius / rangeStepCount
            val sectorWidth = (360 / alertParameters.sectorLabels.size).toFloat()

            lines.color = colorHandler!!.lineColor
            lines.strokeWidth = (size / 150).toFloat()

            textStyle.textAlign = Align.CENTER
            textStyle.color = colorHandler!!.textColor

            val actualTime = System.currentTimeMillis()

            for (alertSector in alertContext.sectors) {

                val startAngle = alertSector.minimumSectorBearing + 90f + 180f

                val ranges = alertSector.ranges
                for (rangeIndex in ranges.indices.reversed()) {
                    val alertSectorRange = ranges[rangeIndex]

                    val sectorRadius = (rangeIndex + 1) * radiusIncrement
                    val leftTop = center - sectorRadius
                    val bottomRight = center + sectorRadius

                    val drawColor = alertSectorRange.strikeCount > 0
                    if (drawColor) {
                        val color = colorHandler!!.getColor(actualTime, alertSectorRange.latestStrikeTimestamp, intervalDuration)
                        sectorPaint.color = color
                    }
                    arcArea.set(leftTop, leftTop, bottomRight, bottomRight)
                    temporaryCanvas!!.drawArc(arcArea, startAngle, sectorWidth, true, if (drawColor) sectorPaint else background)
                }
            }

            for (alertSector in alertContext.sectors) {
                val bearing = alertSector.minimumSectorBearing.toDouble()
                temporaryCanvas!!.drawLine(center, center, center + (radius * Math.sin(bearing / 180.0f * Math.PI)).toFloat(), center + (radius * -Math.cos(bearing / 180.0f * Math.PI)).toFloat(), lines)

                if (size > TEXT_MINIMUM_SIZE) {
                    drawSectorLabel(center, radiusIncrement, alertSector, bearing + sectorWidth / 2.0)
                }
            }

            textStyle.textAlign = Align.RIGHT
            val textHeight = textStyle.getFontMetrics(null)
            for (radiusIndex in 0..rangeStepCount - 1) {
                val leftTop = center - (radiusIndex + 1) * radiusIncrement
                val bottomRight = center + (radiusIndex + 1) * radiusIncrement
                arcArea.set(leftTop, leftTop, bottomRight, bottomRight)
                temporaryCanvas!!.drawArc(arcArea, 0f, 360f, false, lines)

                if (size > TEXT_MINIMUM_SIZE) {
                    val text = "%.0f".format(rangeSteps[radiusIndex])
                    temporaryCanvas!!.drawText(text, center + (radiusIndex + 0.85f) * radiusIncrement, center + textHeight / 3f, textStyle)
                    if (radiusIndex == rangeStepCount - 1) {
                        temporaryCanvas!!.drawText(alertParameters.measurementSystem.unitName, center + (radiusIndex + 0.85f) * radiusIncrement, center + textHeight * 1.33f, textStyle)
                    }
                }
            }

        } else if (size > TEXT_MINIMUM_SIZE) {
            warnText.color = 0xffa00000.toInt()
            warnText.textAlign = Align.CENTER
            warnText.textSize = DEFAULT_FONT_SIZE.toFloat()

            val maxWidth = alarmNotAvailableTextLines.map { warnText.measureText(it) }.max()
                    ?: width.toFloat() - 20
            val scale = (width - 20).toFloat() / maxWidth

            //Now scale the text so we can use the whole width of the canvas
            warnText.textSize = scale * DEFAULT_FONT_SIZE

            for (line in alarmNotAvailableTextLines.indices) {
                temporaryCanvas!!.drawText(alarmNotAvailableTextLines[line], center, center + (line - 1) * warnText.getFontMetrics(null), warnText)
            }
        }
        canvas.drawBitmap(temporaryBitmap, 0f, 0f, transfer)
    }

    private fun drawSectorLabel(center: Float, radiusIncrement: Float, sector: AlertSector, bearing: Double) {
        if (bearing != 90.0) {
            val text = sector.label
            val textRadius = (sector.ranges.size - 0.5f) * radiusIncrement
            temporaryCanvas!!.drawText(text, center + (textRadius * Math.sin(bearing / 180.0 * Math.PI)).toFloat(), center + (textRadius * -Math.cos(bearing / 180.0 * Math.PI)).toFloat() + textStyle.getFontMetrics(null) / 3f, textStyle)
        }
    }

    private fun prepareTemporaryBitmap(size: Int) {
        if (temporaryBitmap == null) {
            temporaryBitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
            temporaryCanvas = Canvas(temporaryBitmap)
        }
        background.color = colorHandler!!.backgroundColor
        background.setXfermode(XFERMODE_CLEAR)
        temporaryCanvas!!.drawPaint(background)

        background.setXfermode(XFERMODE_SRC)
    }

    fun setColorHandler(colorHandler: ColorHandler, intervalDuration: Int) {
        this.colorHandler = colorHandler
        this.intervalDuration = intervalDuration
    }

    override fun setBackgroundColor(backgroundColor: Int) {
        background.color = backgroundColor
    }

    fun setAlpha(alpha: Int) {
        transfer.alpha = alpha
    }

    companion object {
        private val TEXT_MINIMUM_SIZE = 300
        private val DEFAULT_FONT_SIZE = 20
        private val XFERMODE_CLEAR = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
        private val XFERMODE_SRC = PorterDuffXfermode(PorterDuff.Mode.SRC)
    }

}
