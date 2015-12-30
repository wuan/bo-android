package org.blitzortung.android.alert

import android.content.res.Resources

import org.blitzortung.android.app.R

class AlertLabelHandler(
        private val alertLabel: AlertLabel,
        private val resources: Resources
) {
    fun apply(result: AlertResult?) {
        var warningText = ""

        var textColorResource = R.color.Green

        if (result != null && result.closestStrikeDistance < Float.POSITIVE_INFINITY) {
            if (result.closestStrikeDistance > 50) {
                textColorResource = R.color.Green
            } else if (result.closestStrikeDistance > 20) {
                textColorResource = R.color.Yellow
            } else {
                textColorResource = R.color.Red
            }
            warningText = "%.0f%s %s".format(
                    result.closestStrikeDistance,
                    result.parameters.measurementSystem.unitName,
                    result.bearingName)
        }

        val color = resources.getColor(textColorResource)
        alertLabel.setAlarmTextColor(color)
        alertLabel.setAlarmText(warningText)
    }
}
