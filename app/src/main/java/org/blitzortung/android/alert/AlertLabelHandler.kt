package org.blitzortung.android.alert

import android.content.res.Resources

import org.blitzortung.android.app.R

class AlertLabelHandler(
        private val alertLabel: AlertLabel,
        private val resources: Resources
) {
    fun apply(alertResult: AlertResult?) {
        var warningText = ""

        var textColorResource = R.color.Green

        if (alertResult != null) {
            if (alertResult.closestStrikeDistance > 50) {
                textColorResource = R.color.Green
            } else if (alertResult.closestStrikeDistance > 20) {
                textColorResource = R.color.Yellow
            } else {
                textColorResource = R.color.Red
            }
            warningText = "%.0f%s %s".format(alertResult.closestStrikeDistance, alertResult.distanceUnitName, alertResult.bearingName)
        }

        val color = resources.getColor(textColorResource)
        alertLabel.setAlarmTextColor(color)
        alertLabel.setAlarmText(warningText)
    }
}
