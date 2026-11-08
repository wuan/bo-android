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

import android.location.Location
import kotlin.math.abs
import org.blitzortung.android.data.Parameters
import org.blitzortung.android.data.beans.GridParameters
import org.blitzortung.android.data.provider.LOCAL_REGION

/** The camera target selected by a histogram tap. */
data class HistogramZoomTarget(
    val longitude: Double,
    val latitude: Double,
    val zoomLevel: Double,
)

/**
 * Chooses what a histogram tap frames.
 *
 * A non-global result first frames its data area. Once the map is already at
 * that framing, the next tap zooms out to the global view, so the action acts
 * as a two-step zoom-out instead of repeatedly re-centring the same data area.
 * The global view is centred on the current position's longitude when one is
 * known, so the driver's own area stays in view. Its latitude follows the
 * current position only inside a corridor, so the fixed zoom never shows past
 * a pole.
 */
object HistogramZoom {
    const val LOCAL_DATA_AREA_SPAN_METERS = 1_800_000f
    const val REGION_DATA_AREA_SPAN_METERS = 5_000_000f
    const val GLOBAL_ZOOM_LEVEL = 2.5
    const val GLOBAL_LONGITUDE = 0.0
    const val GLOBAL_LATITUDE = 0.0

    /**
     * Latitude corridor for the global view. With tiles scaled to the display
     * density, the world at [GLOBAL_ZOOM_LEVEL] is about 3,800-5,100 px high on
     * a phone, so the viewport covers roughly 60% of it. The viewport only
     * reaches the pole edge beyond a centre latitude of about ±55°, so capping
     * the centre at ±30° keeps a safe margin against the map running over the
     * north or south pole.
     */
    const val GLOBAL_LATITUDE_CORRIDOR = 30.0

    private const val DATA_AREA_ZOOM_TOLERANCE = 1.0

    fun target(
        parameters: Parameters,
        gridParameters: GridParameters?,
        currentZoom: Double,
        viewportPixels: Int,
        currentPosition: Location?,
    ): HistogramZoomTarget {
        if (parameters.isGlobal || gridParameters == null || gridParameters.isGlobal) {
            return globalTarget(currentPosition)
        }

        val spanMeters =
            if (parameters.region == LOCAL_REGION) {
                LOCAL_DATA_AREA_SPAN_METERS
            } else {
                REGION_DATA_AREA_SPAN_METERS
            }
        val dataAreaZoom =
            MapZoom.zoomForSpan(
                spanMeters = spanMeters.toDouble(),
                viewportPixels = viewportPixels.coerceAtLeast(1),
                latitude = gridParameters.rectCenterLatitude,
            )

        return if (abs(currentZoom - dataAreaZoom) < DATA_AREA_ZOOM_TOLERANCE) {
            globalTarget(currentPosition)
        } else {
            HistogramZoomTarget(
                longitude = gridParameters.rectCenterLongitude,
                latitude = gridParameters.rectCenterLatitude,
                zoomLevel = dataAreaZoom,
            )
        }
    }

    private fun globalTarget(currentPosition: Location?) =
        HistogramZoomTarget(
            longitude = currentPosition?.longitude ?: GLOBAL_LONGITUDE,
            latitude =
                (currentPosition?.latitude ?: GLOBAL_LATITUDE).coerceIn(
                    -GLOBAL_LATITUDE_CORRIDOR,
                    GLOBAL_LATITUDE_CORRIDOR,
                ),
            zoomLevel = GLOBAL_ZOOM_LEVEL,
        )
}
