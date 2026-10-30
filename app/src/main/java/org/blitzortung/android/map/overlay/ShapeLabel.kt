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
import android.graphics.RectF

/**
 * Draws a numeric label centred inside [bounds] using the shared grid/cluster text
 * style. Shapes too small to hold a legible number are left unlabelled, and the label
 * fades out for larger shapes via [calculateShapeAlpha]. Shared by the grid and
 * cluster overlays so their counts are rendered identically.
 */
internal fun drawShapeLabel(
    canvas: Canvas,
    paint: Paint,
    bounds: RectF,
    text: String,
    textColor: Int,
) {
    val textSize = bounds.height() / SHAPE_LABEL_TEXT_SIZE_DIVISOR
    if (textSize < MIN_SHAPE_LABEL_TEXT_SIZE) {
        return
    }

    paint.color = textColor
    paint.alpha = calculateShapeAlpha(textSize, 20, 80, 255, 60)
    paint.textAlign = Align.CENTER
    paint.textSize = textSize
    // Shapes may leave the shared paint in STROKE mode (e.g. cluster outlines), which
    // would render the text hollow; force a plain filled label.
    paint.style = Paint.Style.FILL
    canvas.drawText(text, bounds.centerX(), bounds.centerY() + textSize / 2f, paint)
}

/** Label height as a fraction of the shape height, shared by grid and cluster counts. */
internal const val SHAPE_LABEL_TEXT_SIZE_DIVISOR = 2.5f

/** Shapes whose label would be smaller than this are not labelled at all. */
internal const val MIN_SHAPE_LABEL_TEXT_SIZE = 8f
