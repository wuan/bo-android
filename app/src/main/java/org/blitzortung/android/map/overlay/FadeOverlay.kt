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
import org.blitzortung.android.map.overlay.color.ColorHandler
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Overlay

class FadeOverlay(private val colorHandler: ColorHandler) : Overlay() {

    private var alphaValue = 0

    override fun draw(canvas: Canvas?, mapView: MapView?, shadow: Boolean) {
        if (!shadow) {
            val rect = canvas!!.clipBounds
            val paint = Paint()
            paint.color = colorHandler.backgroundColor
            paint.alpha = alphaValue
            canvas.drawRect(rect, paint)
        }
    }

    fun setAlpha(alphaValue: Int) {
        this.alphaValue = alphaValue
    }
}
