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

package org.blitzortung.android.alert

import android.content.res.Resources
import org.blitzortung.android.app.R

class AlertLabelHandler(
    private val alertLabel: AlertLabel,
    private val resources: Resources,
) {
    fun apply(result: AlertResult?) {
        var warningText = ""

        var textColorResource = R.color.Green

        if (result != null && result.closestStrikeDistance < Float.POSITIVE_INFINITY) {
            textColorResource =
                when (result.closestStrikeDistance) {
                    in 0.0..20.0 -> R.color.RedWarn
                    in 20.0..50.0 -> R.color.Yellow
                    else -> R.color.Green
                }
            val distanceUnit = resources.getString(result.parameters.measurementSystem.unitNameString)
            warningText = "%.0f$distanceUnit".format(result.closestStrikeDistance)
            if (result.closestStrikeDistance > 0.1) {
                warningText += " ${result.bearingName}"
            }
        }

        val color = resources.getColor(textColorResource)
        alertLabel.setAlarmTextColor(color)
        alertLabel.setAlarmText(warningText)
    }
}
