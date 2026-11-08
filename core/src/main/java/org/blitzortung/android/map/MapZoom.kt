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

package org.blitzortung.android.map

import kotlin.math.cos
import kotlin.math.log2

/**
 * Exact Web-Mercator fit from a ground distance to a map zoom level.
 *
 * The result fits [spanMeters] across the shorter side of a viewport that is
 * [viewportPixels] wide at the target [latitude]. The Web-Mercator scale shrinks
 * with the cosine of the latitude, so the same ground distance needs a higher
 * zoom near the poles than at the equator. Fractional zooms are returned as-is.
 */
object MapZoom {
    /** Earth radius used by the Web-Mercator projection (WGS 84 semi-major axis). */
    const val EARTH_RADIUS_METERS = 6_378_137.0

    /** osmdroid's default tile size in pixels. */
    private const val TILE_SIZE_PIXELS = 256.0

    /** Latitude limit of the Web-Mercator projection, where the scale stays finite. */
    const val MAX_MERCATOR_LATITUDE = 85.051129

    /**
     * Returns the zoom that fits [spanMeters] across [viewportPixels] at [latitude].
     *
     * @param spanMeters ground distance to frame, in metres
     * @param viewportPixels viewport size on the shorter side, in pixels
     * @param latitude target latitude in degrees; clamped to the Mercator limit
     */
    fun zoomForSpan(
        spanMeters: Double,
        viewportPixels: Int,
        latitude: Double,
    ): Double {
        require(spanMeters > 0.0) { "spanMeters must be positive, was $spanMeters" }
        require(viewportPixels > 0) { "viewportPixels must be positive, was $viewportPixels" }

        val clampedLatitude = latitude.coerceIn(-MAX_MERCATOR_LATITUDE, MAX_MERCATOR_LATITUDE)
        val metresPerPixelAtZoomZero = 2 * Math.PI * EARTH_RADIUS_METERS / TILE_SIZE_PIXELS
        return log2(
            metresPerPixelAtZoomZero *
                cos(Math.toRadians(clampedLatitude)) *
                viewportPixels /
                spanMeters,
        )
    }
}
