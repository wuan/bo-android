package org.blitzortung.android.alert.handler

import android.location.Location
import android.util.Log
import org.blitzortung.android.alert.AlertParameters

import org.blitzortung.android.alert.AlertResult
import org.blitzortung.android.alert.handler.ProcessingAlertSector
import org.blitzortung.android.alert.data.AlertSector
import org.blitzortung.android.app.Main
import org.blitzortung.android.data.beans.Strike
import java.util.Locale
import java.util.SortedMap
import java.util.TreeMap

class AlertStatusHandler(private val alertSectorHandler: AlertSectorHandler = AlertSectorHandler()) {

    internal fun checkStrikes(strikes: Collection<Strike>, location: Location, alertParameters: AlertParameters, referenceTime: Long): AlertResult {

        val sectors = createSectors(alertParameters)

        val thresholdTime = referenceTime - alertParameters.alarmInterval

        alertSectorHandler.setCheckStrikeParameters(location, thresholdTime)

        val strikeLocation = Location("")

        strikes.forEach { strike ->
            val bearingToStrike = calculateBearingToStrike(location, strikeLocation, strike)

            val alertSector = getRelevantSector(bearingToStrike.toDouble(), sectors)
            alertSector?.let { alertSectorHandler.checkStrike(alertSector, strike, alertParameters.measurementSystem) }
        }

        return AlertResult(sectors.map { it.toAlertSector() }, alertParameters, referenceTime)
    }

    private fun createSectors(alertParameters: AlertParameters): List<ProcessingAlertSector> {
        val sectorLabels = alertParameters.sectorLabels
        val sectorWidth = 360f / sectorLabels.size

        val sectors: MutableList<ProcessingAlertSector> = arrayListOf()

        var bearing = -180f
        for (sectorLabel in sectorLabels) {
            var minimumSectorBearing = bearing - sectorWidth / 2.0f
            minimumSectorBearing += (if (minimumSectorBearing < -180f) 360f else 0f)
            val maximumSectorBearing = bearing + sectorWidth / 2.0f
            val alertSector = ProcessingAlertSector(sectorLabel, minimumSectorBearing, maximumSectorBearing, createRanges(alertParameters))
            sectors.add(alertSector)
            bearing += sectorWidth
        }
        return sectors.toList()
    }

    private fun createRanges(alertParameters: AlertParameters): List<ProcessingAlertSectorRange> {
        val rangeSteps = alertParameters.rangeSteps

        val ranges: MutableList<ProcessingAlertSectorRange> = arrayListOf()
        var rangeMinimum = 0.0f
        for (rangeMaximum in rangeSteps) {
            val alertSectorRange = ProcessingAlertSectorRange(rangeMinimum, rangeMaximum)
            ranges.add(alertSectorRange)
            rangeMinimum = rangeMaximum
        }
        return ranges.toList()
    }

    private fun calculateBearingToStrike(location: Location, strikeLocation: Location, strike: Strike): Float {
        strikeLocation.longitude = strike.longitude.toDouble()
        strikeLocation.latitude = strike.latitude.toDouble()
        return location.bearingTo(strikeLocation)
    }

    private fun getRelevantSector(bearing: Double, sectors: Collection<ProcessingAlertSector>): ProcessingAlertSector? {
        return sectors.firstOrNull { sectorContainsBearing(it, bearing) }
    }

    private fun sectorContainsBearing(sector: ProcessingAlertSector, bearing: Double): Boolean {
        val minimumSectorBearing = sector.minimumSectorBearing
        val maximumSectorBearing = sector.maximumSectorBearing

        if (maximumSectorBearing > minimumSectorBearing) {
            return bearing < maximumSectorBearing && bearing >= minimumSectorBearing
        } else if (maximumSectorBearing < minimumSectorBearing){
            return bearing >= minimumSectorBearing || bearing < maximumSectorBearing
        } else {
            // maximumSectorBearing == minimumSectorBearing -> only one sector
            return true;
        }
    }
}
