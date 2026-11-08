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

package org.blitzortung.android.alert

/**
 * Shared own-location framing target for the phone and the car.
 *
 * The target is the outermost alert range step, reduced to twice the closest
 * known strike distance when alert activity reports one, bounded by a 50-unit
 * minimum and the outermost step. The doubled distance keeps some surrounding
 * activity visible in both portrait and landscape, because the map fit uses the
 * shorter viewport side. Distances are expressed in the active measurement
 * system and converted to metres for the map fit.
 */
object AlertZoomTarget {
    private const val CLOSEST_STRIKE_MARGIN = 2.0f
    private const val MINIMUM_DISTANCE_UNITS = 50f

    /**
     * Returns the ground span, in metres, that an own-location view should frame.
     */
    fun framingMeters(
        warning: Warning,
        parameters: AlertParameters,
    ): Float {
        val outerRangeStep = parameters.rangeSteps.last()
        val targetUnits =
            if (warning is LocalActivity) {
                (warning.closestStrikeDistance * CLOSEST_STRIKE_MARGIN)
                    .coerceIn(MINIMUM_DISTANCE_UNITS, outerRangeStep)
            } else {
                outerRangeStep
            }
        return parameters.measurementSystem.toMeters(targetUnits)
    }
}
