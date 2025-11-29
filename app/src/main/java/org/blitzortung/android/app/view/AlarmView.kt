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
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Paint.Align
import android.graphics.Paint.Style
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RectF
import android.location.Location
import android.util.AttributeSet
import android.util.Log
import androidx.core.graphics.createBitmap
import androidx.preference.PreferenceManager
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import org.blitzortung.android.alert.AlertParameters
import org.blitzortung.android.alert.LocalActivity
import org.blitzortung.android.alert.NoLocation
import org.blitzortung.android.alert.Outlying
import org.blitzortung.android.alert.Warning
import org.blitzortung.android.alert.data.AlertSector
import org.blitzortung.android.alert.handler.AlertHandler
import org.blitzortung.android.app.Main
import org.blitzortung.android.app.R
import org.blitzortung.android.app.helper.ViewHelper
import org.blitzortung.android.app.view.alarm.PrimitiveRenderer
import org.blitzortung.android.app.view.alarm.SymbolRenderer
import org.blitzortung.android.data.MainDataHandler
import org.blitzortung.android.dialogs.AlarmDialog
import org.blitzortung.android.dialogs.AlertDialogColorHandler
import org.blitzortung.android.location.LocationEvent
import org.blitzortung.android.location.LocationUpdate
import org.blitzortung.android.map.overlay.color.ColorHandler
import org.blitzortung.android.util.TabletAwareView

