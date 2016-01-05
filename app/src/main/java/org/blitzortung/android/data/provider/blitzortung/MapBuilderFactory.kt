package org.blitzortung.android.data.provider.blitzortung

import org.blitzortung.android.data.beans.DefaultStrike
import org.blitzortung.android.data.beans.Station
import org.blitzortung.android.data.beans.Strike
import org.blitzortung.android.util.TimeFormat
import java.util.*
import java.util.regex.Pattern

class MapBuilderFactory constructor(private val strikeLineSplitter: (String) -> Array<String> = ::lineSplitter, private val stationLineSplitter: (String) -> Array<String> = ::stationLineSplitter) {

    fun createAbstractStrikeMapBuilder(): MapBuilder<Strike> {
        return object : MapBuilder<Strike>(strikeLineSplitter) {

            var longitude: Float = 0.toFloat()
            var latitude: Float = 0.toFloat()
            var amplitude: Float = 0.toFloat()
            private var timestamp: Long = 0
            private var lateralError: Int = 0
            private var altitude: Int = 0
            private var stationCount: Short = 0

            override fun prepare(fields: Array<String>) {
                timestamp = TimeFormat.parseTimestampWithMillisecondsFromFields(fields)
            }

            override fun setBuilderMap(keyValueBuilderMap: MutableMap<String, (Array<String>) -> Unit>) {
                keyValueBuilderMap.put("pos", { values ->
                    longitude = java.lang.Float.valueOf(values[1])
                    latitude = java.lang.Float.parseFloat(values[0])
                    altitude = Integer.parseInt(values[2])
                })
                keyValueBuilderMap.put("str", { values -> amplitude = java.lang.Float.parseFloat(values[0]) })
                keyValueBuilderMap.put("dev", { values -> lateralError = Integer.parseInt(values[0]) })
                keyValueBuilderMap.put("sta", { values -> stationCount = values.size.toShort() })
            }

            override fun build(): Strike {
                return DefaultStrike(timestamp, longitude, latitude, altitude, amplitude, stationCount, lateralError.toFloat())
            }
        }
    }

    fun createStationMapBuilder(): MapBuilder<Station> {
        return object : MapBuilder<Station>(stationLineSplitter) {

            private var name: String = "n/a"
            private var longitude: Float = 0.toFloat()
            private var latitude: Float = 0.toFloat()
            private var offlineSince: Long = 0

            override fun prepare(fields: Array<String>) {
            }

            override fun setBuilderMap(keyValueBuilderMap: MutableMap<String, (Array<String>) -> Unit>) {
                keyValueBuilderMap.put("city", { values -> name = values[0].replace("\"", "") })
                keyValueBuilderMap.put("pos", { values ->
                    longitude = java.lang.Float.parseFloat(values[1])
                    latitude = java.lang.Float.parseFloat(values[0])
                })
                keyValueBuilderMap.put("last_signal", { values ->
                    val dateString = values[0].replace("\"", "").replace("-", "").replace(" ", "T")
                    offlineSince = TimeFormat.parseTime(dateString)
                })
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