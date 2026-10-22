package org.blitzortung.android.app.view

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.util.Log
import androidx.preference.PreferenceManager
import org.blitzortung.android.app.Main.Companion.LOG_TAG
import org.blitzortung.android.app.R
import org.blitzortung.android.data.beans.GridParameters
import org.blitzortung.android.data.provider.result.DataEvent
import org.blitzortung.android.data.provider.result.DataReceived
import org.blitzortung.android.util.TabletAwareView
import org.osmdroid.events.MapListener
import org.osmdroid.events.ScrollEvent
import org.osmdroid.events.ZoomEvent
import org.osmdroid.util.BoundingBox

private const val SMALL_TEXT_SCALE = 0.6f

class RegionView
    @JvmOverloads
    constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyle: Int = 0,
    ) : TabletAwareView(context, attrs, defStyle), MapListener, OnSharedPreferenceChangeListener {
        private val backgroundPaint: Paint = Paint(Paint.ANTI_ALIAS_FLAG)
        private val foregroundPaint: Paint = Paint(Paint.ANTI_ALIAS_FLAG)
        private val textPaint: Paint
        private val defaultForegroundColor: Int
        private val backgroundRect: RectF
        private var mapArea: BoundingBox? = null
        private var zoomLevel: Double? = null

        private var gridParameters: GridParameters? = null

        val dataConsumer = { event: DataEvent ->
            if (event is DataReceived) {
                updateHistogram(event)
            }
        }

        init {
            backgroundPaint.color = 0x00b0b0b0

            defaultForegroundColor = context.getColor(R.color.text_foreground)

            textPaint =
                Paint(Paint.ANTI_ALIAS_FLAG).apply {
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

        override fun onMeasure(
            widthMeasureSpec: Int,
            heightMeasureSpec: Int,
        ) {
            val getSize = fun(spec: Int) = MeasureSpec.getSize(spec)

            val parentWidth = getSize(widthMeasureSpec) * sizeFactor
            val parentHeight = getSize(heightMeasureSpec) * sizeFactor

            super.onMeasure(
                MeasureSpec.makeMeasureSpec(parentWidth.toInt(), MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(parentHeight.toInt(), MeasureSpec.EXACTLY),
            )
        }

        override fun onDraw(canvas: Canvas) {
            backgroundRect.set(0f, 0f, width.toFloat(), height.toFloat())
            canvas.drawRect(backgroundRect, backgroundPaint)

            var topCoordinate = 2 * padding

            gridParameters?.let {
                if (!it.isGlobal) {
                    topCoordinate = drawGridInfo(canvas, it, topCoordinate)
                }

                drawZoomLevel(canvas)
            }
        }

        private fun drawGridInfo(
            canvas: Canvas,
            gridParameters: GridParameters,
            topCoordinate: Float,
        ): Float {
            var currentTop = topCoordinate

            val text =
                "%.1f..%.1f  %.1f..%.1f".format(
                    gridParameters.longitudeStart,
                    gridParameters.longitudeEnd,
                    gridParameters.latitudeEnd,
                    gridParameters.latitudeStart,
                )
            canvas.drawText(
                text,
                width - 2 * padding,
                currentTop + textSize / 1.2f * SMALL_TEXT_SCALE,
                textPaint,
            )
            currentTop += (textSize + padding) * SMALL_TEXT_SCALE

            val xdelta = gridParameters.longitudeEnd - gridParameters.longitudeStart
            val ydelta = gridParameters.latitudeStart - gridParameters.latitudeEnd
            val text2 =
                "%.1f  %.1f".format(
                    xdelta,
                    ydelta,
                )
            canvas.drawText(
                text2,
                width - 2 * padding,
                currentTop + textSize / 1.2f * SMALL_TEXT_SCALE,
                textPaint,
            )
            currentTop += (textSize + padding) * SMALL_TEXT_SCALE

            val x0 = gridParameters.longitudeStart
            val y0 = gridParameters.latitudeStart

            val xs = xdelta / (width - 2 * padding)
            val ys = ydelta / (height - 2 * padding)

            mapArea?.let { area ->
                val x1 = padding + ((area.lonEast - x0) / xs).toFloat()
                val x2 = padding + ((area.lonWest - x0) / xs).toFloat()
                val y1 = padding + ((y0 - area.latNorth) / ys).toFloat()
                val y2 = padding + ((y0 - area.latSouth) / ys).toFloat()

                foregroundPaint.strokeWidth = 1f
                drawBox(canvas, RectF(x1, y1, x2, y2), foregroundPaint)
            }

            foregroundPaint.strokeWidth = 3f
            foregroundPaint.color = defaultForegroundColor
            drawBox(canvas, RectF(padding, padding, width - padding, height - padding), foregroundPaint)

            return currentTop
        }

        private fun drawZoomLevel(canvas: Canvas) {
            zoomLevel?.let {
                val text = "Zoom %.1f".format(it)
                canvas.drawText(
                    text,
                    width - 2 * padding,
                    height - 2 * padding,
                    textPaint,
                )
            }
        }

        private fun drawBox(
            canvas: Canvas,
            box: RectF,
            paint: Paint,
        ) {
            canvas.drawLine(box.left, box.bottom, box.right, box.bottom, paint)
            canvas.drawLine(box.left, box.top, box.right, box.top, paint)
            canvas.drawLine(box.right, box.top, box.right, box.bottom, paint)
            canvas.drawLine(box.left, box.top, box.left, box.bottom, paint)
        }

        private fun updateHistogram(dataEvent: DataReceived) {
            if (dataEvent.failed) {
                visibility = INVISIBLE
                gridParameters = null
            } else {
                gridParameters = dataEvent.gridParameters

                invalidate()
            }
        }

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
                Log.d(LOG_TAG, "RebionView.onZoom(): ${event.zoomLevel}")
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
            key: PreferenceKey,
        ) {
            when (key) {
                PreferenceKey.DIAGNOSIS_ENABLED -> {
                    val diagnosisEnabled = sharedPreferences.get(key, false)
                    visibility =
                        if (diagnosisEnabled) {
                            VISIBLE
                        } else {
                            INVISIBLE
                        }
                }

                else -> {}
            }
        }
    }
