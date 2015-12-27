package org.blitzortung.android.data.provider.result

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
        val incrementalData: Boolean = false,
        val referenceTime: Long = 0,
        val parameters: Parameters? = null
) : DataEvent {

    fun containsRealtimeData(): Boolean {
        return parameters != null && parameters.intervalOffset == 0
    }

    override fun toString(): String {
        val sb = StringBuilder()
        if (failed) {
            sb.append("FailedResult()")
        } else {
            sb.append("Result(")
            val currentStrikes = strikes
            sb.append(if (currentStrikes != null) currentStrikes.size else 0).append(" strikes, ")
            sb.append(parameters)
            if (rasterParameters != null) {
                sb.append(", ").append(rasterParameters)
            }
            sb.append(", incrementalData=$incrementalData")
            sb.append(")")
        }

        return sb.toString()
    }
}
