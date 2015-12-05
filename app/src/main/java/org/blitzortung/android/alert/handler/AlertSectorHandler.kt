package org.blitzortung.android.alert.handler


import android.location.Location
import org.blitzortung.android.alert.data.AlertContext
import org.blitzortung.android.alert.data.AlertSector
import org.blitzortung.android.data.beans.Strike
import org.blitzortung.android.util.MeasurementSystem

class AlertSectorHandler {

    private val strikeLocation: Location

    private var location: Location? = null

    private var thresholdTime: Long = 0

    init {
        strikeLocation = Location("")
    }

    fun setCheckStrikeParameters(location: Location, thresholdTime: Long) {
        this.location = location
        this.thresholdTime = thresholdTime
    }

    fun checkStrike(sector: AlertSector, strike: Strike, alertContext: AlertContext) {
        val distance = calculateDistanceTo(strike, alertContext.alertParameters.measurementSystem)

        sector.ranges.find { r -> distance <= r.rangeMaximum }?.let {
            it.addStrike(strike);

            if (strike.timestamp >= thresholdTime) {
                sector.updateClosestStrikeDistance(distance)
            }
        }
    }

    private fun calculateDistanceTo(strike: Strike, measurementSystem: MeasurementSystem): Float {
        strikeLocation.longitude = strike.longitude.toDouble()
        strikeLocation.latitude = strike.latitude.toDouble()
        val distanceInMeters = location!!.distanceTo(strikeLocation)
        return measurementSystem.calculateDistance(distanceInMeters)
    }

    fun getLatestTimestampWithin(distanceLimit: Float, sector: AlertSector): Long {
        return sector.ranges
                .filter { distanceLimit <= it.rangeMaximum }
                .map { it.latestStrikeTimestamp }
                .max() ?: 0L
    }
}