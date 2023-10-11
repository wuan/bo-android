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

data class TimeInterval(
    val offset: Int = 0,
    val duration: Int = DEFAULT_OFFSET,
    val range: Int = MAX_HISTORY_RANGE
) {

    fun withOffset(offset: Int): TimeInterval {
        if (offset in -MAX_HISTORY_RANGE + duration..0) {
            return copy(offset = offset)
        }
        return this
    }

    fun isRealtime(): Boolean = offset == 0

    fun rewInterval(timeIncrement: Int): TimeInterval {
        return updateIntervalOffset(-timeIncrement)
    }

    fun ffwdInterval(timeIncrement: Int): TimeInterval {
        return updateIntervalOffset(timeIncrement)
    }


    fun goRealtime(): TimeInterval {
        return copy(offset = 0)
    }

    private fun updateIntervalOffset(offsetIncrement: Int): TimeInterval {
        val intervalOffset = offset + offsetIncrement
        return this.copy(offset = alignValue(intervalOffset.coerceIn(-range + duration, 0), offsetIncrement))
    }

    private fun alignValue(value: Int, offsetIncrement: Int): Int {
        return (value / offsetIncrement) * offsetIncrement
    }

    companion object {
        const val DEFAULT_OFFSET = 60
        const val DEFAULT_OFFSET_INCREMENT = 30
        const val MAX_HISTORY_RANGE = 24 * 60
    }
}
