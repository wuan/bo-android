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
import android.graphics.*
import android.graphics.Paint.Align
import android.graphics.Paint.Style
import android.location.Location
import android.preference.PreferenceManager
import android.util.AttributeSet
import android.util.Log
import org.blitzortung.android.alert.AlertResult
import org.blitzortung.android.alert.data.AlertSector
import org.blitzortung.android.alert.event.AlertEvent
import org.blitzortung.android.alert.event.AlertResultEvent
import org.blitzortung.android.alert.handler.AlertHandler
import org.blitzortung.android.app.Main
import org.blitzortung.android.app.R
import org.blitzortung.android.data.MainDataHandler
import org.blitzortung.android.dialogs.AlertDialog
import org.blitzortung.android.dialogs.AlertDialogColorHandler
import org.blitzortung.android.location.LocationEvent
import org.blitzortung.android.map.overlay.color.ColorHandler
import org.blitzortung.android.util.TabletAwareView
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

class AlertView @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyle: Int = 0
) : TabletAwareView(context, attrs, defStyle) {
    private val arcArea = RectF()
    private val background = Paint()
    private val sectorPaint = Paint()
    private val lines = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textStyle = Paint(Paint.ANTI_ALIAS_FLAG)
    private val warnText = Paint(Paint.ANTI_ALIAS_FLAG)
    private val transfer = Paint()
    private val alarmNotAvailableTextLines: Array<String> = context.getString(R.string.alarms_not_available)
            .split("\n".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
    private lateinit var colorHandler: ColorHandler
    private var intervalDuration: Int = 0
    private var temporaryBitmap: Bitmap? = null
    private var temporaryCanvas: Canvas? = null
    private var alertResult: AlertResult? = null
    private var location: Location? = null
    private var enableDescriptionText = false

    val alertEventConsumer: (AlertEvent?) -> Unit = { event ->
        Log.v(Main.LOG_TAG, "AlertView alertEventConsumer received $event")
        alertResult = if (event is AlertResultEvent) {
            event.alertResult
        } else {
            null
        }
        invalidate()
    }

    val locationEventConsumer: (LocationEvent) -> Unit = { locationEvent ->
        Log.v(Main.LOG_TAG, "AlertView received location ${locationEvent.location}")
        location = locationEvent.location
        val visibility = if (location != null) VISIBLE else INVISIBLE
        setVisibility(visibility)
        invalidate()
    }

    init {
        with(lines) {
            color = 0xff404040.toInt()
            style = Style.STROKE
        }

        with(textStyle) {
            color = 0xff404040.toInt()
            textSize = 0.8f * this@AlertView.textSize * textSizeFactor(context)
        }

        background.color = 0xffb0b0b0.toInt()
    }

    fun enableLongClickListener(dataHandler: MainDataHandler, alertHandler: AlertHandler) {

        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)

        setOnLongClickListener {
            AlertDialog(context, AlertDialogColorHandler(sharedPreferences), dataHandler, alertHandler)
                    .show()

            true
        }
    }

    fun enableDescriptionText() {
        enableDescriptionText = true
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val getSize = fun(spec: Int) = MeasureSpec.getSize(spec)

        val parentWidth = getSize(widthMeasureSpec) * sizeFactor
        val parentHeight = getSize(heightMeasureSpec) * sizeFactor

        val size = min(parentWidth.toInt(), parentHeight.toInt())

        super.onMeasure(MeasureSpec.makeMeasureSpec(size, MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(size, MeasureSpec.EXACTLY))
    }

    override fun onDraw(canvas: Canvas) {
        val size = max(width, height)
        val pad = 4

        val center = size / 2.0f
        val radius = center - pad

        prepareTemporaryBitmap(size)

        val alertResult = alertResult
        val temporaryCanvas = temporaryCanvas
        val temporaryBitmap = temporaryBitmap
        if (temporaryBitmap != null && temporaryCanvas != null) {
            if (alertResult != null && intervalDuration != 0) {
                val alertParameters = alertResult.parameters
                val rangeSteps = alertParameters.rangeSteps
                val rangeStepCount = rangeSteps.size
                val radiusIncrement = radius / rangeStepCount
                val sectorWidth = (360 / alertParameters.sectorLabels.size).toFloat()

                with(lines) {
                    color = colorHandler.lineColor
                    strokeWidth = (size / 150).toFloat()
                }

                with(textStyle) {
                    textAlign = Align.CENTER
                    color = colorHandler.textColor
                }

                val actualTime = System.currentTimeMillis()

                for (alertSector in alertResult.sectors) {

                    val startAngle = alertSector.minimumSectorBearing + 90f + 180f

                    val ranges = alertSector.ranges
                    for (rangeIndex in ranges.indices.reversed()) {
                        val alertSectorRange = ranges[rangeIndex]

                        val sectorRadius = (rangeIndex + 1) * radiusIncrement
                        val leftTop = center - sectorRadius
                        val bottomRight = center + sectorRadius

                        val drawColor = alertSectorRange.strikeCount > 0
                        if (drawColor) {
                            val color = colorHandler.getColor(actualTime, alertSectorRange.latestStrikeTimestamp, intervalDuration)
                            sectorPaint.color = color
                        }
                        arcArea.set(leftTop, leftTop, bottomRight, bottomRight)
                        temporaryCanvas.drawArc(arcArea, startAngle, sectorWidth, true, if (drawColor) sectorPaint else background)
                    }
                }

                for (alertSector in alertResult.sectors) {
                    val bearing = alertSector.minimumSectorBearing.toDouble()
                    temporaryCanvas.drawLine(center, center, center + (radius * sin(bearing / 180.0f * Math.PI)).toFloat(), center + (radius * -cos(bearing / 180.0f * Math.PI)).toFloat(), lines)

                    if (enableDescriptionText && size > TEXT_MINIMUM_SIZE) {
                        drawSectorLabel(center, radiusIncrement, alertSector, bearing + sectorWidth / 2.0)
                    }
                }

                textStyle.textAlign = Align.RIGHT
                val textHeight = textStyle.getFontMetrics(null)
                for (radiusIndex in 0 until rangeStepCount) {
                    val leftTop = center - (radiusIndex + 1) * radiusIncrement
                    val bottomRight = center + (radiusIndex + 1) * radiusIncrement
                    arcArea.set(leftTop, leftTop, bottomRight, bottomRight)
                    temporaryCanvas.drawArc(arcArea, 0f, 360f, false, lines)

                    if (enableDescriptionText && size > TEXT_MINIMUM_SIZE) {
                        val text = "%.0f".format(rangeSteps[radiusIndex])
                        temporaryCanvas.drawText(text, center + (radiusIndex + 0.85f) * radiusIncrement, center + textHeight / 3f, textStyle)
                        if (radiusIndex == rangeStepCount - 1) {
                            val distanceUnit = resources.getString(alertParameters.measurementSystem.unitNameString)
                            temporaryCanvas.drawText(distanceUnit, center + (radiusIndex + 0.85f) * radiusIncrement, center + textHeight * 1.33f, textStyle)
                        }
                    }
                }

            } else {
                if (enableDescriptionText && size > TEXT_MINIMUM_SIZE) {
                    drawAlertOrLocationMissingMessage(center, temporaryCanvas)
                } else {
                    if (location != null) {
                        drawOwnLocationSymbol(center, radius, size, temporaryCanvas)
                    }
                }
            }
            canvas.drawBitmap(temporaryBitmap, 0f, 0f, transfer)
        }
    }

    private fun drawAlertOrLocationMissingMessage(center: Float, canvas: Canvas) {
        with(warnText) {
            color = resources.getColor(R.color.text_warning)
            textAlign = Align.CENTER
            textSize = DEFAULT_FONT_SIZE.toFloat()

            val maxWidth = alarmNotAvailableTextLines.map { warnText.measureText(it) }.max()
                    ?: width.toFloat() - 20
            val scale = (width - 20).toFloat() / maxWidth

            //Now scale the text so we can use the whole width of the canvas
            textSize = scale * DEFAULT_FONT_SIZE
        }


        for (line in alarmNotAvailableTextLines.indices) {
            canvas.drawText(alarmNotAvailableTextLines[line], center, center + (line - 1) * warnText.getFontMetrics(null), warnText)
        }
    }

    private fun drawOwnLocationSymbol(center: Float, radius: Float, size: Int, temporaryCanvas: Canvas) {
        with(lines) {
            color = colorHandler.lineColor
            strokeWidth = (size / 80).toFloat()
        }

        val largeRadius = radius * 0.8f
        val leftTop = center - largeRadius
        val bottomRight = center + largeRadius
        arcArea.set(leftTop, leftTop, bottomRight, bottomRight)
        temporaryCanvas.drawArc(arcArea, 0f, 360f, false, lines)

        val smallRadius = radius * 0.6f
        temporaryCanvas.drawLine(center - smallRadius, center, center + smallRadius, center, lines)
        temporaryCanvas.drawLine(center, center - smallRadius, center, center + smallRadius, lines)
    }

    private fun drawSectorLabel(center: Float, radiusIncrement: Float, sector: AlertSector, bearing: Double) {
        if (bearing != 90.0) {
            val text = sector.label
            val textRadius = (sector.ranges.size - 0.5f) * radiusIncrement
            temporaryCanvas!!.drawText(text, center + (textRadius * sin(bearing / 180.0 * Math.PI)).toFloat(), center + (textRadius * -cos(bearing / 180.0 * Math.PI)).toFloat() + textStyle.getFontMetrics(null) / 3f, textStyle)
        }
    }

    private fun prepareTemporaryBitmap(size: Int) {
        if (temporaryBitmap == null) {
            val temporaryBitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
            this.temporaryBitmap = temporaryBitmap
            temporaryCanvas = Canvas(temporaryBitmap)
        }
        background.color = colorHandler.backgroundColor
        background.xfermode = XFERMODE_CLEAR
        temporaryCanvas!!.drawPaint(background)

        background.xfermode = XFERMODE_SRC
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
        private const val TEXT_MINIMUM_SIZE = 300
        private const val DEFAULT_FONT_SIZE = 20
        private val XFERMODE_CLEAR = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
        private val XFERMODE_SRC = PorterDuffXfermode(PorterDuff.Mode.SRC)
    }

}
