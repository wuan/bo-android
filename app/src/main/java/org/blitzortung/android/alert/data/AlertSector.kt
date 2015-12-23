package org.blitzortung.android.alert.data

class AlertSector(
        val label: String,
        val minimumSectorBearing: Float,
        val maximumSectorBearing: Float,
        val ranges: List<AlertSectorRange>
) {

    var closestStrikeDistance: Float = Float.POSITIVE_INFINITY
        private set

    fun updateClosestStrikeDistance(distance: Float) {
        closestStrikeDistance = Math.min(distance, closestStrikeDistance)
    }
}
