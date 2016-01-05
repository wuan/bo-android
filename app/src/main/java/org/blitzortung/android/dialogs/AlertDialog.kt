package org.blitzortung.android.dialogs

import android.content.Context
import android.os.Bundle
import android.view.KeyEvent

import org.blitzortung.android.app.AppService
import org.blitzortung.android.app.R
import org.blitzortung.android.app.view.AlertView
import org.blitzortung.android.map.overlay.color.ColorHandler

class AlertDialog(context: Context, private val service: AppService?, private val colorHandler: ColorHandler) : android.app.AlertDialog(context) {
    internal var intervalDuration: Int = 0
    private lateinit var alertView: AlertView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.alarm_dialog)
    }

    fun setIntervalDuration(intervalDuration: Int) {
        this.intervalDuration = intervalDuration
    }

    override fun onStart() {
        super.onStart()

        alertView = findViewById(R.id.alarm_diagram) as AlertView

        setTitle(context.getString(R.string.alarms))

        if (service != null) {
            alertView.setColorHandler(colorHandler, service.dataHandler().intervalDuration)
            alertView.alertEventConsumer.invoke(service.alertEvent())
            service.addAlertConsumer(alertView!!.alertEventConsumer)
        }
        colorHandler.updateTarget()
    }

    override fun onStop() {
        super.onStop()

        service?.removeAlertListener(alertView!!.alertEventConsumer)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode == KeyEvent.KEYCODE_MENU) {
            dismiss()
            return true
        }
        return super.onKeyUp(keyCode, event)
    }
}
