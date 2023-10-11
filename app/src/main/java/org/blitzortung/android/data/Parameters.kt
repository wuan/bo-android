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

package org.blitzortung.android.data

import org.blitzortung.android.data.TimeInterval.Companion.DEFAULT_OFFSET_INCREMENT
import org.blitzortung.android.data.provider.GLOBAL_REGION
import org.blitzortung.android.data.provider.LOCAL_REGION

data class Parameters(
    val region: Int = -1,
    val rasterBaselength: Int = 0,
    val interval: TimeInterval = TimeInterval(),
    val timeIncrement: Int = DEFAULT_OFFSET_INCREMENT,
    val countThreshold: Int = 0,
    val localReference: LocalReference? = null
) {

    val intervalDuration: Int
        get() = interval.duration

    val intervalOffset: Int
        get() = interval.offset

    val isGlobal: Boolean = region == GLOBAL_REGION

    val isLocal: Boolean = region == LOCAL_REGION

    fun isRealtime(): Boolean = interval.isRealtime()

    fun rewInterval(): Parameters = copy(interval = interval.rewInterval(timeIncrement))

    fun ffwdInterval(): Parameters = copy(interval = interval.ffwdInterval(timeIncrement))

    fun withIntervalOffset(offset: Int): Parameters = copy(interval = interval.withOffset(offset))

    fun goRealtime(): Parameters = copy(interval = interval.goRealtime())

    fun withTimeIncrement(timeInrement: Int): Parameters {
        return copy(timeIncrement = timeInrement)
    }

    fun withIntervalDuration(intervalDuration: Int): Parameters {
        return copy(interval = interval.copy(duration=intervalDuration))
    }

    val position: Int
        get() = interval.position

}

data class LocalReference(
    val x: Int,
    val y: Int
)