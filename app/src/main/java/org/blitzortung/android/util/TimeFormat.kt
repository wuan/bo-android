package org.blitzortung.android.util

import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

object TimeFormat {

    private val DATE_TIME_MILLISECONDS_FORMATTER = SimpleDateFormat("yyyyMMdd'T'HH:mm:ss.SSS", Locale.US)
    private val JSON_DATE_TIME_FORMATTER = SimpleDateFormat("yyyyMMdd'T'HH:mm:ss", Locale.US)

    init {
        DATE_TIME_MILLISECONDS_FORMATTER.timeZone = TimeZone.getTimeZone("UTC")
    }

    init {
        JSON_DATE_TIME_FORMATTER.timeZone = TimeZone.getTimeZone("UTC")
    }

    fun parseTimeWithMilliseconds(timestampString: String): Long {
        try {
            return DATE_TIME_MILLISECONDS_FORMATTER.parse(timestampString).time
        } catch (e: ParseException) {
            throw IllegalArgumentException("Unable to parse millisecond time string '%s'".format(timestampString), e)
        }

    }

    fun parseTimestampWithMillisecondsFromFields(fields: Array<String>): Long {
        val timeString = fields[0].replace("-", "") + "T" + fields[1]
        return parseTimeWithMilliseconds(timeString.substring(0, timeString.length - 6))
    }

    fun parseTime(timestampString: String): Long {
        try {
            return JSON_DATE_TIME_FORMATTER.parse(timestampString).time
        } catch (e: ParseException) {
            throw IllegalArgumentException("Unable to parse time string '%s'".format(timestampString), e)
        }

    }
}