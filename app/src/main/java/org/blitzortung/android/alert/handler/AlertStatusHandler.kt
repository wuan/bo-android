package org.blitzortung.android.alert.handler

import android.location.Location
import android.util.Log

import org.blitzortung.android.alert.AlertResult
import org.blitzortung.android.alert.data.AlertSector
import org.blitzortung.android.alert.data.AlertContext
import org.blitzortung.android.app.Main
import org.blitzortung.android.data.beans.Strike
import java.util.Locale
import java.util.SortedMap
import java.util.TreeMap

class AlertStatusHandler(private val alertSectorHandler: AlertSectorHandler) {

    fun checkStrikes(alertContext: AlertContext, strikes: Collection<Strike>, location: Location): AlertContext {

        val thresholdTime = System.currentTimeMillis() - alertContext.alertParameters.alarmInterval

        alertSectorHandler.setCheckStrikeParameters(location, thresholdTime)

        val strikeLocation = Location("")

        for (strike in strikes) {
            val bearingToStrike = calculateBearingToStrike(location, strikeLocation, strike)

            val alertSector = getRelevantSector(bearingToStrike.toDouble(), alertContext)
            alertSector?.let { alertSectorHandler.checkStrike(alertSector, strike, alertContext) }
        }
        return alertContext
    }

    private fun calculateBearingToStrike(location: Location, strikeLocation: Location, strike: Strike): Float {
        strikeLocation.longitude = strike.longitude.toDouble()
        strikeLocation.latitude = strike.latitude.toDouble()
        return location.bearingTo(strikeLocation)
    }


    fun getLatestTimstampWithin(distanceLimit: Float, alertContext: AlertContext): Long {
        return alertContext.sectors.fold(0L, {
            latestTimestamp, sector ->
            Math.max(latestTimestamp, alertSectorHandler.getLatestTimestampWithin(distanceLimit, sector))
        })
    }

    fun getSectorWithClosestStrike(alertContext: AlertContext): AlertSector? {
        return alertContext.sectors
                .filter { it.closestStrikeDistance < Float.POSITIVE_INFINITY }
                .sortedBy { it.closestStrikeDistance }
                .firstOrNull()
    }

    fun getCurrentActivity(alertContext: AlertContext?): AlertResult? {
        if (alertContext != null) {
            val sector = getSectorWithClosestStrike(alertContext)

            return if (sector != null) AlertResult(sector, alertContext.alertParameters.measurementSystem.unitName) else null
        }
        return null
    }

    fun getTextMessage(alertContext: AlertContext, notificationDistanceLimit: Float): String {
        val distanceSectors = getSectorsSortedByClosestStrikeDistance(alertContext, notificationDistanceLimit)

        val sb = StringBuilder()

        if (distanceSectors.size > 0) {
            for (sector in distanceSectors.values) {
                sb.append(sector.label)
                sb.append(" ")
                sb.append("%.0f%s".format(sector.closestStrikeDistance, alertContext.alertParameters.measurementSystem.unitName))
                sb.append(", ")
            }
            sb.setLength(sb.length - 2)
        }

        return sb.toString()
    }

    private fun getSectorsSortedByClosestStrikeDistance(alertContext: AlertContext, notificationDistanceLimit: Float): Map<Float, AlertSector> {
        return alertContext.sectors
                .filter { it.closestStrikeDistance <= notificationDistanceLimit }
                .sortedBy { it.closestStrikeDistance }
                .toMapBy { it.closestStrikeDistance }
    }

    private fun getRelevantSector(bearing: Double, alertContext: AlertContext): AlertSector? {
        return alertContext.sectors.firstOrNull {sectorContainsBearing(it, bearing) }
    }

    private fun sectorContainsBearing(sector: AlertSector, bearing: Double): Boolean {
        val minimumSectorBearing = sector.minimumSectorBearing
        val maximumSectorBearing = sector.maximumSectorBearing

        if (maximumSectorBearing > minimumSectorBearing) {
            return bearing < maximumSectorBearing && bearing >= minimumSectorBearing
        } else {
            return bearing >= minimumSectorBearing || bearing < maximumSectorBearing
        }
    }
}
