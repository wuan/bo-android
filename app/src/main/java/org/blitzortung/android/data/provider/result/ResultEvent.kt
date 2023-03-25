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

package org.blitzortung.android.data.provider.result

import org.blitzortung.android.data.Flags
import org.blitzortung.android.data.Parameters
import org.blitzortung.android.data.beans.RasterParameters
import org.blitzortung.android.data.beans.Station
import org.blitzortung.android.data.beans.Strike

data class ResultEvent(
    val strikes: List<Strike>? = null,
    val stations: List<Station>? = null,
    val rasterParameters: RasterParameters? = null,
    val histogram: IntArray? = null,
    val failed: Boolean = false,
    val updated: Int = -1,
    val referenceTime: Long = 0,
    val parameters: Parameters,
    val flags: Flags
) : DataEvent {

    fun containsRealtimeData(): Boolean {
        return parameters.isRealtime()
    }

    override fun toString(): String {
        val sb = StringBuilder()
        if (failed) {
            sb.append("FailedResult()")
        } else {
            sb.append("Result(")
            sb.append(strikes?.size ?: 0).append(" strikes, ")
            sb.append(parameters)
            if (rasterParameters != null) {
                sb.append(", ").append(rasterParameters)
            }
            sb.append(", updated=$updated")
            sb.append(", referenceTime=$referenceTime")
            sb.append(")")
        }

        return sb.toString()
    }
}
