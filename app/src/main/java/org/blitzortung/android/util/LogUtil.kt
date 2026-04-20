package org.blitzortung.android.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object LogUtil {
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)

    val timestamp: String
        get() = dateFormat.format(Date())
}
