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
import android.graphics.Paint.Align
import android.graphics.Point
import android.graphics.RectF
import org.osmdroid.api.IGeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.Projection

class RasterShape(private val center: IGeoPoint) : LightningShape {

    private val size: RectF = RectF()
    private var color: Int = 0
    private var alpha: Int = 0
    private var multiplicity: Int = 0
    private var textColor: Int = 0

    override fun draw(canvas: Canvas, mapView: MapView, paint: Paint) {
        val centerPoint = Point()
        mapView.projection.toPixels(center, centerPoint)

        val rect = RectF(
                centerPoint.x + size.left,
                centerPoint.y + size.top,
                centerPoint.x + size.right,
                centerPoint.y + size.bottom)

        //Only draw visible Raster-Items
        if (canvas.quickReject(rect, Canvas.EdgeType.BW)) {
            return
        }

        paint.color = color
        paint.alpha = alpha
        canvas.drawRect(rect, paint)

        val textSize = rect.height() / 2.5f
        if (textSize >= 8f) {
            paint.color = textColor
            paint.alpha = calculateAlphaValue(textSize, 20, 80, 255, 60)
            paint.textAlign = Align.CENTER
            paint.textSize = textSize
            canvas.drawText(
                    multiplicity.toString(),
                    centerPoint.x.toFloat(),
                    centerPoint.y.toFloat() + textSize / 2,
                    paint)
        }
    }

    override fun isPointInside(tappedGeoPoint: IGeoPoint, projection: Projection): Boolean {
        val shapeCenter = Point()
        projection.toPixels(center, shapeCenter)

        val tappedPoint = Point()
        projection.toPixels(tappedGeoPoint, tappedPoint)

        return tappedPoint.x >= shapeCenter.x + size.left && tappedPoint.x <= shapeCenter.x + size.right
                && tappedPoint.y >= shapeCenter.y + size.top && tappedPoint.y <= shapeCenter.y + size.bottom
    }

    fun update(topLeft: Point, bottomRight: Point, color: Int, multiplicity: Int, textColor: Int) {
        val x1 = Math.min(topLeft.x.toFloat(), -MIN_SIZE)
        val y1 = Math.min(topLeft.y.toFloat(), -MIN_SIZE)
        val x2 = Math.max(bottomRight.x.toFloat(), MIN_SIZE)
        val y2 = Math.max(bottomRight.y.toFloat(), MIN_SIZE)
        size.set(x1, y1, x2, y2)

        this.multiplicity = multiplicity
        this.color = color
        this.textColor = textColor

        alpha = calculateAlphaValue(size.width(), 10, 40, 255, 100)
    }

    private fun calculateAlphaValue(value: Float, minValue: Int, maxValue: Int, maxAlpha: Int, minAlpha: Int): Int {
        val targetValue = coerceToRange((value - minValue) / (maxValue - minValue), 0.0f, 1.0f)
        return minAlpha + ((maxAlpha - minAlpha) * (1.0 - targetValue)).toInt()
    }

    private fun coerceToRange(value: Float, lowerBound: Float, upperBound: Float): Float {
        return Math.min(Math.max(value, lowerBound), upperBound)
    }

    companion object {
        private const val MIN_SIZE = 1.5f
    }
}
