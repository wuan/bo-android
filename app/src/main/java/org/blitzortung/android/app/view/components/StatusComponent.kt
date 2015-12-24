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
import org.blitzortung.android.util.UI

class StatusComponent(activity: Activity) : AlertLabel {

    private val alertLabelHandler: AlertLabelHandler

    val alertEventConsumer: (AlertEvent) -> Unit

    private val status: TextView

    private val warning: TextView

    private val progressBar: ProgressBar

    private val errorIndicator: ImageView

    init {
        val textSize = UI.textSize(activity)

        status = activity.findViewById(R.id.status) as TextView
        status.textSize = textSize

        warning = activity.findViewById(R.id.warning) as TextView
        warning.textSize = textSize

        progressBar = activity.findViewById(R.id.progress) as ProgressBar
        progressBar.visibility = View.INVISIBLE

        errorIndicator = activity.findViewById(R.id.error_indicator) as ImageView
        errorIndicator.visibility = View.INVISIBLE

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
