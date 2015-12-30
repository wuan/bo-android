package org.blitzortung.android.alert.data

data class AlertSector(
        val label: String,
        val minimumSectorBearing: Float,
        val maximumSectorBearing: Float,
        val ranges: List<AlertSectorRange>,
        val closestStrikeDistance: Float
) {}