package org.blitzortung.android.app.components

import android.content.Context
import com.michaelflisar.changelog.ChangelogBuilder
import javax.inject.Inject

class ChangeLogComponent @Inject constructor(
        private val context: Context
) {
    fun showChangeLog(): Unit {
        ChangelogBuilder()
                .withManagedShowOnStart(false)
                .buildAndStartActivity(context, true)

    }
}