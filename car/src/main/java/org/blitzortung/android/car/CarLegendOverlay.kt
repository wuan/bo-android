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

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.util.DisplayMetrics
import org.blitzortung.android.map.overlay.StrikeListOverlay
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Overlay

/** A single legend row: the strike colour band and the age it represents. */
internal data class LegendEntry(
    val color: Int,
    val label: String,
)

/**
 * Persistent legend drawn against the left edge of the car map surface,
 * explaining the strike age colours. One row per colour band is drawn as a
 * swatch plus an age label, so the driver can read the colours used for the
 * strikes.
 *
 * The legend reads the active colours from the strike overlay and the interval
 * from its parameters, so it follows the current colour scheme and animation
 * interval without any extra data wiring. It is drawn above every other overlay
 * (including the dimming scrim), is never interactive, and has no show/hide
 * preference.
 *
 * The host draws its own UI (the alert list, the header, the car bar) over the
 * map surface. Rather than sit beside the alert list at the top of the map, the
 * legend hugs the left edge and its bottom is anchored to the map area the host
 * reports as stable (falling back to the visible area, then the surface bottom),
 * so it stays clear of the alert list however tall that list grows. Metrics are
 * scaled from the host surface dpi rather than the presentation's resource
 * density, which comes from the car context.
 */
internal class CarLegendOverlay(
    context: Context,
    private val strikesOverlay: StrikeListOverlay,
) : Overlay() {
    private val entryFormat = context.getString(R.string.car_legend_entry)

    /** Surface density scale relative to the baseline dpi. */
    private var scale = 1f

    /** Surface height in pixels, used as the bottom anchor before any area is reported. */
    var surfaceHeight: Int = 0

    /** The map area the host designates as stable, or `null` while unknown. */
    private var stableArea: Rect? = null

    /** The map area the host reports as currently visible, or `null` while unknown. */
    private var visibleArea: Rect? = null

    private val backgroundPaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            alpha = PANEL_ALPHA
        }

    private val swatchPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    private val textPaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = TEXT_SIZE_DP
        }

    /** Surface density in dpi, set from the host surface; drives all metrics. */
    var densityDpi: Int = DisplayMetrics.DENSITY_DEFAULT
        set(value) {
            field = value
            scale = value / BASE_DENSITY_DPI
            textPaint.textSize = TEXT_SIZE_DP * scale
        }

    /**
     * Records the map area the host guarantees to be visible, so the legend's
     * bottom stays clear of host chrome such as the car bar. An empty area is
     * treated as unknown.
     */
    fun onVisibleAreaChanged(visibleArea: Rect) {
        this.visibleArea = visibleArea.takeUnless { it.isEmpty }
    }

    /**
     * Records the map area the host designates as stable. It is preferred over
     * the visible area so the legend does not move when transient host UI
     * appears. An empty area is treated as unknown.
     */
    fun onStableAreaChanged(stableArea: Rect) {
        this.stableArea = stableArea.takeUnless { it.isEmpty }
    }

    /** The area the legend is anchored to, or `null` to fall back to the surface bottom. */
    private val anchorArea: Rect?
        get() = stableArea ?: visibleArea

    private val panelInset: Float get() = PANEL_INSET_DP * scale

    private val innerPadding: Float get() = PADDING_DP * scale

    private val swatchSize: Float get() = SWATCH_SIZE_DP * scale

    private val rowHeight: Float get() = ROW_HEIGHT_DP * scale

    private val labelGap: Float get() = GAP_DP * scale

    private val cornerRadius: Float get() = CORNER_RADIUS_DP * scale

    /**
     * One entry per active strike colour: the band colour and the age it
     * represents. The bounding labels mirror the phone legend: each band is
     * `< (index + 1) * bandMinutes` and the last band is
     * `> (colors - 1) * bandMinutes`.
     */
    fun entries(): List<LegendEntry> {
        val colorHandler = strikesOverlay.colorHandler
        val numberOfColors = colorHandler.numberOfColors
        if (numberOfColors <= 0) {
            return emptyList()
        }
        val minutesPerColor = strikesOverlay.parameters.intervalDuration / numberOfColors
        return (0 until numberOfColors).map { index ->
            val isLastValue = index == numberOfColors - 1
            LegendEntry(
                color = colorHandler.getColor(index),
                label =
                    entryFormat.format(
                        if (isLastValue) ">" else "<",
                        (index + if (isLastValue) 0 else 1) * minutesPerColor,
                    ),
            )
        }
    }

    /** The panel rectangle in surface pixels, against the left edge above the host's chrome. */
    internal fun panelBounds(): RectF = panelBounds(entries())

    private fun panelBounds(rows: List<LegendEntry>): RectF {
        val textWidth = rows.maxOfOrNull { textPaint.measureText(it.label) } ?: 0f
        val width = innerPadding + swatchSize + labelGap + textWidth + innerPadding
        val height = innerPadding + rows.size * rowHeight + innerPadding
        val left = panelInset
        val bottom = (anchorArea?.bottom ?: surfaceHeight).toFloat() - panelInset
        return RectF(left, bottom - height, left + width, bottom)
    }

    override fun draw(
        canvas: Canvas?,
        mapView: MapView?,
        shadow: Boolean,
    ) {
        val targetCanvas = canvas ?: return
        val rows = entries()
        if (shadow || rows.isEmpty()) {
            return
        }

        val panel = panelBounds(rows)
        targetCanvas.drawRoundRect(panel, cornerRadius, cornerRadius, backgroundPaint)

        rows.forEachIndexed { index, row ->
            val rowTop = panel.top + innerPadding + index * rowHeight
            val swatchTop = rowTop + (rowHeight - swatchSize) / 2f
            val swatchRect =
                RectF(
                    panel.left + innerPadding,
                    swatchTop,
                    panel.left + innerPadding + swatchSize,
                    swatchTop + swatchSize,
                )
            swatchPaint.color = row.color
            targetCanvas.drawRect(swatchRect, swatchPaint)

            val textBaseline = rowTop + rowHeight / 2f - (textPaint.descent() + textPaint.ascent()) / 2f
            targetCanvas.drawText(row.label, swatchRect.right + labelGap, textBaseline, textPaint)
        }
    }

    private companion object {
        /** Baseline dpi the metric constants are expressed in. */
        const val BASE_DENSITY_DPI = 160f

        /** Opacity of the dark panel behind the legend rows. */
        const val PANEL_ALPHA = 200

        const val PANEL_INSET_DP = 12f
        const val PADDING_DP = 8f
        const val SWATCH_SIZE_DP = 12f
        const val ROW_HEIGHT_DP = 20f
        const val GAP_DP = 8f
        const val CORNER_RADIUS_DP = 8f
        const val TEXT_SIZE_DP = 12f
    }
}
