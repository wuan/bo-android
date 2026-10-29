package org.blitzortung.android.data

import android.content.Context
import android.widget.Toast

/**
 * Wrapper around [Toast.makeText].
 *
 * A custom toast view was previously inflated here, but `Toast.setView` has been deprecated since
 * API 30 (and is ignored by the platform), so the default toast presentation is used instead.
 */
object CustomToast {
    fun makeText(
        context: Context,
        text: Int,
        duration: Int,
    ): Toast = Toast.makeText(context, text, duration)
}
