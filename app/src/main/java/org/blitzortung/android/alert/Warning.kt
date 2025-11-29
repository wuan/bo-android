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

package org.blitzortung.android.alert

import org.blitzortung.android.alert.data.AlertSector

sealed interface Warning

object Outlying : Warning

object NoData : Warning

object NoLocation : Warning

data class LocalActivity(
    val sectors: List<AlertSector>,
    val parameters: AlertParameters,
    val referenceTime: Long,
) : Warning {

    val sectorsByDistance: Map<Float, AlertSector> by lazy {
        sectors
            .filter { it.closestStrikeDistance < Float.POSITIVE_INFINITY }
            .sortedBy { it.closestStrikeDistance }
            .associateBy { it.closestStrikeDistance }
    }

    val sectorWithClosestStrike: AlertSector? by lazy {
        sectorsByDistance.values.firstOrNull()
    }

    val closestStrikeDistance: Float
        get() = sectorWithClosestStrike?.closestStrikeDistance ?: Float.POSITIVE_INFINITY

    val bearingName: String
        get() = sectorWithClosestStrike?.label ?: "n/a"

    override fun toString(): String {
        return "%s %.1f %s".format(bearingName, closestStrikeDistance, parameters.measurementSystem)
    }
}
