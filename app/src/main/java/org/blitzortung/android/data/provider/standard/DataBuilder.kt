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

package org.blitzortung.android.data.provider.standard

import org.blitzortung.android.data.beans.DefaultStrike
import org.blitzortung.android.data.beans.GridElement
import org.blitzortung.android.data.beans.GridParameters
import org.blitzortung.android.data.beans.Strike
import org.blitzortung.android.util.TimeFormat
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

internal class DataBuilder {
    fun createDefaultStrike(
        referenceTimestamp: Long,
        jsonArray: JSONArray,
    ): Strike {
        try {
            return DefaultStrike(
                timestamp = referenceTimestamp - 1000 * jsonArray.getInt(0),
                longitude = jsonArray.getDouble(1),
                latitude = jsonArray.getDouble(2),
                lateralError = jsonArray.getDouble(3),
                altitude = 0,
                amplitude = jsonArray.getDouble(4).toFloat(),
            )
        } catch (e: JSONException) {
            throw IllegalStateException("error with JSON format while parsing strike data", e)
        }
    }

    @Throws(JSONException::class)
    fun createGridParameters(
        response: JSONObject,
        gridSize: Int,
    ): GridParameters {
        return GridParameters(
            longitudeStart = response.getDouble("x0"),
            latitudeStart = response.getDouble("y1"),
            longitudeDelta = response.getDouble("xd"),
            latitudeDelta = response.getDouble("yd"),
            longitudeBins = response.getInt("xc"),
            latitudeBins = response.getInt("yc"),
            size = gridSize,
        )
    }

    @Throws(JSONException::class)
    fun createGridElement(
        gridParameters: GridParameters,
        referenceTimestamp: Long,
        jsonArray: JSONArray,
    ): GridElement {
        return GridElement(
            timestamp = referenceTimestamp + 1000 * jsonArray.getInt(3),
            longitude = gridParameters.getCenterLongitude(jsonArray.getInt(0)),
            latitude = gridParameters.getCenterLatitude(jsonArray.getInt(1)),
            multiplicity = jsonArray.getInt(2),
        )
    }
}
