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
import android.graphics.drawable.shapes.Shape

class StrikeShape : Shape() {

    private var size: Float = 0.toFloat()
    private var color: Int = 0

    override fun draw(canvas: Canvas, paint: Paint) {
        paint.color = color
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = size / 4
        canvas.drawLine(-size / 2, 0.0f, size / 2, 0.0f, paint)
        canvas.drawLine(0.0f, -size / 2, 0.0f, size / 2, paint)
    }

    fun update(size: Float, color: Int) {
        this.size = size
        this.color = color
    }
}
