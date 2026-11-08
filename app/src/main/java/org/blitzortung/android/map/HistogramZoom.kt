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

import kotlin.math.abs
import org.blitzortung.android.data.Parameters
import org.blitzortung.android.data.beans.GridParameters
import org.blitzortung.android.data.provider.LOCAL_REGION

/** The camera target selected by a histogram tap. */
data class HistogramZoomTarget(
    val longitude: Double,
    val latitude: Double,
    val spanMeters: Float,
)

/**
 * Chooses what a histogram tap frames.
 *
 * A non-global result first frames its data area. Once the map is already at
 * that framing, the next tap zooms out to the global view, so the action acts
 * as a two-step zoom-out instead of repeatedly re-centring the same data area.
 */
object HistogramZoom {
    const val LOCAL_DATA_AREA_SPAN_METERS = 1_800_000f
    const val REGION_DATA_AREA_SPAN_METERS = 5_000_000f
    const val GLOBAL_SPAN_METERS = 40_000_000f
    const val GLOBAL_LONGITUDE = 0.0
    const val GLOBAL_LATITUDE = -30.0

    private const val DATA_AREA_ZOOM_TOLERANCE = 1.0

    fun target(
        parameters: Parameters,
        gridParameters: GridParameters?,
        currentZoom: Double,
        viewportPixels: Int,
    ): HistogramZoomTarget {
        if (parameters.isGlobal || gridParameters == null || gridParameters.isGlobal) {
            return globalTarget()
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
            globalTarget()
        } else {
            HistogramZoomTarget(
                longitude = gridParameters.rectCenterLongitude,
                latitude = gridParameters.rectCenterLatitude,
                spanMeters = spanMeters,
            )
        }
    }

    private fun globalTarget() =
        HistogramZoomTarget(
            longitude = GLOBAL_LONGITUDE,
            latitude = GLOBAL_LATITUDE,
            spanMeters = GLOBAL_SPAN_METERS,
        )
}
