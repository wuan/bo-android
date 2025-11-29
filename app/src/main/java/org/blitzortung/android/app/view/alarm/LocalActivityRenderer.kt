package org.blitzortung.android.app.view.alarm

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Paint.Align
import android.graphics.Paint.Style
import android.graphics.RectF
import kotlin.math.cos
import kotlin.math.sin
import org.blitzortung.android.alert.AlertParameters
import org.blitzortung.android.alert.LocalActivity
import org.blitzortung.android.alert.data.AlertSector
import org.blitzortung.android.app.view.AlarmViewData
import org.blitzortung.android.app.view.support.CanvasWrapper
import org.blitzortung.android.map.overlay.color.ColorHandler

class LocalActivityRenderer(
    private val context: Context,
    private val primitiveRenderer: PrimitiveRenderer,
    private val textSize: Float,
) {

    var enableDescriptionText: Boolean = false
    private val arcArea = RectF()
    private val sectorPaint = Paint()
    private val lines = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textStyle = Paint(Paint.ANTI_ALIAS_FLAG)
    var colorHandler: ColorHandler? = null
    var intervalDuration: Int? = null

    init {
        with(lines) {
            color = 0xff404040.toInt()
            style = Style.STROKE
        }

        with(textStyle) {
            color = 0xff404040.toInt()
            textSize = 0.8f * this@LocalActivityRenderer.textSize
        }
    }

    fun renderLocalActivity(
        alertResult: LocalActivity,
        data: AlarmViewData,
        canvasWrapper: CanvasWrapper,
    ) {
        val alertParameters = alertResult.parameters
        val rangeSteps = alertParameters.rangeSteps
        val radiusIncrement = data.radius / rangeSteps.size
        val sectorWidth = alertParameters.sectorWidth

        with(lines) {
            colorHandler?.also { color = it.lineColor }
            strokeWidth = (data.size / 150).toFloat()
        }

        with(textStyle) {
            textAlign = Align.CENTER
            colorHandler?.also { color = it.textColor }
        }

        val actualTime = System.currentTimeMillis()

        for (alertSector in alertResult.sectors) {
            renderSectorBackground(alertSector, radiusIncrement, data, actualTime, sectorWidth, canvasWrapper)
        }

        for (alertSector in alertResult.sectors) {
            renderSectorSideLines(alertSector, data, radiusIncrement, sectorWidth, canvasWrapper.canvas)
        }

        textStyle.textAlign = Align.RIGHT
        val textHeight = textStyle.getFontMetrics(null)
        for (radiusIndex in 0 until rangeSteps.size) {
            renderRangeCircle(
                radiusIndex,
                data,
                radiusIncrement,
                rangeSteps,
                textHeight,
                alertParameters,
                canvasWrapper.canvas
            )
        }
    }

    private fun renderRangeCircle(
        radiusIndex: Int,
        data: AlarmViewData,
        radiusIncrement: Float,
        rangeSteps: List<Float>,
        textHeight: Float,
        alertParameters: AlertParameters,
        canvas: Canvas
    ) {
        val isOuterCircle = radiusIndex == rangeSteps.size - 1

        if (isOuterCircle) {
            lines.strokeWidth = (data.size / 80).toFloat()
        }
        primitiveRenderer.drawCircle(
            data.center,
            (radiusIndex + 1) * radiusIncrement,
            lines,
            canvas
        )

        if (enableDescriptionText && data.size > TEXT_MINIMUM_SIZE) {
            val text = "%.0f".format(rangeSteps[radiusIndex])
            canvas.drawText(
                text,
                data.center + (radiusIndex + 0.85f) * radiusIncrement,
                data.center + textHeight / 3f,
                textStyle,
            )
            if (isOuterCircle) {
                val distanceUnit = context.getString(alertParameters.measurementSystem.unitNameString)
                canvas.drawText(
                    distanceUnit,
                    data.center + (radiusIndex + 0.85f) * radiusIncrement,
                    data.center + textHeight * 1.33f,
                    textStyle,
                )
            }
        }
    }

    private fun renderSectorSideLines(
        alertSector: AlertSector,
        data: AlarmViewData,
        radiusIncrement: Float,
        sectorWidth: Float,
        canvas: Canvas
    ) {
        val bearing = alertSector.minimumSectorBearing.toDouble()
        canvas.drawLine(
            data.center,
            data.center,
            data.center + (data.radius * sin(bearing / 180.0f * Math.PI)).toFloat(),
            data.center + (data.radius * -cos(bearing / 180.0f * Math.PI)).toFloat(),
            lines,
        )

        if (enableDescriptionText && data.size > TEXT_MINIMUM_SIZE) {
            drawSectorLabel(data.center, radiusIncrement, alertSector, bearing + sectorWidth / 2.0, canvas)
        }
    }

    private fun renderSectorBackground(
        alertSector: AlertSector,
        radiusIncrement: Float,
        data: AlarmViewData,
        actualTime: Long,
        sectorWidth: Float,
        canvas: CanvasWrapper
    ) {
        val startAngle = alertSector.minimumSectorBearing + 90f + 180f

        val ranges = alertSector.ranges
        for (rangeIndex in ranges.indices.reversed()) {
            val alertSectorRange = ranges[rangeIndex]

            val sectorRadius = (rangeIndex + 1) * radiusIncrement
            val leftTop = data.center - sectorRadius
            val bottomRight = data.center + sectorRadius

            val drawColor = alertSectorRange.strikeCount > 0
            if (drawColor) {
                colorHandler?.also {
                    sectorPaint.color = it.getColor(
                        actualTime,
                        alertSectorRange.latestStrikeTimestamp,
                        intervalDuration!!,
                    )
                }
            }
            arcArea.set(leftTop, leftTop, bottomRight, bottomRight)
            canvas.canvas.drawArc(
                arcArea,
                startAngle,
                sectorWidth,
                true,
                if (drawColor) sectorPaint else canvas.background,
            )
        }
    }

    private fun drawSectorLabel(
        center: Float,
        radiusIncrement: Float,
        sector: AlertSector,
        bearing: Double,
        canvas: Canvas,
    ) {
        if (bearing != 90.0) {
            val text = sector.label
            val textRadius = (sector.ranges.size - 0.5f) * radiusIncrement
            canvas.drawText(
                text,
                center + (textRadius * sin(bearing / 180.0 * Math.PI)).toFloat(),
                center + (textRadius * -cos(bearing / 180.0 * Math.PI)).toFloat() + textStyle.getFontMetrics(null) / 3f,
                textStyle,
            )
        }
    }

    companion object {
        private const val TEXT_MINIMUM_SIZE = 300
    }
}
