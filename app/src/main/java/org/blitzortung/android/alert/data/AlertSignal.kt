package org.blitzortung.android.alert.data

import android.net.Uri

data class AlertSignal(
        val vibrationDuration: Int = 0,
        val soundSignal: Uri? = null
) {
}

