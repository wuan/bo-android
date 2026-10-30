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
        val worldSize = mapView.projection.worldMapSize
        var referenceX = 0.0
        shape.forEachIndexed { index, coordinate ->
            mapView.projection.toPixels(GeoPoint(coordinate.second, coordinate.first), point)
            val x =
                if (index == 0) {
                    point.x.toDouble()
                } else {
                    wrapPixelX(point.x.toDouble(), referenceX, worldSize)
                }
            referenceX = x
            if (index == 0) {
                path.moveTo(x.toFloat(), point.y.toFloat())
            } else {
                path.lineTo(x.toFloat(), point.y.toFloat())
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
        paint.strokeWidth = if (current) strokeWidth * CURRENT_STROKE_MULTIPLIER else strokeWidth
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

        drawShapeLabel(canvas, paint, bounds, strikeCount.toString(), textColor)
    }

    companion object {
        /** 20% opacity for the fill of the current clusters. */
        private const val FILL_ALPHA = 51

        /** The current (newest) clusters are drawn with a thicker outline than the older ones. */
        private const val CURRENT_STROKE_MULTIPLIER = 2f

        private const val MIN_LABEL_ZOOM_LEVEL = 9.0
    }
}

/**
 * Shifts a projected pixel [x] by whole world widths so that it lands in the world copy
 * closest to [referenceX] (the previous vertex). osmdroid clamps longitudes to
 * `[-180, 180]` and picks the visible world copy for every vertex independently, so a
 * cluster ring crossing the date line would otherwise have consecutive vertices placed
 * on opposite edges of the map and be drawn as a long straight line across it.
 */
internal fun wrapPixelX(
    x: Double,
    referenceX: Double,
    worldSize: Double,
): Double {
    if (worldSize <= 0.0) {
        return x
    }

    var result = x
    while (result - referenceX > worldSize / 2) {
        result -= worldSize
    }
    while (referenceX - result > worldSize / 2) {
        result += worldSize
    }
    return result
}
