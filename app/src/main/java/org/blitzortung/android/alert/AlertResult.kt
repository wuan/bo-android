package org.blitzortung.android.alert

import org.blitzortung.android.alert.data.AlertSector

data class AlertResult(
        private val sector: AlertSector,
        val distanceUnitName: String) {

    val closestStrikeDistance: Float
        get() = sector.closestStrikeDistance

    val bearingName: String
        get() = sector.label

    override fun toString(): String {
        return "%s %.1f %s".format(bearingName, closestStrikeDistance, distanceUnitName)
    }
}
