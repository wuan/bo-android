package org.blitzortung.android.dialogs

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.view.KeyEvent
import org.blitzortung.android.app.R
import org.blitzortung.android.app.components.VersionComponent

class InfoDialog(context: Context, versionComponent: VersionComponent) : AlertDialog(context) {

    init {
        setTitle("" + context.resources.getText(R.string.app_name) + " V" + versionComponent.versionName + " (" + versionComponent.versionCode + ")")
        @SuppressLint("InflateParams") val infoDialogView = layoutInflater.inflate(R.layout.info_dialog, null, false)
        setView(infoDialogView)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode == KeyEvent.KEYCODE_MENU) {
            dismiss()
            return true
        }
        return super.onKeyUp(keyCode, event)
    }
}
