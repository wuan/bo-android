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
import org.blitzortung.android.dialogs.AlarmDialog
import org.blitzortung.android.dialogs.AlertDialogColorHandler
import org.blitzortung.android.dialogs.InfoDialog
import org.blitzortung.android.dialogs.LogDialog
import org.blitzortung.android.settings.SettingsActivity

data class MainPopupMenuDependencies(
    val preferences: SharedPreferences,
    val dataHandler: MainDataHandler,
    val alertHandler: AlertHandler,
    val buildVersion: BuildVersion,
    val changeLogComponent: ChangeLogComponent,
)

class MainPopupMenu(
    context: Context,
    anchor: View,
    private val dependencies: MainPopupMenuDependencies,
) : PopupMenu(context, anchor) {
    /**
     * Invoked when the "Quick guide" menu entry is selected.
     */
    var onShowDocOverlay: () -> Unit = {}

    init {
        setOnMenuItemClickListener(
            ClickListener(context, dependencies.preferences, dependencies.dataHandler, dependencies.alertHandler),
        )
    }

    inner class ClickListener(
        private val context: Context,
        private val preferences: SharedPreferences,
        private val dataHandler: MainDataHandler,
        private val alertHandler: AlertHandler,
    ) : OnMenuItemClickListener {
        override fun onMenuItemClick(item: MenuItem?): Boolean {
            if (item?.itemId == R.id.menu_preferences) {
                context.startActivity(Intent(context, SettingsActivity::class.java))
            } else if (item?.itemId == R.id.menu_doc_overlay) {
                onShowDocOverlay()
            } else {
                val dialog =
                    when (item?.itemId) {
                        R.id.menu_info -> InfoDialog(context, dependencies.buildVersion)

                        R.id.menu_alarms ->
                            AlarmDialog(
                                context,
                                AlertDialogColorHandler(preferences),
                                dataHandler,
                                alertHandler,
                            )

                        R.id.menu_log ->
                            LogDialog(context, dataHandler.calculateTotalCacheSize(), dependencies.buildVersion)

                        R.id.menu_changelog -> dependencies.changeLogComponent.getChangeLogDialog(context)

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
