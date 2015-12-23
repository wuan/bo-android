package org.blitzortung.android.alert.data

import org.blitzortung.android.data.beans.Strike

class AlertSectorRange(
        val rangeMinimum: Float,
        val rangeMaximum: Float
) {

    var strikeCount: Int = 0
        private set

    var latestStrikeTimestamp: Long = 0
        private set

    fun addStrike(strike: Strike) {
        if (strike.timestamp > this.latestStrikeTimestamp) {
            this.latestStrikeTimestamp = strike.timestamp
        }
        strikeCount += strike.multiplicity
    }
}
