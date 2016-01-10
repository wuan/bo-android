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