class AlarmView
@JvmOverloads
constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0,
) : TabletAwareView(context, attrs, defStyle) {

    private val arcArea = RectF()
    private val background = Paint()
    private val sectorPaint = Paint()
    private val lines = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textStyle = Paint(Paint.ANTI_ALIAS_FLAG)
    private val transfer = Paint()
    private lateinit var colorHandler: ColorHandler
    private var intervalDuration: Int = 0
    private var temporaryBitmap: Bitmap? = null
    private var temporaryCanvas: Canvas? = null
    private var warning: Warning? = null
    private var location: Location? = null
    private var enableDescriptionText = false

    private var primitiveRenderer: PrimitiveRenderer
    private lateinit var symbolRenderer: SymbolRenderer

    val alertEventConsumer: (Warning) -> Unit = { event ->
        val updated = warning != event
        if (updated) {
            Log.v(Main.LOG_TAG, "AlertView alertEventConsumer received $event")
            warning = event
            invalidate()
        }
    }

    val locationEventConsumer: (LocationEvent) -> Unit = { locationEvent ->
        val newLocation = if (locationEvent is LocationUpdate) locationEvent.location else null
        if (location != newLocation) {
            Log.v(Main.LOG_TAG, "AlertView received location ${newLocation}")
            location = newLocation
            val visibility = if (location != null) VISIBLE else INVISIBLE
            setVisibility(visibility)
            invalidate()
        }
    }

    init {
        with(lines) {
            color = 0xff404040.toInt()
            style = Style.STROKE
        }

        with(textStyle) {
            color = 0xff404040.toInt()
            textSize = 0.8f * (textSize * textSizeFactor(context))
        }

        background.color = 0xffb0b0b0.toInt()

        primitiveRenderer = PrimitiveRenderer()
    }

    fun setColorHandler(
        colorHandler: ColorHandler,
        intervalDuration: Int,
    ) {
        this.colorHandler = colorHandler
        this.intervalDuration = intervalDuration

        symbolRenderer = SymbolRenderer(context, primitiveRenderer, colorHandler, textSize * textSizeFactor(this.context))
    }

    fun enableLongClickListener(
        dataHandler: MainDataHandler,
        alertHandler: AlertHandler,
    ) {
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)

        setOnLongClickListener {
            AlarmDialog(context, AlertDialogColorHandler(sharedPreferences), dataHandler, alertHandler)
                .show()

            true
        }
    }

    fun enableDescriptionText() {
        enableDescriptionText = true
    }

    override fun onMeasure(
        widthMeasureSpec: Int,
        heightMeasureSpec: Int,
    ) {
        val getSize = fun(spec: Int) = MeasureSpec.getSize(spec)

        val parentWidth = getSize(widthMeasureSpec) * sizeFactor
        val parentHeight = getSize(heightMeasureSpec) * sizeFactor

        val size = min(parentWidth.toInt(), parentHeight.toInt())

        super.onMeasure(
            MeasureSpec.makeMeasureSpec(size, MeasureSpec.EXACTLY),
            MeasureSpec.makeMeasureSpec(size, MeasureSpec.EXACTLY),
        )
    }

    data class AlarmViewData(
        val size: Int,
        val center: Float,
        val radius: Float,
    )

    override fun onDraw(canvas: Canvas) {
        val size = max(width, height)
        val pad = ViewHelper.pxFromDp(context, 5f)

        val center = size / 2.0f
        val radius = center - pad

        val alarmViewData = AlarmViewData(size, center, radius)


        prepareTemporaryBitmap(size)

        val alertResult = warning
        val temporaryCanvas = temporaryCanvas
        val temporaryBitmap = temporaryBitmap
        if (temporaryBitmap != null && temporaryCanvas != null) {
            when (alertResult) {
                is LocalActivity if intervalDuration != 0 -> {
                    renderLocalActivity(alertResult, radius, size, center, temporaryCanvas)
                }

                Outlying -> {
                    symbolRenderer.drawOutOfRangeSymbol(alarmViewData, temporaryCanvas)
                }

                NoLocation -> {
                    if (enableDescriptionText && size > TEXT_MINIMUM_SIZE) {
                        symbolRenderer.drawAlertOrLocationMissingMessage(center, width, temporaryCanvas)
                    } else {
                        symbolRenderer.drawNoLocationSymbol(center, radius, size, temporaryCanvas)
                    }
                }

                else -> {
                    if (enableDescriptionText && size > TEXT_MINIMUM_SIZE) {
                        symbolRenderer.drawAlertOrLocationMissingMessage(center, width, temporaryCanvas)
                    } else {
                        if (location != null) {
                            symbolRenderer.drawOwnLocationSymbol(center, radius, size, temporaryCanvas)
                        } else {
                            symbolRenderer.drawNoLocationSymbol(center, radius, size, temporaryCanvas)
                        }
                    }
                }
            }
            canvas.drawBitmap(temporaryBitmap, 0f, 0f, transfer)
        }
    }

    private fun renderLocalActivity(
        alertResult: LocalActivity,
        radius: Float,
        size: Int,
        center: Float,
        temporaryCanvas: Canvas
    ) {
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
            renderSectorBackground(alertSector, radiusIncrement, center, actualTime, temporaryCanvas, sectorWidth)
        }

        for (alertSector in alertResult.sectors) {
            renderSectorSideLines(alertSector, temporaryCanvas, center, radius, size, radiusIncrement, sectorWidth)
        }

        textStyle.textAlign = Align.RIGHT
        val textHeight = textStyle.getFontMetrics(null)
        for (radiusIndex in 0 until rangeStepCount) {
            renderRangeCircles(
                radiusIndex,
                rangeStepCount,
                size,
                center,
                radiusIncrement,
                temporaryCanvas,
                rangeSteps,
                textHeight,
                alertParameters
            )
        }
    }

    private fun renderRangeCircles(
        radiusIndex: Int,
        rangeStepCount: Int,
        size: Int,
        center: Float,
        radiusIncrement: Float,
        temporaryCanvas: Canvas,
        rangeSteps: List<Float>,
        textHeight: Float,
        alertParameters: AlertParameters
    ) {
        if (radiusIndex == rangeStepCount - 1) {
            lines.strokeWidth = (size / 80).toFloat()
        }
        primitiveRenderer.drawCircle(
            center,
            (radiusIndex + 1) * radiusIncrement,
            lines,
            temporaryCanvas
        )

        if (enableDescriptionText && size > TEXT_MINIMUM_SIZE) {
            val text = "%.0f".format(rangeSteps[radiusIndex])
            temporaryCanvas.drawText(
                text,
                center + (radiusIndex + 0.85f) * radiusIncrement,
                center + textHeight / 3f,
                textStyle,
            )
            if (radiusIndex == rangeStepCount - 1) {
                val distanceUnit = resources.getString(alertParameters.measurementSystem.unitNameString)
                temporaryCanvas.drawText(
                    distanceUnit,
                    center + (radiusIndex + 0.85f) * radiusIncrement,
                    center + textHeight * 1.33f,
                    textStyle,
                )
            }
        }
    }

    private fun renderSectorSideLines(
        alertSector: AlertSector,
        temporaryCanvas: Canvas,
        center: Float,
        radius: Float,
        size: Int,
        radiusIncrement: Float,
        sectorWidth: Float
    ) {
        val bearing = alertSector.minimumSectorBearing.toDouble()
        temporaryCanvas.drawLine(
            center,
            center,
            center + (radius * sin(bearing / 180.0f * Math.PI)).toFloat(),
            center + (radius * -cos(bearing / 180.0f * Math.PI)).toFloat(),
            lines,
        )

        if (enableDescriptionText && size > TEXT_MINIMUM_SIZE) {
            drawSectorLabel(center, radiusIncrement, alertSector, bearing + sectorWidth / 2.0)
        }
    }

    private fun renderSectorBackground(
        alertSector: AlertSector,
        radiusIncrement: Float,
        center: Float,
        actualTime: Long,
        temporaryCanvas: Canvas,
        sectorWidth: Float
    ) {
        val startAngle = alertSector.minimumSectorBearing + 90f + 180f

        val ranges = alertSector.ranges
        for (rangeIndex in ranges.indices.reversed()) {
            val alertSectorRange = ranges[rangeIndex]

            val sectorRadius = (rangeIndex + 1) * radiusIncrement
            val leftTop = center - sectorRadius
            val bottomRight = center + sectorRadius

            val drawColor = alertSectorRange.strikeCount > 0
            if (drawColor) {
                val color =
                    colorHandler.getColor(
                        actualTime,
                        alertSectorRange.latestStrikeTimestamp,
                        intervalDuration,
                    )
                sectorPaint.color = color
            }
            arcArea.set(leftTop, leftTop, bottomRight, bottomRight)
            temporaryCanvas.drawArc(
                arcArea,
                startAngle,
                sectorWidth,
                true,
                if (drawColor) sectorPaint else background,
            )
        }
    }

    private fun drawSectorLabel(
        center: Float,
        radiusIncrement: Float,
        sector: AlertSector,
        bearing: Double,
    ) {
        if (bearing != 90.0) {
            val text = sector.label
            val textRadius = (sector.ranges.size - 0.5f) * radiusIncrement
            temporaryCanvas!!.drawText(
                text,
                center + (textRadius * sin(bearing / 180.0 * Math.PI)).toFloat(),
                center + (textRadius * -cos(bearing / 180.0 * Math.PI)).toFloat() + textStyle.getFontMetrics(null) / 3f,
                textStyle,
            )
        }
    }

    private fun prepareTemporaryBitmap(size: Int) {
        if (temporaryBitmap == null) {
            val temporaryBitmap = createBitmap(size, size)
            this.temporaryBitmap = temporaryBitmap
            temporaryCanvas = Canvas(temporaryBitmap)
        }
        background.color = colorHandler.backgroundColor
        background.xfermode = XFERMODE_CLEAR
        temporaryCanvas!!.drawPaint(background)

        background.xfermode = XFERMODE_SRC
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
