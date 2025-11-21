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

package org.blitzortung.android.dialogs

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.view.KeyEvent
import org.blitzortung.android.alert.handler.AlertHandler
import org.blitzortung.android.app.R
import org.blitzortung.android.app.view.AlarmView
import org.blitzortung.android.data.MainDataHandler
import org.blitzortung.android.map.overlay.color.ColorHandler

class AlarmDialog(
    context: Context,
    private val colorHandler: ColorHandler,
    private val dataHandler: MainDataHandler,
    private val alertHandler: AlertHandler,
) : AlertDialog(context) {
    private val alarmView: AlarmView

    init {
        setTitle(
            context.getText(R.string.alarms)
        )

        @SuppressLint("InflateParams")
        val view = layoutInflater.inflate(R.layout.alarm_dialog, null)
        alarmView = view.findViewById(R.id.alarm_diagram)

        setView(view)

        setButton(AlertDialog.BUTTON_NEUTRAL, context.resources.getText(R.string.ok)) { _, _ ->
            dismiss()
        }
    }

    override fun onStart() {
        super.onStart()

        alarmView.enableDescriptionText()
        alarmView.setOnClickListener { dismiss() }

        alarmView.setColorHandler(colorHandler, dataHandler.intervalDuration)
        colorHandler.updateTarget()

        alertHandler.requestUpdates(alarmView.alertEventConsumer)
    }

    override fun onStop() {
        super.onStop()

        alertHandler.removeUpdates(alarmView.alertEventConsumer)
    }

    override fun onKeyUp(
        keyCode: Int,
        event: KeyEvent,
    ): Boolean {
        if (keyCode == KeyEvent.KEYCODE_MENU) {
            dismiss()
            return true
        }
        return super.onKeyUp(keyCode, event)
    }
}
