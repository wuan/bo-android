/*

   Copyright 2015 Andreas Würl

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.

*/

package org.blitzortung.android.util

import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*

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