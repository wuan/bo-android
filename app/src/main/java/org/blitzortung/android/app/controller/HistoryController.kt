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

package org.blitzortung.android.app.controller

import android.app.Activity
import android.view.View
import android.widget.ImageButton
import android.widget.Toast
import org.blitzortung.android.app.ButtonGroup
import org.blitzortung.android.app.ParametersComponent
import org.blitzortung.android.app.R

class HistoryController(activity: Activity, private val buttonHandler: ButtonColumnHandler<ImageButton, ButtonGroup>, private val parametersComponent: ParametersComponent) {

    private val buttons: MutableCollection<ImageButton> = arrayListOf()

    private lateinit var historyRewind: ImageButton

    private lateinit var historyForward: ImageButton

    private lateinit var goRealtime: ImageButton

    init {
        setupHistoryRewindButton(activity)
        setupHistoryForwardButton(activity)
        setupGoRealtimeButton(activity)

        setRealtimeData(parametersComponent.isRealtime)
    }

    fun setRealtimeData(realtimeData: Boolean) {
        historyRewind.visibility = View.VISIBLE
        val historyButtonsVisibility = if (realtimeData) View.INVISIBLE else View.VISIBLE
        historyForward.visibility = historyButtonsVisibility
        goRealtime.visibility = historyButtonsVisibility
        updateButtonColumn()
    }

    private fun setupHistoryRewindButton(activity: Activity) {
        historyRewind = addButtonWithAction(activity, R.id.historyRew, { v ->
            if (parametersComponent.rewInterval()) {
                historyForward.visibility = View.VISIBLE
                goRealtime.visibility = View.VISIBLE
                updateButtonColumn()
            } else {
                val toast = Toast.makeText(activity.baseContext, activity.resources.getText(R.string.historic_timestep_limit_reached), Toast.LENGTH_SHORT)
                toast.show()
            }
        })
    }

    private fun setupHistoryForwardButton(activity: Activity) {
        historyForward = addButtonWithAction(activity, R.id.historyFfwd, { v ->
            if (parametersComponent.ffwdInterval()) {
                if (parametersComponent.parameters.isRealtime()) {
                    configureForRealtimeOperation()
                }
            }
        })
    }

    private fun setupGoRealtimeButton(activity: Activity) {
        goRealtime = addButtonWithAction(activity, R.id.goRealtime, { v ->
            if (parametersComponent.goRealtime()) {
                configureForRealtimeOperation()
            }
        })
    }

    private fun addButtonWithAction(activity: Activity, id: Int, action: (View) -> Unit): ImageButton {
        return (activity.findViewById(id) as ImageButton).apply {
            buttons.add(this)
            visibility = View.INVISIBLE
            setOnClickListener(action)
        }
    }

    private fun configureForRealtimeOperation() {
        historyForward.visibility = View.INVISIBLE
        goRealtime.visibility = View.INVISIBLE
        updateButtonColumn()
    }

    private fun updateButtonColumn() {
        buttonHandler.updateButtonColumn()
    }

    fun getButtons(): Collection<ImageButton> {
        return buttons.toList()
    }
}
