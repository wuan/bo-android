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
 * Translucent scrim over the map background. It is placed as the first overlay
 * so osmdroid draws it above the map tiles but below the strike and grid
 * overlays, shading only the background and leaving the lightning data readable.
 *
 * The scrim has a direction: a black wash for [MapShading.DARK] and a white wash
 * for [MapShading.BRIGHT]. The two directions have independent alphas because
 * black and white read differently over the MAPNIK tiles.
 */
internal class MapShadeOverlay : Overlay() {
    private val paint = Paint()

    /** Scrim colour; [Color.BLACK] for dark shading, [Color.WHITE] for bright. */
    var color: Int = Color.BLACK
        set(value) {
            field = value
            paint.color = value
        }

    /** Overlay opacity: 0 draws nothing, 255 is fully opaque. */
    var alpha: Int = 0
        set(value) {
            field = value.coerceIn(0, 255)
            paint.alpha = field
        }

    /** Applies the scrim for [shading]. */
    fun applyShading(shading: MapShading) {
        when (shading) {
            MapShading.DARK -> {
                color = Color.BLACK
                alpha = DARK_ALPHA
            }

            MapShading.BRIGHT -> {
                color = Color.WHITE
                alpha = BRIGHT_ALPHA
            }
        }
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

    internal companion object {
        /** Opacity of the dark wash: keeps today's night behaviour. */
        const val DARK_ALPHA = 128

        /** Opacity of the bright wash: matches the phone's 55% bright-map fade. */
        const val BRIGHT_ALPHA = 140
    }
}
