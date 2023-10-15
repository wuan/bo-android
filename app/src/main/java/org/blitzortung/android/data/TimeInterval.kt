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

import java.io.Serializable

data class TimeInterval(
    val offset: Int = 0,
    val duration: Int = DEFAULT_DURATION,
) : Serializable {

    fun withOffset(offset: Int, history: History): TimeInterval {
        if (offset in history.lowerLimit(this)..0) {
            return copy(offset = offset)
        }
        return this
    }

    fun isRealtime(): Boolean = offset == 0

    fun rewInterval(history: History): TimeInterval {
        return updateIntervalOffset(history, Int::minus)
    }

    fun ffwdInterval(history: History): TimeInterval {
        return updateIntervalOffset(history, Int::plus)
    }

    fun animationStep(history: History): TimeInterval {
        return if (offset == 0) {
            this.copy(offset = history.lowerLimit(this))
        } else {
            updateIntervalOffset(history, Int::plus)
        }
    }

    fun goRealtime(): TimeInterval {
        return copy(offset = 0)
    }

    private fun updateIntervalOffset(history: History, operation: (Int, Int) -> Int): TimeInterval {
        val intervalOffset = operation.invoke(offset, history.timeIncrement)
        return this.copy(
            offset = alignValue(
                intervalOffset.coerceIn(history.lowerLimit(this), 0),
                history.timeIncrement
            )
        )
    }

    private fun alignValue(value: Int, offsetIncrement: Int): Int {
        return (value / offsetIncrement) * offsetIncrement
    }

    companion object {
        const val DEFAULT_DURATION = 60
        val BACKGROUND = TimeInterval(duration = 10, offset = 0)
    }
}
