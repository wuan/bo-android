package org.blitzortung.android.alert.handler

import android.location.Location
import org.blitzortung.android.alert.AlertParameters
import org.blitzortung.android.alert.AlertResult
import org.blitzortung.android.alert.data.AlertSector
import org.blitzortung.android.data.beans.Strike

class AlertDataHandler(
        private val alertStatusHandler: AlertStatusHandler = AlertStatusHandler()
) {
    fun checkStrikes(strikes: Collection<Strike>, location: Location, parameters: AlertParameters,
                     referenceTime: Long = System.currentTimeMillis()): AlertResult {
        return alertStatusHandler.checkStrikes(strikes, location, parameters, referenceTime)
    }

    fun getLatestTimstampWithin(distanceLimit: Float, alertResult: AlertResult): Long {
        return alertResult.sectors.fold(0L, {
            latestTimestamp, sector ->
            Math.max(latestTimestamp, getLatestTimestampWithin(distanceLimit, sector))
        })
    }

    internal fun getLatestTimestampWithin(distanceLimit: Float, sector: AlertSector): Long {
        return sector.ranges
                .filter { distanceLimit <= it.rangeMaximum }
                .map { it.latestStrikeTimestamp }
                .max() ?: 0L
    }

    fun getTextMessage(alertResult: AlertResult, notificationDistanceLimit: Float): String {
        return alertResult.sectorsByDistance
                .filter { it.key <= notificationDistanceLimit }
                .map {
                    "%s %.0f%s".format(it.value.label, it.key, alertResult.parameters.measurementSystem.unitName)
                }.joinToString()
    }
}