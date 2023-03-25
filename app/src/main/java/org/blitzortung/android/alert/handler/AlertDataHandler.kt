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

import android.content.res.Resources
import android.location.Location
import org.blitzortung.android.alert.AlertParameters
import org.blitzortung.android.alert.AlertResult
import org.blitzortung.android.alert.data.AlertSector
import org.blitzortung.android.data.beans.RasterElement
import org.blitzortung.android.data.beans.RasterParameters
import org.blitzortung.android.data.beans.Strike
import org.blitzortung.android.util.MeasurementSystem
import javax.inject.Inject
import kotlin.math.max

open class AlertDataHandler @Inject internal constructor(
    private val aggregatingAlertDataMapper: AggregatingAlertDataMapper
) {

    private val strikeLocation: Location = Location("")

    fun checkStrikes(
        strikes: Strikes, location: Location, parameters: AlertParameters,
        referenceTime: Long = System.currentTimeMillis()
    ): AlertResult {
        val sectors = createSectors(parameters)

        val thresholdTime = referenceTime - parameters.alarmInterval

        val strikeLocation = Location("")

        strikes.strikes.forEach { strike ->
            val bearingToStrike = calculateBearingToStrike(location, strikeLocation, strike)

            val alertSector = getRelevantSector(bearingToStrike.toDouble(), sectors)
            alertSector?.let {
                checkStrike(
                    alertSector,
                    strike,
                    strikes.rasterParameters,
                    parameters.measurementSystem,
                    location,
                    thresholdTime
                )
            }
        }

        return AlertResult(sectors.map { aggregatingAlertDataMapper.mapSector(it) }, parameters, referenceTime)
    }

    private fun checkStrike(
        sector: AggregatingAlertSector,
        strike: Strike,
        rasterParameters: RasterParameters?,
        measurementSystem: MeasurementSystem,
        location: Location,
        thresholdTime: Long
    ) {
        val distance = calculateDistanceTo(location, strike, rasterParameters, measurementSystem)

        sector.ranges.find { distance <= it.rangeMaximum }?.let {
            it.addStrike(strike)
            if (strike.timestamp >= thresholdTime) {
                sector.updateClosestStrikeDistance(distance)
            }
        }
    }

    private fun calculateDistanceTo(
        location: Location,
        strike: Strike,
        rasterParameters: RasterParameters?,
        measurementSystem: MeasurementSystem
    ): Float {
        if (strike is RasterElement && rasterParameters != null) {
            strikeLocation.longitude =
                closestValue(location.longitude, strike.longitude, rasterParameters.longitudeDelta)
            strikeLocation.latitude = closestValue(location.latitude, strike.latitude, rasterParameters.latitudeDelta)
        } else {
            strikeLocation.longitude = strike.longitude
            strikeLocation.latitude = strike.latitude
        }
        val distanceInMeters = location.distanceTo(strikeLocation)
        return measurementSystem.calculateDistance(distanceInMeters)
    }

    private fun closestValue(location: Double, target: Double, rasterSize: Double): Double {
        val delta = rasterSize / 2.0
        if (location in target - delta..target + delta) {
            return location
        } else {
            if (location < target) {
                return target - delta
            } else {
                return target + delta
            }
        }

    }

    fun getLatestTimstampWithin(distanceLimit: Float, alertResult: AlertResult): Long {
        return alertResult.sectors.fold(0L, { latestTimestamp, sector ->
            max(latestTimestamp, getLatestTimestampWithin(distanceLimit, sector))
        })
    }

    fun getTextMessage(alertResult: AlertResult, notificationDistanceLimit: Float, resources: Resources): String {
        return alertResult.sectorsByDistance
            .filter { it.key <= notificationDistanceLimit }
            .map {
                "%s %.0f%s".format(
                    it.value.label,
                    it.key,
                    resources.getString(alertResult.parameters.measurementSystem.unitNameString)
                )
            }.joinToString()
    }

    private fun getLatestTimestampWithin(distanceLimit: Float, sector: AlertSector): Long {
        return sector.ranges
            .filter { distanceLimit <= it.rangeMaximum }
            .map { it.latestStrikeTimestamp }
            .maxOrNull() ?: 0L
    }

    private fun createSectors(alertParameters: AlertParameters): List<AggregatingAlertSector> {
        val sectorLabels = alertParameters.sectorLabels
        val sectorWidth = 360f / sectorLabels.size

        var bearing = -180f
        return sectorLabels.map { sectorLabel ->
            var minimumSectorBearing = bearing - sectorWidth / 2.0f
            minimumSectorBearing += (if (minimumSectorBearing < -180f) 360f else 0f)
            val maximumSectorBearing = bearing + sectorWidth / 2.0f
            val alertSector = AggregatingAlertSector(
                sectorLabel,
                minimumSectorBearing,
                maximumSectorBearing,
                createRanges(alertParameters)
            )
            bearing += sectorWidth
            alertSector
        }
    }

    private fun createRanges(alertParameters: AlertParameters): List<AggregatingAlertSectorRange> {
        val rangeSteps = alertParameters.rangeSteps

        var rangeMinimum = 0.0f
        return rangeSteps.map { rangeMaximum ->
            val alertSectorRange = AggregatingAlertSectorRange(rangeMinimum, rangeMaximum)
            rangeMinimum = rangeMaximum
            alertSectorRange
        }
    }

    private fun calculateBearingToStrike(location: Location, strikeLocation: Location, strike: Strike): Float {
        strikeLocation.longitude = strike.longitude
        strikeLocation.latitude = strike.latitude
        return location.bearingTo(strikeLocation)
    }

    private fun getRelevantSector(
        bearing: Double,
        sectors: Collection<AggregatingAlertSector>
    ): AggregatingAlertSector? {
        return sectors.firstOrNull { sectorContainsBearing(it, bearing) }
    }

    private fun sectorContainsBearing(sector: AggregatingAlertSector, bearing: Double): Boolean {
        val minimumSectorBearing = sector.minimumSectorBearing
        val maximumSectorBearing = sector.maximumSectorBearing

        return when {
            maximumSectorBearing > minimumSectorBearing ->
                bearing < maximumSectorBearing && bearing >= minimumSectorBearing
            maximumSectorBearing < minimumSectorBearing ->
                bearing >= minimumSectorBearing || bearing < maximumSectorBearing
            else -> true
        }
    }
}