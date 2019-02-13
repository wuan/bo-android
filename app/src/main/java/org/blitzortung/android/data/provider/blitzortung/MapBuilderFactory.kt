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

package org.blitzortung.android.data.provider.blitzortung

import org.blitzortung.android.data.beans.DefaultStrike
import org.blitzortung.android.data.beans.Station
import org.blitzortung.android.data.beans.Strike
import org.blitzortung.android.util.TimeFormat
import org.blitzortung.android.app.R
import android.content.Context

import java.util.*
import java.util.regex.Pattern

class MapBuilderFactory constructor(private val context: Context, private val strikeLineSplitter: (String) -> Array<String> = ::lineSplitter, private val stationLineSplitter: (String) -> Array<String> = ::stationLineSplitter) {

    fun createAbstractStrikeMapBuilder(): MapBuilder<Strike> {
        return object : MapBuilder<Strike>(strikeLineSplitter) {

            var longitude: Double = 0.0
            var latitude: Double = 0.0
            var amplitude: Float = 0f
            private var timestamp: Long = 0
            private var lateralError: Int = 0
            private var altitude: Int = 0
            private var stationCount: Short = 0

            override fun prepare(fields: Array<String>) {
                timestamp = TimeFormat.parseTimestampWithMillisecondsFromFields(fields)
            }

            override fun setBuilderMap(keyValueBuilderMap: MutableMap<String, (Array<String>) -> Unit>) {
                keyValueBuilderMap.put("pos") { values ->
                    longitude = values[1].toDouble()
                    latitude = values[0].toDouble()
                    altitude = values[2].toInt()
                }
                keyValueBuilderMap.put("str") { values -> amplitude = java.lang.Float.parseFloat(values[0]) }
                keyValueBuilderMap.put("dev") { values -> lateralError = Integer.parseInt(values[0]) }
                keyValueBuilderMap.put("sta") { values -> stationCount = values.size.toShort() }
            }

            override fun build(): Strike {
                return DefaultStrike(timestamp, longitude, latitude, altitude, amplitude, stationCount, lateralError.toDouble())
            }
        }
    }

    fun createStationMapBuilder(): MapBuilder<Station> {
        return object : MapBuilder<Station>(stationLineSplitter) {

            private var name: String = context.getString(R.string.not_available)
            private var longitude: Double = 0.0
            private var latitude: Double = 0.0
            private var offlineSince: Long = 0

            override fun prepare(fields: Array<String>) {
            }

            override fun setBuilderMap(keyValueBuilderMap: MutableMap<String, (Array<String>) -> Unit>) {
                keyValueBuilderMap.put("city") { values -> name = values[0].replace("\"", "") }
                keyValueBuilderMap.put("pos") { values ->
                    longitude = values[1].toDouble()
                    latitude = values[0].toDouble()
                }
                keyValueBuilderMap.put("last_signal") { values ->
                    val dateString = values[0].replace("\"", "").replace("-", "").replace(" ", "T")
                    offlineSince = TimeFormat.parseTime(dateString)
                }
            }

            override fun build(): Station {
                return Station(longitude = longitude, latitude = latitude, name = name, offlineSince = offlineSince)
            }
        }
    }
}

fun lineSplitter(text: String): Array<String> {
    return text.split(" ".toRegex()).dropLastWhile({ it.isEmpty() }).toTypedArray()
}

fun stationLineSplitter(text: String): Array<String> {
    val matchList = ArrayList<String>()
    val regex = Pattern.compile("(\\w+(;(\"[^\"]+?\"|\\S+))+)")
    val regexMatcher = regex.matcher(text)
    while (regexMatcher.find()) {
        if (regexMatcher.group(0) != null) {
            matchList.add(regexMatcher.group(1))
        }
    }
    return matchList.toArray<String>(arrayOfNulls<String>(matchList.size))
}