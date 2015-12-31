package org.blitzortung.android.alert.handler

import org.blitzortung.android.alert.data.AlertSector

internal class AggregatingAlertSector(
        val label: String,
        val minimumSectorBearing: Float,
        val maximumSectorBearing: Float,
        val ranges: List<AggregatingAlertSectorRange>
) {

    var closestStrikeDistance: Float = Float.POSITIVE_INFINITY
        private set

    fun updateClosestStrikeDistance(distance: Float) {
        closestStrikeDistance = Math.min(distance, closestStrikeDistance)
    }

    fun toAlertSector(): AlertSector {
        return AlertSector(
                label = label,
                minimumSectorBearing = minimumSectorBearing,
                maximumSectorBearing = maximumSectorBearing,
                closestStrikeDistance = closestStrikeDistance,
                ranges = ranges.map { it.toAlertSectorRange() }
        )
    }
}
