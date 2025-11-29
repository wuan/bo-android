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

import javax.inject.Inject
import javax.inject.Singleton
import org.blitzortung.android.data.beans.DefaultStrike
import org.blitzortung.android.data.beans.Strike
import org.blitzortung.android.util.TimeFormat

class MapBuilderFailedException(message: String) : Throwable(message)

@Singleton
class MapBuilderFactory(
    private val strikeLineSplitter: (String) -> Array<String>,
) {
    @Inject
    constructor() : this(::lineSplitter)

    fun createStrikeMapBuilder(): MapBuilder<Strike> {
        return object : MapBuilder<Strike>(strikeLineSplitter) {
            init {
                setBuilderMap()
            }

            var longitude: Double = 0.0
            var latitude: Double = 0.0
            var amplitude: Float = 0f
            private var timestamp: Long = 0
            private var lateralError: Int = 0
            private var altitude: Int = 0
            private var stationCount: Short = 0

            override fun prepare(fields: Array<String>) {
                if (fields.size > 2) {
                    timestamp = TimeFormat.parseTimestampWithMillisecondsFromFields(fields)
                } else {
                    throw MapBuilderFailedException("not enough values for time parsing")
                }
            }

            fun setBuilderMap() {
                keyValueBuilderMap["pos"] = { values ->
                    if (values.size >= 3) {
                        longitude = values[1].toDouble()
                        latitude = values[0].toDouble()
                        altitude = values[2].toInt()
                    } else {
                        throw MapBuilderFailedException("not enough values for location parsing")
                    }
                }
                keyValueBuilderMap["str"] = { values -> amplitude = java.lang.Float.parseFloat(values[0]) }
                keyValueBuilderMap["dev"] = { values -> lateralError = Integer.parseInt(values[0]) }
                keyValueBuilderMap["sta"] = { values -> stationCount = values.size.toShort() }
            }

            override fun build(): Strike {
                return DefaultStrike(
                    timestamp,
                    longitude,
                    latitude,
                    altitude,
                    amplitude,
                    stationCount,
                    lateralError.toDouble(),
                )
            }
        }
    }
}

fun lineSplitter(text: String): Array<String> {
    return text.split(" ".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
}
