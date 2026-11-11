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
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.util.DisplayMetrics
import org.blitzortung.android.map.overlay.StrikeListOverlay
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Overlay

/**
 * The continuous strike-age band: the active palette from newest to oldest and
 * the three age labels from the top of the band to the bottom.
 */
internal data class LegendBand(
    val colors: List<Int>,
    val labels: List<String>,
)

/**
 * Persistent panel drawn in the bottom-left of the car map surface, showing the
 * timestamp of the current data and a legend that explains the strike age
 * colours. The legend is one continuous vertical band built from the active
 * palette, labelled at the top, middle and bottom of the interval, so the driver
 * can read the colours used for the strikes.
 *
 * The data timestamp is drawn by the app rather than through the host's map
 * header: the host controls the header font size and rebuilds the template when
 * the header text changes. Drawing the timestamp here keeps it small, lets it
 * update without rebuilding the host template, and uses the same font size as
 * the legend labels.
 *
 * The legend reads the active colours from the strike overlay and the interval
 * from its parameters, so it follows the current colour scheme and animation
 * interval without any extra data wiring. It is drawn above every other overlay
 * (including the shading scrim), is never interactive, and has no show/hide
 * preference.
 *
 * The host draws its own UI (the alert list, the car bar) over the map surface.
 * The panel anchors to the bottom-left of the host's map area with the same inset
 * on both edges, so it stays visually centred in that corner. The map area's
 * bottom comes from the visible/stable area reported by the host, which excludes
 * host chrome such as the car bar; the surface height is only a fallback while no
 * area is known. Metrics are scaled from the host surface dpi rather than the
 * presentation's resource density, which comes from the car context.
 */
