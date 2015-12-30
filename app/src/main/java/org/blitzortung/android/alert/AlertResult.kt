package org.blitzortung.android.alert

import org.blitzortung.android.alert.data.AlertSector

data class AlertResult(
        val sectors: List<AlertSector>,
        val parameters: AlertParameters,
        val referenceTime: Long
) {
    val sectorsByDistance: Map<Float, AlertSector> by lazy {
        sectors.filter { it.closestStrikeDistance < Float.POSITIVE_INFINITY }
                .sortedBy { it.closestStrikeDistance }
                .toMapBy { it.closestStrikeDistance }
    }

    val sectorWithClosestStrike: AlertSector? by lazy {
        sectorsByDistance.values.firstOrNull()
    }

    val closestStrikeDistance: Float
        get() = sectorWithClosestStrike?.closestStrikeDistance ?: Float.POSITIVE_INFINITY

    val bearingName: String
        get() = sectorWithClosestStrike?.label ?: "n/a"

    override fun toString(): String {
        return "%s %.1f %s".format(bearingName, closestStrikeDistance, parameters.measurementSystem.unitName)
    }
}
