package org.blitzortung.android.alert.handler

import org.blitzortung.android.alert.data.AlertSector
import org.blitzortung.android.alert.data.AlertSectorRange

internal class AggregatingAlertDataMapper {
    fun mapSector(aggregatingAlertSector: AggregatingAlertSector): AlertSector {
        with (aggregatingAlertSector) {
            return AlertSector(
                    label = label,
                    minimumSectorBearing = minimumSectorBearing,
                    maximumSectorBearing = maximumSectorBearing,
                    closestStrikeDistance = closestStrikeDistance,
                    ranges = ranges.map { mapSectorRange(it) }
            )
        }
    }

    fun mapSectorRange(aggregatingAlertSectorRange: AggregatingAlertSectorRange): AlertSectorRange {
        with (aggregatingAlertSectorRange) {
            return AlertSectorRange(
                    rangeMinimum = rangeMinimum,
                    rangeMaximum = rangeMaximum,
                    latestStrikeTimestamp = latestStrikeTimestamp,
                    strikeCount = strikeCount
            )
        }
    }
}