internal class CarLegendOverlay(
    context: Context,
    private val strikesOverlay: StrikeListOverlay,
) : Overlay() {
    private val ageNowLabel = context.getString(R.string.car_legend_age_now)

    private val ageMinutesFormat = context.getString(R.string.car_legend_age_minutes)

    /**
     * Legend and status-line text size in dp at the baseline density. Read from a
     * resource so the automotive build can use a larger value for the vehicle
     * display distance without changing the phone/Android Auto build.
     */
    private val baseTextSizeDp: Float = context.resources.getInteger(R.integer.car_legend_text_size_dp).toFloat()

    /** Status line above the legend, for example `Data 12:34:56`. */
    var dataTime: String = ""

    /** Surface density scale relative to the baseline dpi. */
    private var scale = 1f

    /** Height of the map surface, used to keep the panel's bottom inset symmetric to its left inset. */
    internal var surfaceHeight: Int = 0

    /** The map area the host designates as stable, or `null` while unknown. */
    private var stableArea: Rect? = null

    /** The map area the host reports as currently visible, or `null` while unknown. */
    private var visibleArea: Rect? = null

    private val backgroundPaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            alpha = PANEL_ALPHA
        }

    /** Paints the colour band; only its shader changes between frames. */
    private val bandPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    private val textPaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = baseTextSizeDp
        }

    /** Cached band gradient, rebuilt only when the palette, bounds or density changes. */
    private var gradient: LinearGradient? = null
    private var gradientColors: List<Int>? = null
    private var gradientBounds: RectF? = null
    private var gradientScale = 0f

    /** Number of gradient shaders built; tests use it to verify the shader cache. */
    internal var gradientBuilds: Int = 0
        private set

    /** Effective legend and status-line text size in pixels for the current surface density. */
    internal val textSizePx: Float
        get() = textPaint.textSize

    /** Half a text line, reserved above and below the band for the label overhang. */
    internal val labelOverhangPx: Float
        get() = textPaint.fontSpacing / 2f

    /** Surface density in dpi, set from the host surface; drives all metrics. */
    var densityDpi: Int = DisplayMetrics.DENSITY_DEFAULT
        set(value) {
            field = value
            scale = value / BASE_DENSITY_DPI
            textPaint.textSize = baseTextSizeDp * scale
        }

    /**
     * Records the map area the host guarantees to be visible, so the legend
     * starts below the header. An empty area is treated as unknown.
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

    /** The area the legend must stay inside, or `null` while the host reports none. */
    private val anchorArea: Rect?
        get() = stableArea ?: visibleArea

    /**
     * The bottom edge of the host's map area in surface pixels. The reported area
     * excludes host chrome such as the AAOS car bar, unlike the full surface
     * height, so preferring it keeps the panel inside the usable map area.
     */
    private val anchorBottom: Float
        get() {
            val areaBottom = anchorArea?.bottom?.toFloat() ?: 0f
            val surfaceBottom = surfaceHeight.toFloat()
            return when {
                areaBottom > 0f && surfaceBottom > 0f -> minOf(areaBottom, surfaceBottom)
                areaBottom > 0f -> areaBottom
                else -> surfaceBottom
            }
        }

    private val panelInset: Float get() = PANEL_INSET_DP * scale

    private val innerPadding: Float get() = PADDING_DP * scale

    private val bandWidth: Float get() = BAND_WIDTH_DP * scale

    private val rowHeight: Float get() = ROW_HEIGHT_DP * scale

    private val labelGap: Float get() = GAP_DP * scale

    private val dataTimeGap: Float get() = DATA_TIME_GAP_DP * scale

    private val cornerRadius: Float get() = CORNER_RADIUS_DP * scale

    /**
     * The continuous strike-age band for the active palette and interval. The
     * colours run from newest (top) to oldest (bottom), and the labels are
     * `0 min`, half the interval, and the full interval.
     */
    fun band(): LegendBand? {
        val colorHandler = strikesOverlay.colorHandler
        val numberOfColors = colorHandler.numberOfColors
        val intervalMinutes = strikesOverlay.parameters.intervalDuration
        if (numberOfColors <= 0 || intervalMinutes <= 0) {
            return null
        }
        return LegendBand(
            colors = colorHandler.colors.toList(),
            labels =
                listOf(
                    ageNowLabel,
                    ageMinutesFormat.format(intervalMinutes / 2),
                    ageMinutesFormat.format(intervalMinutes),
                ),
        )
    }

    /** The panel rectangle in surface pixels, bottom-left of the host's map area. */
    internal fun panelBounds(): RectF = panelBounds(band())

    private fun panelBounds(band: LegendBand?): RectF {
        val legendWidth =
            band?.let {
                bandWidth + labelGap + (it.labels.maxOfOrNull { label -> textPaint.measureText(label) } ?: 0f)
            } ?: 0f
        val dataTimeWidth = if (dataTime.isBlank()) 0f else textPaint.measureText(dataTime)
        val width = innerPadding + maxOf(legendWidth, dataTimeWidth) + innerPadding
        val bandContentHeight =
            band?.let { labelOverhangPx + bandHeight(it.colors.size) + labelOverhangPx } ?: 0f
        val height = innerPadding + dataTimeHeight + bandContentHeight + innerPadding
        val left = panelInset
        val top = anchorBottom - panelInset - height
        return RectF(left, top, left + width, top + height)
    }

    /** The colour band rectangle in surface pixels, or `null` while no palette is known. */
    internal fun bandBounds(): RectF? {
        val band = band() ?: return null
        return bandBounds(band, panelBounds(band))
    }

    private fun bandBounds(
        band: LegendBand,
        panel: RectF,
    ): RectF {
        val top = panel.top + innerPadding + dataTimeHeight + labelOverhangPx
        val left = panel.left + innerPadding
        return RectF(left, top, left + bandWidth, top + bandHeight(band.colors.size))
    }

    private fun bandHeight(numberOfColors: Int): Float = numberOfColors * rowHeight

    /** Height of the status line, including the gap to the legend. */
    private val dataTimeHeight: Float
        get() = if (dataTime.isBlank()) 0f else textPaint.fontSpacing + dataTimeGap

    override fun draw(
        canvas: Canvas?,
        mapView: MapView?,
        shadow: Boolean,
    ) {
        val targetCanvas = canvas ?: return
        val band = band()
        if (shadow || (band == null && dataTime.isBlank())) {
            return
        }

        mapView?.let { surfaceHeight = it.height }
        val panel = panelBounds(band)
        targetCanvas.drawRoundRect(panel, cornerRadius, cornerRadius, backgroundPaint)

        val contentTop = panel.top + innerPadding
        if (dataTime.isNotBlank()) {
            val dataTimeBaseline = contentTop - textPaint.fontMetrics.ascent
            targetCanvas.drawText(dataTime, panel.left + innerPadding, dataTimeBaseline, textPaint)
        }

        if (band != null) {
            drawBand(targetCanvas, band, bandBounds(band, panel))
        }
    }

    private fun drawBand(
        canvas: Canvas,
        band: LegendBand,
        bounds: RectF,
    ) {
        bandPaint.shader = gradientFor(band, bounds)
        canvas.drawRect(bounds, bandPaint)
        bandPaint.shader = null

        band.labels.forEachIndexed { index, label ->
            val centerY = bounds.top + bounds.height() * LABEL_FRACTIONS[index]
            val textBaseline = centerY - (textPaint.descent() + textPaint.ascent()) / 2f
            canvas.drawText(label, bounds.right + labelGap, textBaseline, textPaint)
        }
    }

    /** The band gradient, reusing the cached shader while its inputs are unchanged. */
    private fun gradientFor(
        band: LegendBand,
        bounds: RectF,
    ): LinearGradient {
        val cached = gradient
        if (cached != null &&
            gradientColors == band.colors &&
            gradientBounds == bounds &&
            gradientScale == scale
        ) {
            return cached
        }

        val created =
            LinearGradient(
                bounds.left,
                bounds.top,
                bounds.left,
                bounds.bottom,
                band.colors.toIntArray(),
                null,
                Shader.TileMode.CLAMP,
            )
        gradient = created
        gradientColors = band.colors
        gradientBounds = RectF(bounds)
        gradientScale = scale
        gradientBuilds++
        return created
    }

    private companion object {
        /** Baseline dpi the metric constants are expressed in. */
        const val BASE_DENSITY_DPI = 160f

        /** Opacity of the dark panel behind the legend. */
        const val PANEL_ALPHA = 200

        const val PANEL_INSET_DP = 8f
        const val PADDING_DP = 8f

        /** Band width; 30% narrower than the original 16 dp swatch. */
        const val BAND_WIDTH_DP = 11.2f
        const val ROW_HEIGHT_DP = 24f
        const val GAP_DP = 8f
        const val CORNER_RADIUS_DP = 10f

        /** Vertical positions of the three age labels: top, middle, bottom. */
        val LABEL_FRACTIONS = floatArrayOf(0f, 0.5f, 1f)

        /** Gap between the status line and the legend. */
        const val DATA_TIME_GAP_DP = 4f
    }
}
