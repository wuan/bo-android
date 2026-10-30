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

package org.blitzortung.android.data

import java.io.Serializable

/**
 * Parameters for a cluster request, independent of the strike-centric [Parameters].
 *
 * [global] selects the `get_global_clusters` endpoint; when `false` [dataArea] carries
 * the local tile coordinates used for `get_local_clusters`. The cluster bin length is
 * fixed at [MINUTE_LENGTH] (the backend produces 10-minute cluster intervals), so the
 * requested window is expressed as [intervalCount] consecutive bins, resolved by the
 * caller from the selected time interval.
 */
data class ClusterParameters(
    val global: Boolean = true,
    val dataArea: DataArea? = null,
    val minuteLength: Int = MINUTE_LENGTH,
    val minuteOffset: Int = DEFAULT_MINUTE_OFFSET,
    val intervalCount: Int = 1,
) : Serializable {
    fun withDataArea(dataArea: DataArea?): ClusterParameters = copy(global = false, dataArea = dataArea)

    fun withMinuteOffset(minuteOffset: Int): ClusterParameters = copy(minuteOffset = minuteOffset)

    fun withIntervalCount(intervalCount: Int): ClusterParameters = copy(intervalCount = intervalCount)

    companion object {
        const val MINUTE_LENGTH = 10
        const val DEFAULT_MINUTE_OFFSET = 0

        fun intervalCountFor(timeIntervalMinutes: Int): Int =
            if (timeIntervalMinutes > 0) timeIntervalMinutes / MINUTE_LENGTH else 1
    }
}
