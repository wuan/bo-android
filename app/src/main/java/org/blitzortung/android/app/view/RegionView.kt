package org.blitzortung.android.app.view

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.preference.PreferenceManager
import android.util.AttributeSet
import org.blitzortung.android.app.R
import org.blitzortung.android.data.beans.GridParameters
import org.blitzortung.android.data.provider.result.ResultEvent
import org.blitzortung.android.protocol.Event
import org.blitzortung.android.util.TabletAwareView
import org.osmdroid.events.MapListener
import org.osmdroid.events.ScrollEvent
import org.osmdroid.events.ZoomEvent
import org.osmdroid.util.BoundingBox

private const val SMALL_TEXT_SCALE = 0.6f

class RegionView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : TabletAwareView(context, attrs, defStyle), MapListener, OnSharedPreferenceChangeListener {

    private val backgroundPaint: Paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val foregroundPaint: Paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint: Paint
    private val defaultForegroundColor: Int
    private val backgroundRect: RectF
    private var mapArea: BoundingBox? = null
    private var zoomLevel: Double? = null

    private var gridParameters: GridParameters? = null

    val dataConsumer = { event: Event ->
        if (event is ResultEvent) {
            updateHistogram(event)
        }
    }

    init {
        backgroundPaint.color = 0x00b0b0b0

        defaultForegroundColor = context.resources.getColor(R.color.text_foreground)

        textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = defaultForegroundColor
            textSize = this@RegionView.textSize * SMALL_TEXT_SCALE
            textAlign = Paint.Align.RIGHT
        }

        foregroundPaint.strokeWidth = 5f

        backgroundRect = RectF()

        val preferences = PreferenceManager.getDefaultSharedPreferences(context)
        preferences.registerOnSharedPreferenceChangeListener(this)
        onSharedPreferenceChanged(preferences, PreferenceKey.DIAGNOSIS_ENABLED)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val getSize = fun(spec: Int) = MeasureSpec.getSize(spec)

        val parentWidth = getSize(widthMeasureSpec) * sizeFactor
        val parentHeight = getSize(heightMeasureSpec) * sizeFactor

        super.onMeasure(
            MeasureSpec.makeMeasureSpec(parentWidth.toInt(), MeasureSpec.EXACTLY),
            MeasureSpec.makeMeasureSpec(parentHeight.toInt(), MeasureSpec.EXACTLY)
        )
    }

    override fun onDraw(canvas: Canvas) {

        backgroundRect.set(0f, 0f, width.toFloat(), height.toFloat())
        canvas.drawRect(backgroundRect, backgroundPaint)


        var topCoordinate = 2 * padding

        gridParameters?.let {
            if (!it.isGlobal) {
                val text = "%.1f..%.1f  %.1f..%.1f".format(
                    it.longitudeStart,
                    it.longitudeEnd,
                    it.latitudeEnd,
                    it.latitudeStart
                )
                canvas.drawText(
                    text,
                    width - 2 * padding,
                    topCoordinate + textSize / 1.2f * SMALL_TEXT_SCALE,
                    textPaint
                )
                topCoordinate += (textSize + padding) * SMALL_TEXT_SCALE

                val xdelta = it.longitudeEnd - it.longitudeStart
                val ydelta = it.latitudeStart - it.latitudeEnd
                val text2 = "%.1f  %.1f".format(
                    xdelta,
                    ydelta,
                )
                canvas.drawText(
                    text2,
                    width - 2 * padding,
                    topCoordinate + textSize / 1.2f * SMALL_TEXT_SCALE,
                    textPaint
                )
                topCoordinate += (textSize + padding) * SMALL_TEXT_SCALE

                val x0 = it.longitudeStart
                val y0 = it.latitudeStart

                val xs = xdelta / (width - 2 * padding)
                val ys = ydelta / (height - 2 * padding)

                mapArea?.let {
                    val x1 = padding + ((it.lonEast - x0) / xs).toFloat()
                    val x2 = padding + ((it.lonWest - x0) / xs).toFloat()
                    val y1 = padding + ((y0 - it.latNorth) / ys).toFloat()
                    val y2 = padding + ((y0 - it.latSouth) / ys).toFloat()

                    foregroundPaint.strokeWidth = 1f
                    drawBox(canvas, x1, y1, x2, y2, foregroundPaint)
                }
                topCoordinate += textSize

                foregroundPaint.strokeWidth = 3f
                foregroundPaint.color = defaultForegroundColor
                drawBox(canvas, padding, padding, width - padding, height - padding, foregroundPaint)
            }

            zoomLevel?.let {
                val text = "Zoom %.1f".format(it)
                canvas.drawText(
                    text,
                    width - 2 * padding,
                    height - 2 * padding,
                    textPaint
                )
            }
        }
    }

    private fun drawBox(
        canvas: Canvas,
        x1: Float,
        y1: Float,
        x2: Float,
        y2: Float,
        paint: Paint
    ) {
        canvas.drawLine(x1, y2, x2, y2, paint)
        canvas.drawLine(x1, y1, x2, y1, paint)
        canvas.drawLine(x2, y1, x2, y2, paint)
        canvas.drawLine(x1, y1, x1, y2, paint)
    }


    private fun updateHistogram(dataEvent: ResultEvent) {
        if (dataEvent.failed) {
            visibility = INVISIBLE
            gridParameters = null
        } else {
            gridParameters = dataEvent.gridParameters

            invalidate()
        }
    }

    private fun isVisible(gridParameters: GridParameters?) = gridParameters != null && !gridParameters.isGlobal
    override fun onScroll(event: ScrollEvent?): Boolean {
        return if (event != null) {
            this.mapArea = event.source.boundingBox
            zoomLevel = event.source.zoomLevelDouble
            updateViewSize()
            true
        } else {
            false
        }
    }

    override fun onZoom(event: ZoomEvent?): Boolean {
        return if (event != null) {
            mapArea = event.source.boundingBox
            zoomLevel = event.zoomLevel
            updateViewSize()
            true
        } else {
            false
        }
    }

    private fun updateViewSize() {
        mapArea?.let {
            val lonDelta = it.lonEast - it.lonWest
            val pixelSize = lonDelta / width
            val latDelta = it.latNorth - it.latSouth
            val height = Math.max((latDelta / pixelSize).toInt(), (3 * padding + 3 * textSize).toInt())
            layoutParams.height = height
            invalidate()
        }
    }

    override fun onSharedPreferenceChanged(
        sharedPreferences: SharedPreferences,
        key: PreferenceKey
    ) {
        when (key) {
            PreferenceKey.DIAGNOSIS_ENABLED -> {
                val diagnosisEnabled = sharedPreferences.get(key, false)
                visibility = if (diagnosisEnabled) {
                    VISIBLE
                } else {
                    INVISIBLE
                }
            }

            else -> {}
        }
    }

}