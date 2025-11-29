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

import android.content.Context
import org.blitzortung.android.app.R.color.Green
import org.blitzortung.android.app.R.color.RedWarn
import org.blitzortung.android.app.R.color.Yellow

class AlertLabelHandler(
    private val alertLabel: AlertLabel,
    private val context: Context,
) {
    fun apply(result: Warning) {

        val (warningText, textColorResource) = when (result) {
            is LocalActivity -> extractStatus(result)
            Outlying -> "<->" to RedWarn
            NoData -> "" to Green
            NoLocation -> "?" to RedWarn
        }

        val color = context.getColor(textColorResource)
        alertLabel.setAlarmTextColor(color)
        alertLabel.setAlarmText(warningText)
    }

    fun extractStatus(result: LocalActivity): Pair<String, Int> {
        return if (result.closestStrikeDistance < Float.POSITIVE_INFINITY) {
            val textColorResource =
                when (result.closestStrikeDistance) {
                    in 0.0..20.0 -> RedWarn
                    in 20.0..50.0 -> Yellow
                    else -> Green
                }
            val distanceUnit = context.resources.getString(result.parameters.measurementSystem.unitNameString)
            val status =
                "%.0f$distanceUnit".format(result.closestStrikeDistance) + if (result.closestStrikeDistance > 0.1) {
                    " ${result.bearingName}"
                } else {
                    ""
                }
            status to textColorResource
        } else {
            "" to Green
        }
    }

}
