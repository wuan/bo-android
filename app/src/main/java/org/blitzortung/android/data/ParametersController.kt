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

import android.util.Log
import org.blitzortung.android.app.Main

class ParametersController private constructor(private val offsetIncrement: Int) {

    fun rewInterval(parameters: Parameters): Parameters {
        return updateInterval(parameters, -offsetIncrement)
    }

    fun ffwdInterval(parameters: Parameters): Parameters {
        return updateInterval(parameters, offsetIncrement)
    }

    private fun updateInterval(parameters: Parameters, offsetIncrement: Int): Parameters {
        var intervalOffset = parameters.intervalOffset + offsetIncrement
        val intervalDuration = parameters.intervalDuration

        if (intervalOffset < -MAX_HISTORY_RANGE + intervalDuration) {
            intervalOffset = -MAX_HISTORY_RANGE + intervalDuration
        } else if (intervalOffset > 0) {
            intervalOffset = 0
        }

        return parameters.copy(intervalOffset = alignValue(intervalOffset))
    }

    fun goRealtime(parameters: Parameters): Parameters {
        return parameters.copy(intervalOffset = 0)
    }

    private fun alignValue(value: Int): Int {
        return (value / offsetIncrement) * offsetIncrement
    }

    fun setOffset(parameters: Parameters, offset: Int): Parameters {
        Log.v(Main.LOG_TAG, "ParametersController.setOffset(${offset})")
        if (offset in -MAX_HISTORY_RANGE + parameters.intervalDuration..0) {
            return parameters.copy(intervalOffset = offset)
        }
        return parameters
    }

    companion object {

        const val MAX_HISTORY_RANGE = 24 * 60

        fun withOffsetIncrement(offsetIncrement: Int): ParametersController {
            return ParametersController(offsetIncrement)
        }
    }

}
