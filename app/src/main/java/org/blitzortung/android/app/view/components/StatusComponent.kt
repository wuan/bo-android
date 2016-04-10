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

package org.blitzortung.android.app.view.components

import android.app.Activity
import android.view.View
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import org.blitzortung.android.alert.AlertLabel
import org.blitzortung.android.alert.AlertLabelHandler
import org.blitzortung.android.alert.event.AlertEvent
import org.blitzortung.android.alert.event.AlertResultEvent
import org.blitzortung.android.app.R

class StatusComponent(activity: Activity) : AlertLabel {

    private val alertLabelHandler: AlertLabelHandler

    val alertEventConsumer: (AlertEvent) -> Unit

    private val status: TextView

    private val warning: TextView

    private val progressBar: ProgressBar

    private val errorIndicator: ImageView

    init {
        status = activity.findViewById(R.id.status) as TextView

        warning = activity.findViewById(R.id.warning) as TextView

        progressBar = (activity.findViewById(R.id.progress) as ProgressBar).apply {
            visibility = View.INVISIBLE
        }

        errorIndicator = (activity.findViewById(R.id.error_indicator) as ImageView).apply {
            visibility = View.INVISIBLE
        }

        alertLabelHandler = AlertLabelHandler(this, activity.resources)

        alertEventConsumer = { event ->
            alertLabelHandler.apply(
                    if (event is AlertResultEvent)
                        event.alertResult
                    else
                        null)
        }
    }

    fun startProgress() {
        progressBar.visibility = View.VISIBLE
        progressBar.progress = 0
    }

    fun stopProgress() {
        progressBar.visibility = View.INVISIBLE
        progressBar.progress = progressBar.max
    }

    fun indicateError(indicateError: Boolean) {
        errorIndicator.visibility = if (indicateError) View.VISIBLE else View.INVISIBLE
    }

    fun setText(statusText: String) {
        status.text = statusText
    }

    override fun setAlarmTextColor(color: Int) {
        warning.setTextColor(color)
    }

    override fun setAlarmText(alarmText: String) {
        warning.text = alarmText
    }
}
