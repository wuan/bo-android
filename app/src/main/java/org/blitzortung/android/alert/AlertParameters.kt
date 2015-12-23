package org.blitzortung.android.alert

import org.blitzortung.android.util.MeasurementSystem

data class AlertParameters(
        val alarmInterval: Long,
        val rangeSteps: Array<Float>,
        val sectorLabels: Array<String>,
        val measurementSystem: MeasurementSystem
) {
}

