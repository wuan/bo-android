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

package org.blitzortung.android.map.overlay

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Point
import org.osmdroid.api.IGeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.Projection

class StrikeShape(private val center: IGeoPoint) : LightningShape {
    override fun isPointInside(tappedGeoPoint: IGeoPoint, projection: Projection): Boolean {
        val shapeCenter = Point()
        projection.toPixels(center, shapeCenter)

        val tappedPoint = Point()
        projection.toPixels(tappedGeoPoint, tappedPoint)

        return tappedPoint.x >= shapeCenter.x - size / 2 && tappedPoint.x <= shapeCenter.x + size / 2
                && tappedPoint.y >= shapeCenter.y - size / 2 && tappedPoint.y <= shapeCenter.y + size / 2
    }

    private var size: Float = 0.toFloat()
    private var color: Int = 0

    override fun draw(canvas: Canvas, mapView: MapView, paint: Paint) {
        mapView.projection.toPixels(center, centerPoint)

        //Only draw it when its visible
        if (canvas.quickReject(
                        centerPoint.x - size / 2,
                        centerPoint.y - size / 2,
                        centerPoint.x + size / 2,
                        centerPoint.y + size / 2,
                        Canvas.EdgeType.BW)) {
            return
        }

        paint.color = color
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = size / 4

        canvas.drawLine(
                centerPoint.x - size / 2,
                centerPoint.y.toFloat(),
                centerPoint.x + size / 2,
                centerPoint.y.toFloat(),
                paint)

        canvas.drawLine(
                centerPoint.x.toFloat(),
                centerPoint.y - size / 2,
                centerPoint.x.toFloat(),
                centerPoint.y + size / 2,
                paint)
    }

    fun update(size: Float, color: Int) {
        this.size = size
        this.color = color
    }

    companion object {
        private val centerPoint = Point()
    }
}
