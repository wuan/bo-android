package org.blitzortung.android.app.components

import android.app.AlertDialog
import android.content.Context
import de.cketti.library.changelog.ChangeLog
import javax.inject.Inject

class ChangeLogComponent @Inject constructor(
) {
    fun getChangeLogDialog(context: Context): AlertDialog? {
        return ChangeLog(context).fullLogDialog
    }

    fun showChangeLogDialog(context: Context) {
        getChangeLogDialog(context)?.show()
    }
}