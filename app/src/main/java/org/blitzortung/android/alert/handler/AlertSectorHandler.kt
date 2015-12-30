package org.blitzortung.android.alert.handler


import android.location.Location
import org.blitzortung.android.alert.handler.ProcessingAlertSector
import org.blitzortung.android.data.beans.Strike
import org.blitzortung.android.util.MeasurementSystem

class AlertSectorHandler {

    private val strikeLocation: Location = Location("")

    private var location: Location? = null

    private var thresholdTime: Long = 0

    fun setCheckStrikeParameters(location: Location, thresholdTime: Long) {
        this.location = location
        this.thresholdTime = thresholdTime
    }

    internal fun checkStrike(sector: ProcessingAlertSector, strike: Strike, measurementSystem: MeasurementSystem) {
        location?.let { location ->
            val distance = calculateDistanceTo(location, strike, measurementSystem)

            if (strike.timestamp >= thresholdTime) {
                sector.ranges.find { r -> distance <= r.rangeMaximum }?.let {
                    it.addStrike(strike);
                    sector.updateClosestStrikeDistance(distance)
                }
            }
        }
    }

    private fun calculateDistanceTo(location: Location, strike: Strike, measurementSystem: MeasurementSystem): Float {
        strikeLocation.longitude = strike.longitude.toDouble()
        strikeLocation.latitude = strike.latitude.toDouble()
        val distanceInMeters = location.distanceTo(strikeLocation)
        return measurementSystem.calculateDistance(distanceInMeters)
    }

}