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
 * the local tile coordinates used for `get_local_clusters`. By default [intervalCount]
 * is derived from [minuteLength] so a single request covers the last hour; when
 * [latestOnly] is set only the most recent interval is requested.
 */
data class ClusterParameters(
    val global: Boolean = true,
    val dataArea: DataArea? = null,
    val minuteLength: Int = DEFAULT_MINUTE_LENGTH,
    val minuteOffset: Int = 0,
    val latestOnly: Boolean = false,
) : Serializable {
    val intervalCount: Int
        get() = if (latestOnly) 1 else intervalCountFor(minuteLength)

    fun withDataArea(dataArea: DataArea?): ClusterParameters = copy(global = false, dataArea = dataArea)

    fun withMinuteOffset(minuteOffset: Int): ClusterParameters = copy(minuteOffset = minuteOffset)

    fun withLatestOnly(latestOnly: Boolean): ClusterParameters = copy(latestOnly = latestOnly)

    companion object {
        const val DEFAULT_MINUTE_LENGTH = 10
        const val DEFAULT_MINUTE_OFFSET = 0
        const val CLUSTER_RANGE_MINUTES = 60

        fun intervalCountFor(minuteLength: Int): Int = if (minuteLength > 0) CLUSTER_RANGE_MINUTES / minuteLength else 1
    }
}
