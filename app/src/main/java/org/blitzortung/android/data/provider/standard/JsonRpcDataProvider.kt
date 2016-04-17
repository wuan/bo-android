/*

   Copyright 2015, 2016 Andreas Würl

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

import android.content.SharedPreferences
import android.util.Log
import org.blitzortung.android.app.Main
import org.blitzortung.android.app.view.PreferenceKey
import org.blitzortung.android.app.view.get
import org.blitzortung.android.data.Parameters
import org.blitzortung.android.data.beans.Station
import org.blitzortung.android.data.beans.Strike
import org.blitzortung.android.data.provider.DataBuilder
import org.blitzortung.android.data.provider.DataProvider
import org.blitzortung.android.data.provider.result.DataEvent
import org.blitzortung.android.jsonrpc.JsonRpcClient
import org.blitzortung.android.util.TimeFormat
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

class JsonRpcDataProvider(private val agentSuffix: String, preferences: SharedPreferences) : DataProvider {

    private val dataBuilder: DataBuilder

    private var serviceUrl: String = ""

    private var nextId = 0

    init {
        dataBuilder = DataBuilder()

        Log.v(Main.LOG_TAG, "JsonRpcDataProvider(${this.serviceUrl})")
        onSharedPreferencesChanged(preferences, PreferenceKey.SERVICE_URL)
    }

    override fun getStrikes(parameters: Parameters): DataEvent {
        val result = createResultEvent(parameters)

        val client = JsonRpcClient(serviceUrl, agentSuffix)
        client.connectionTimeout = 40000
        client.socketTimeout = 40000

        return if (parameters.isRaster()) {
            getStrikesGrid(client, parameters, result)
        } else {
            getStrikes(client, parameters, result)
        }
    }


    private fun getStrikes(client: JsonRpcClient, parameters: Parameters, data: DataEvent): DataEvent {
        var result = data
        val intervalDuration = parameters.intervalDuration
        val intervalOffset = parameters.intervalOffset
        if (intervalOffset < 0) {
            nextId = 0
        }
        result = result.copy(incrementalData = (nextId != 0))

        try {
            val response = client.call("get_strikes", intervalDuration, if (intervalOffset < 0) intervalOffset else nextId)

            result = addStrikes(response, result)
            result = addStrikesHistogram(response, result)
        } catch (e: Exception) {
            throw RuntimeException(e)
        }

        Log.d(Main.LOG_TAG,
                "JsonRpcDataProvider: read %d bytes (%d new strikes)".format(client.lastNumberOfTransferredBytes, result.strikes?.size))
        return result
    }

    private fun getStrikesGrid(client: JsonRpcClient, parameters: Parameters, dataParam: DataEvent): DataEvent {
        var result = dataParam

        nextId = 0

        val intervalDuration = parameters.intervalDuration
        val intervalOffset = parameters.intervalOffset
        val rasterBaselength = parameters.rasterBaselength
        val countThreshold = parameters.countThreshold
        val region = parameters.region

        try {
            val response = client.call("get_strikes_grid", intervalDuration, rasterBaselength, intervalOffset, region, countThreshold)

            val info = "%.0f km".format(rasterBaselength / 1000f)
            result = addRasterData(response, result, info)
            result = addStrikesHistogram(response, result)
        } catch (e: Exception) {
            throw RuntimeException(e)
        }

        Log.v(Main.LOG_TAG,
                "JsonRpcDataProvider: read %d bytes (%d raster positions, region %d)".format(client.lastNumberOfTransferredBytes, result.strikes?.size, region))
        return result
    }

    private fun getStations(client: JsonRpcClient, region: Int): List<Station> {
        val stations = ArrayList<Station>()

        try {
            val response = client.call("get_stations")
            val stations_array = response.get("stations") as JSONArray

            for (i in 0..stations_array.length() - 1) {
                stations.add(dataBuilder.createStation(stations_array.getJSONArray(i)))
            }
        } catch (e: Exception) {
            throw RuntimeException(e)
        }

        return stations
    }

    override fun reset() {
        nextId = 0
    }

    @Throws(JSONException::class)
    private fun addStrikes(response: JSONObject, data: DataEvent): DataEvent {
        val strikes = ArrayList<Strike>()
        val referenceTimestamp = getReferenceTimestamp(response)
        val strikes_array = response.get("s") as JSONArray
        for (i in 0..strikes_array.length() - 1) {
            strikes.add(dataBuilder.createDefaultStrike(referenceTimestamp, strikes_array.getJSONArray(i)))
        }
        if (response.has("next")) {
            nextId = response.get("next") as Int
        }
        return data.copy(strikes = strikes)
    }

    private fun addRasterData(response: JSONObject, data: DataEvent, info: String): DataEvent {
        val rasterParameters = dataBuilder.createRasterParameters(response, info)
        val referenceTimestamp = getReferenceTimestamp(response)

        val strikes_array = response.get("r") as JSONArray
        val strikes = ArrayList<Strike>()
        for (i in 0..strikes_array.length() - 1) {
            strikes.add(dataBuilder.createRasterElement(rasterParameters, referenceTimestamp, strikes_array.getJSONArray(i)))
        }

        return data.copy(strikes = strikes, rasterParameters = rasterParameters, incrementalData = false)
    }

    @Throws(JSONException::class)
    private fun getReferenceTimestamp(response: JSONObject): Long {
        return TimeFormat.parseTime(response.getString("t"))
    }

    @Throws(JSONException::class)
    private fun addStrikesHistogram(response: JSONObject, dataParam: DataEvent): DataEvent {
        var result = dataParam

        if (response.has("h")) {
            val histogram_array = response.get("h") as JSONArray

            val histogram = IntArray(histogram_array.length())

            for (i in 0..histogram_array.length() - 1) {
                histogram[i] = histogram_array.getInt(i)
            }
            result = result.copy(histogram = histogram)
        }

        return result
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: PreferenceKey) {
        when (key) {
            PreferenceKey.SERVICE_URL -> {
                val serviceUrl = sharedPreferences.get(key, "").trim()
                this.serviceUrl =
                        if (!serviceUrl.isEmpty()) serviceUrl
                        else "http://bo-service.tryb.de/"
            }

            else -> {}
        }
    }

    companion object {
        private val DATE_TIME_FORMATTER = SimpleDateFormat("yyyyMMdd'T'HH:mm:ss")

        init {
            val tz = TimeZone.getTimeZone("UTC")
            DATE_TIME_FORMATTER.timeZone = tz
        }
    }
}
