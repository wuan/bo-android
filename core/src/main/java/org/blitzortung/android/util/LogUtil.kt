package org.blitzortung.android.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object LogUtil {
    const val LOG_TAG = "BO_ANDROID"

    /** Formats epoch milliseconds for log messages; not thread-safe by design, see [formatTimestamp]. */
    internal val timestampFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)

    val timestamp: String
        get() = timestampFormat.format(Date())

    /** Formats [epochMillis] (typically a strike timestamp) for diagnostics. */
    fun formatTimestamp(epochMillis: Long): String =
        synchronized(timestampFormat) {
            timestampFormat.format(Date(epochMillis))
        }
}
