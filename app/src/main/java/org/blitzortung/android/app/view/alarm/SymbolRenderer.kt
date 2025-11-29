package org.blitzortung.android.app.view.alarm

import android.content.Context
import android.graphics.Canvas
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Paint.Align
import android.graphics.Paint.Style
import org.blitzortung.android.app.R
import org.blitzortung.android.app.view.AlarmView
import org.blitzortung.android.map.overlay.color.ColorHandler

class SymbolRenderer(
    val context: Context,
    val primitiveRenderer: PrimitiveRenderer,
    val colorHandler: ColorHandler,
    val textSize: Float
) {
    private val lines = Paint(Paint.ANTI_ALIAS_FLAG)
    private val hugeText = Paint(Paint.ANTI_ALIAS_FLAG)
    private val warnText = Paint(Paint.ANTI_ALIAS_FLAG)
    private val alarmNotAvailableTextLines: Array<String> =
        context.getString(R.string.alarms_not_available)
            .split("\n".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()

    init {
        with(lines) {
            color = 0xff404040.toInt()
            style = Style.STROKE
        }

        with(hugeText) {
            color = 0xff404040.toInt()
            textSize = textSize
        }
    }

    fun drawOutOfRangeSymbol(
        data: AlarmView.AlarmViewData,
        temporaryCanvas: Canvas,
    ) {
        with(lines) {
            color = colorHandler.lineColor
            strokeWidth = (data.size / 80).toFloat()
        }

        primitiveRenderer.drawCross(data.center, data.radius * 0.1f, lines, temporaryCanvas)
        primitiveRenderer.drawCircle(data.center, data.radius * 0.5f, lines, temporaryCanvas)
        primitiveRenderer.drawCircle(data.center, data.radius * 0.8f, lines, temporaryCanvas)
        primitiveRenderer.drawCircle(data.center, data.radius * 1.0f, lines, temporaryCanvas)
    }

    fun drawOwnLocationSymbol(
        center: Float,
        radius: Float,
        size: Int,
        temporaryCanvas: Canvas,
    ) {
        with(lines) {
            color = colorHandler.lineColor
            strokeWidth = (size / 80).toFloat()
        }

        primitiveRenderer.drawCircle(center, radius * 0.8f, lines, temporaryCanvas)
        primitiveRenderer.drawCross(center, radius * 0.6f, lines, temporaryCanvas)
    }

    fun drawNoLocationSymbol(
        center: Float,
        radius: Float,
        size: Int,
        temporaryCanvas: Canvas,
    ) {
        with(lines) {
            color = colorHandler.lineColor
            strokeWidth = (size / 80).toFloat()
            pathEffect = DashPathEffect(floatArrayOf(15f, 10f), 0f)
        }

        with(hugeText) {
            color = colorHandler.lineColor
            textSize = 3f * textSize
        }

        val noLocationText = "?"

        primitiveRenderer.drawCenteredText(temporaryCanvas, noLocationText, center, hugeText)
        primitiveRenderer.drawCircle(center, radius * 0.8f, lines, temporaryCanvas)
    }

    fun drawAlertOrLocationMissingMessage(
        center: Float,
        width: Int,
        canvas: Canvas,
    ) {
        with(warnText) {
            color = context.getColor(R.color.RedWarn)
            textAlign = Align.CENTER
            textSize = DEFAULT_FONT_SIZE.toFloat()

            val maxWidth =
                alarmNotAvailableTextLines.maxOfOrNull { warnText.measureText(it) }
                    ?: (width.toFloat() - 20)
            val scale = (width.toFloat() - 20) / maxWidth

            // Now scale the text so we can use the whole width of the canvas
            textSize = scale * DEFAULT_FONT_SIZE
        }

        for (line in alarmNotAvailableTextLines.indices) {
            canvas.drawText(
                alarmNotAvailableTextLines[line],
                center,
                center + (line - 1) * warnText.getFontMetrics(null),
                warnText,
            )
        }
    }

    companion object {
        private const val DEFAULT_FONT_SIZE = 20
    }
}
