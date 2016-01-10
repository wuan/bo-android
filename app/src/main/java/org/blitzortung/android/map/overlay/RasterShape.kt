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
import android.graphics.drawable.shapes.Shape

class RasterShape : Shape() {

    private val rect: RectF
    private var color: Int = 0
    private var alpha: Int = 0
    private var multiplicity: Int = 0
    private var textColor: Int = 0

    init {
        rect = RectF()
    }

    override fun draw(canvas: Canvas, paint: Paint) {
        paint.color = color
        paint.alpha = alpha
        canvas.drawRect(rect, paint)

        val textSize = rect.height() / 2.5f
        if (textSize >= 8f) {
            paint.color = textColor
            paint.alpha = calculateAlphaValue(textSize, 20, 80, 255, 60)
            paint.textAlign = Align.CENTER
            paint.textSize = textSize
            canvas.drawText(multiplicity.toString(), 0.0f, 0.4f * textSize, paint)
        }
    }

    override fun hasAlpha(): Boolean {
        return alpha != 255
    }

    fun update(topLeft: Point, bottomRight: Point, color: Int, multiplicity: Int, textColor: Int) {
        val x1 = Math.min(topLeft.x.toFloat(), -MIN_SIZE)
        val y1 = Math.min(topLeft.y.toFloat(), -MIN_SIZE)
        val x2 = Math.max(bottomRight.x.toFloat(), MIN_SIZE)
        val y2 = Math.max(bottomRight.y.toFloat(), MIN_SIZE)
        rect.set(x1, y1, x2, y2)
        resize(rect.width(), rect.height())

        this.multiplicity = multiplicity
        this.color = color
        this.textColor = textColor

        alpha = calculateAlphaValue(rect.width(), 10, 40, 255, 100)
    }

    private fun calculateAlphaValue(value: Float, minValue: Int, maxValue: Int, maxAlpha: Int, minAlpha: Int): Int {
        val targetValue = coerceToRange((value - minValue) / (maxValue - minValue), 0.0f, 1.0f)
        return minAlpha + ((maxAlpha - minAlpha) * (1.0 - targetValue)).toInt()
    }

    private fun coerceToRange(value: Float, lowerBound: Float, upperBound: Float): Float {
        return Math.min(Math.max(value, lowerBound), upperBound)
    }

    companion object {
        private val MIN_SIZE = 1.5f
    }
}
