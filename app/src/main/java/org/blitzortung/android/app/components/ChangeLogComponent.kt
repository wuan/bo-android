package org.blitzortung.android.app.components

import android.app.AlertDialog
import android.content.Context
import org.blitzortung.android.dialogs.changelog.ChangelogDialog
import javax.inject.Inject

class ChangeLogComponent @Inject constructor(
) {
    fun getChangeLogDialog(context: Context): AlertDialog? {
        return ChangelogDialog(context)
    }

    fun showChangeLogDialog(context: Context) {
        getChangeLogDialog(context)?.show()
    }
}