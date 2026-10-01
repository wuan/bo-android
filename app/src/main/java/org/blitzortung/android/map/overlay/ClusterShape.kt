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
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView

/**
 * Draws a cluster's polygon outline (the closed `[lon, lat]` ring from [shape])
 * as a stroked path in the age-derived [color]. No fill, no hit-testing.
 */
class ClusterShape(private val shape: List<Pair<Double, Double>>) {
    private var color: Int = 0

    fun update(color: Int) {
        this.color = color
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

        paint.style = Paint.Style.STROKE
        paint.strokeWidth = strokeWidth
        paint.color = color
        canvas.drawPath(path, paint)
    }
}
