package org.blitzortung.android.app

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.util.Log
import android.view.MenuItem
import android.view.View
import androidx.appcompat.widget.PopupMenu
import org.blitzortung.android.alert.handler.AlertHandler
import org.blitzortung.android.app.components.BuildVersion
import org.blitzortung.android.app.components.ChangeLogComponent
import org.blitzortung.android.data.MainDataHandler
import org.blitzortung.android.dialogs.AlertDialog
import org.blitzortung.android.dialogs.AlertDialogColorHandler
import org.blitzortung.android.dialogs.InfoDialog
import org.blitzortung.android.dialogs.LogDialog
import org.blitzortung.android.settings.SettingsActivity

class MainPopupMenu(
    context: Context,
    anchor: View,
    preferences: SharedPreferences,
    dataHandler: MainDataHandler,
    alertHandler: AlertHandler,
    private val buildVersion: BuildVersion,
    private val changeLogComponent: ChangeLogComponent
) : PopupMenu(context, anchor) {

    init {
        setOnMenuItemClickListener(ClickListener(context, preferences, dataHandler, alertHandler))
    }

    inner class ClickListener(
        private val context: Context,
        private val preferences: SharedPreferences,
        private val dataHandler: MainDataHandler,
        private val alertHandler: AlertHandler
    ) : OnMenuItemClickListener {
        override fun onMenuItemClick(item: MenuItem?): Boolean {
            if (item?.itemId == R.id.menu_preferences) {
                context.startActivity(Intent(context, SettingsActivity::class.java))
            } else {
                val dialog = when (item?.itemId) {
                    R.id.menu_info -> InfoDialog(context, buildVersion)

                    R.id.menu_alarms -> AlertDialog(
                        context,
                        AlertDialogColorHandler(preferences),
                        dataHandler,
                        alertHandler
                    )

                    R.id.menu_log -> LogDialog(context, dataHandler.calculateTotalCacheSize(), buildVersion)

                    R.id.menu_changelog -> changeLogComponent.getChangeLogDialog(context)

                    else -> null
                }

                if (dialog is Dialog) {
                    dialog.show()
                } else {
                    return false
                }
            }

            return true
        }
    }

    fun showPopupMenu() {
        inflate(R.menu.main_menu)
        show()
        Log.v(Main.LOG_TAG, "MainPopupMenu.showPopupMenu()")
    }
}
