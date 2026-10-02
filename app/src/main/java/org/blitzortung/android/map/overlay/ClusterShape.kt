/*

   Copyright 2026 Andreas Würl

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

package org.blitzortung.android.map.overlay

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Paint.Align
import android.graphics.Path
import android.graphics.Point
import android.graphics.RectF
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView

/**
 * Draws a cluster's polygon outline (the closed `[lon, lat]` ring from [shape])
 * as a stroked path in the age-derived [color]. The current (newest) clusters are
 * additionally filled with a translucent tint and labelled with their strike count.
 */
class ClusterShape(private val shape: List<Pair<Double, Double>>) {
    private var color: Int = 0
    private var textColor: Int = 0
    private var strikeCount: Int = 0
    private var current: Boolean = false

    fun update(
        color: Int,
        textColor: Int,
        strikeCount: Int,
        current: Boolean,
    ) {
        this.color = color
        this.textColor = textColor
        this.strikeCount = strikeCount
        this.current = current
    }

    fun draw(
        canvas: Canvas,
        mapView: MapView,
        paint: Paint,
        strokeWidth: Float,
    ) {
        if (shape.size < 2) {
            return
        }

        val path = Path()
        val point = Point()
        shape.forEachIndexed { index, coordinate ->
            mapView.projection.toPixels(GeoPoint(coordinate.second, coordinate.first), point)
            if (index == 0) {
                path.moveTo(point.x.toFloat(), point.y.toFloat())
            } else {
                path.lineTo(point.x.toFloat(), point.y.toFloat())
            }
        }
        path.close()

        if (current) {
            paint.style = Paint.Style.FILL
            paint.color = color
            paint.alpha = FILL_ALPHA
            canvas.drawPath(path, paint)
        }

        paint.alpha = 255
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = strokeWidth
        paint.color = color
        canvas.drawPath(path, paint)

        if (current && mapView.zoomLevelDouble >= MIN_LABEL_ZOOM_LEVEL) {
            drawStrikeCount(canvas, paint, path)
        }
    }

    private fun drawStrikeCount(
        canvas: Canvas,
        paint: Paint,
        path: Path,
    ) {
        val bounds = RectF()
        path.computeBounds(bounds, true)

        val textSize = bounds.height() / TEXT_SIZE_DIVISOR
        if (textSize < MIN_TEXT_SIZE) {
            return
        }

        val drawnTextSize = textSize.coerceAtMost(MAX_TEXT_SIZE)
        paint.color = textColor
        paint.alpha = calculateShapeAlpha(drawnTextSize, 20, 80, 255, 60)
        paint.textAlign = Align.CENTER
        paint.textSize = drawnTextSize

        val textY = bounds.centerY() - (paint.descent() + paint.ascent()) / 2f
        canvas.drawText(strikeCount.toString(), bounds.centerX(), textY, paint)
    }

    companion object {
        /** 20% opacity for the fill of the current clusters. */
        private const val FILL_ALPHA = 51

        /** Half the grid's divisor, so cluster labels are drawn twice as large. */
        private const val TEXT_SIZE_DIVISOR = 1.25f
        private const val MIN_TEXT_SIZE = 8f
        private const val MAX_TEXT_SIZE = 56f
        private const val MIN_LABEL_ZOOM_LEVEL = 9.0
    }
}
