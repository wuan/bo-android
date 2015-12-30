package org.blitzortung.android.alert.handler

import org.blitzortung.android.alert.data.AlertSectorRange
import org.blitzortung.android.data.beans.Strike

internal class ProcessingAlertSectorRange(
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

    fun toAlertSectorRange(): AlertSectorRange {
        return AlertSectorRange(
                rangeMinimum = rangeMinimum,
                rangeMaximum = rangeMaximum,
                latestStrikeTimestamp = latestStrikeTimestamp,
                strikeCount = strikeCount
        )
    }
}
