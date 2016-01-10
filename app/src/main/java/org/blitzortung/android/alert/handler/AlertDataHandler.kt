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

package org.blitzortung.android.alert.handler

import android.location.Location
import org.blitzortung.android.alert.AlertParameters
import org.blitzortung.android.alert.AlertResult
import org.blitzortung.android.alert.data.AlertSector
import org.blitzortung.android.data.beans.Strike
import org.blitzortung.android.util.MeasurementSystem

class AlertDataHandler {

    private val strikeLocation: Location = Location("")

    fun checkStrikes(strikes: Collection<Strike>, location: Location, parameters: AlertParameters,
                     referenceTime: Long = System.currentTimeMillis()): AlertResult {
        val sectors = createSectors(parameters)

        val thresholdTime = referenceTime - parameters.alarmInterval

        val strikeLocation = Location("")

        strikes.forEach { strike ->
            val bearingToStrike = calculateBearingToStrike(location, strikeLocation, strike)

            val alertSector = getRelevantSector(bearingToStrike.toDouble(), sectors)
            alertSector?.let {
                checkStrike(alertSector, strike, parameters.measurementSystem, location, thresholdTime)
            }
        }

        return AlertResult(sectors.map { it.toAlertSector() }, parameters, referenceTime)
    }

    internal fun checkStrike(sector: AggregatingAlertSector, strike: Strike, measurementSystem: MeasurementSystem,
                             location: Location, thresholdTime: Long) {
        val distance = calculateDistanceTo(location, strike, measurementSystem)

        sector.ranges.find { r -> distance <= r.rangeMaximum }?.let {
            it.addStrike(strike);
            if (strike.timestamp >= thresholdTime) {
                sector.updateClosestStrikeDistance(distance)
            }
        }
    }

    private fun calculateDistanceTo(location: Location, strike: Strike, measurementSystem: MeasurementSystem): Float {
        strikeLocation.longitude = strike.longitude.toDouble()
        strikeLocation.latitude = strike.latitude.toDouble()
        val distanceInMeters = location.distanceTo(strikeLocation)
        return measurementSystem.calculateDistance(distanceInMeters)
    }

    fun getLatestTimstampWithin(distanceLimit: Float, alertResult: AlertResult): Long {
        return alertResult.sectors.fold(0L, {
            latestTimestamp, sector ->
            Math.max(latestTimestamp, getLatestTimestampWithin(distanceLimit, sector))
        })
    }

    fun getTextMessage(alertResult: AlertResult, notificationDistanceLimit: Float): String {
        return alertResult.sectorsByDistance
                .filter { it.key <= notificationDistanceLimit }
                .map {
                    "%s %.0f%s".format(it.value.label, it.key, alertResult.parameters.measurementSystem.unitName)
                }.joinToString()
    }

    internal fun getLatestTimestampWithin(distanceLimit: Float, sector: AlertSector): Long {
        return sector.ranges
                .filter { distanceLimit <= it.rangeMaximum }
                .map { it.latestStrikeTimestamp }
                .max() ?: 0L
    }

    private fun createSectors(alertParameters: AlertParameters): List<AggregatingAlertSector> {
        val sectorLabels = alertParameters.sectorLabels
        val sectorWidth = 360f / sectorLabels.size

        val sectors: MutableList<AggregatingAlertSector> = arrayListOf()

        var bearing = -180f
        for (sectorLabel in sectorLabels) {
            var minimumSectorBearing = bearing - sectorWidth / 2.0f
            minimumSectorBearing += (if (minimumSectorBearing < -180f) 360f else 0f)
            val maximumSectorBearing = bearing + sectorWidth / 2.0f
            val alertSector = AggregatingAlertSector(sectorLabel, minimumSectorBearing, maximumSectorBearing, createRanges(alertParameters))
            sectors.add(alertSector)
            bearing += sectorWidth
        }
        return sectors.toList()
    }

    private fun createRanges(alertParameters: AlertParameters): List<AggregatingAlertSectorRange> {
        val rangeSteps = alertParameters.rangeSteps

        val ranges: MutableList<AggregatingAlertSectorRange> = arrayListOf()
        var rangeMinimum = 0.0f
        for (rangeMaximum in rangeSteps) {
            val alertSectorRange = AggregatingAlertSectorRange(rangeMinimum, rangeMaximum)
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

    private fun getRelevantSector(bearing: Double, sectors: Collection<AggregatingAlertSector>): AggregatingAlertSector? {
        return sectors.firstOrNull { sectorContainsBearing(it, bearing) }
    }

    private fun sectorContainsBearing(sector: AggregatingAlertSector, bearing: Double): Boolean {
        val minimumSectorBearing = sector.minimumSectorBearing
        val maximumSectorBearing = sector.maximumSectorBearing

        if (maximumSectorBearing > minimumSectorBearing) {
            return bearing < maximumSectorBearing && bearing >= minimumSectorBearing
        } else if (maximumSectorBearing < minimumSectorBearing) {
            return bearing >= minimumSectorBearing || bearing < maximumSectorBearing
        } else {
            // maximumSectorBearing == minimumSectorBearing -> only one sector
            return true;
        }
    }
}