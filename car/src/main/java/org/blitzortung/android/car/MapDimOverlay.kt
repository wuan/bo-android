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

package org.blitzortung.android.car

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Overlay

/**
 * Translucent black scrim over the map background. It is placed as the first
 * overlay so osmdroid draws it above the map tiles but below the strike and grid
 * overlays, dimming only the background and leaving the lightning data readable.
 */
internal class MapDimOverlay : Overlay() {
    private val paint = Paint().apply { color = Color.BLACK }

    /** Overlay opacity: 0 is fully transparent (not dimmed), 255 fully opaque. */
    var alpha: Int = 0
        set(value) {
            field = value.coerceIn(0, 255)
            paint.alpha = field
        }

    override fun draw(
        canvas: Canvas?,
        mapView: MapView?,
        shadow: Boolean,
    ) {
        if (shadow || alpha == 0) {
            return
        }
        val targetCanvas = canvas ?: return
        targetCanvas.drawRect(targetCanvas.clipBounds, paint)
    }
}
