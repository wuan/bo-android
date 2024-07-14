/*

   Copyright 2015-2022 Andreas Würl

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
import org.blitzortung.android.app.view.OnSharedPreferenceChangeListener
import org.blitzortung.android.app.view.PreferenceKey
import org.blitzortung.android.app.view.get
import org.blitzortung.android.data.Flags
import org.blitzortung.android.data.History
import org.blitzortung.android.data.Parameters
import org.blitzortung.android.data.beans.Station
import org.blitzortung.android.data.beans.Strike
import org.blitzortung.android.data.provider.DataProviderType
import org.blitzortung.android.data.provider.data.DataProvider
import org.blitzortung.android.data.provider.data.DataProvider.DataRetriever
import org.blitzortung.android.data.provider.data.initializeResult
import org.blitzortung.android.data.provider.result.ResultEvent
import org.blitzortung.android.jsonrpc.JsonRpcClient
import org.blitzortung.android.jsonrpc.JsonRpcResponse
import org.blitzortung.android.util.TimeFormat
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.net.URI
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class JsonRpcDataProvider @Inject constructor(
    preferences: SharedPreferences,
    private val client: JsonRpcClient
) : OnSharedPreferenceChangeListener, DataProvider {

    private lateinit var serviceUrl: URL
    private val dataBuilder: DataBuilder = DataBuilder()
    private var nextId = 0

    init {
        preferences.registerOnSharedPreferenceChangeListener(this)
        onSharedPreferenceChanged(preferences, PreferenceKey.SERVICE_URL)

        Log.v(Main.LOG_TAG, "JsonRpcDataProvider($serviceUrl)")
    }

    override val type: DataProviderType = DataProviderType.RPC

    override fun reset() {
        nextId = 0
    }

    @Throws(JSONException::class)
    private fun addStrikes(response: JsonRpcResponse, result: ResultEvent): ResultEvent {
        val isIncremental = nextId != 0
        val strikes = ArrayList<Strike>()
        val referenceTimestamp = getReferenceTimestamp(response.data)
        val strikesArray = response.data.get("s") as JSONArray
        for (i in 0 until strikesArray.length()) {
            strikes.add(dataBuilder.createDefaultStrike(referenceTimestamp, strikesArray.getJSONArray(i)))
        }
        if (response.data.has("next")) {
            nextId = response.data.get("next") as Int
        }
        return result.copy(strikes = strikes, updated = if (isIncremental) strikes.size else -1)
    }

    private fun addGridData(response: JsonRpcResponse, result: ResultEvent, size: Int): ResultEvent {
        val gridParameters = dataBuilder.createGridParameters(response.data, size)
        val referenceTimestamp = getReferenceTimestamp(response.data)

        val strikesArray = response.data.get("r") as JSONArray
        val strikes = ArrayList<Strike>()
        for (i in 0 until strikesArray.length()) {
            strikes.add(
                dataBuilder.createGridElement(
                    gridParameters,
                    referenceTimestamp,
                    strikesArray.getJSONArray(i)
                )
            )
        }

        return result.copy(strikes = strikes, gridParameters = gridParameters, referenceTime = referenceTimestamp)
    }

    @Throws(JSONException::class)
    private fun getReferenceTimestamp(response: JSONObject): Long {
        return TimeFormat.parseTime(response.getString("t"))
    }

    @Throws(JSONException::class)
    private fun addStrikesHistogram(response: JSONObject, result: ResultEvent): ResultEvent {
        var resultVar = result

        if (response.has("h")) {
            val histogramArray = response.get("h") as JSONArray

            val histogram = IntArray(histogramArray.length())

            for (i in 0 until histogramArray.length()) {
                histogram[i] = histogramArray.getInt(i)
            }
            resultVar = resultVar.copy(histogram = histogram)
        }

        return resultVar
    }

    override fun <T> retrieveData(retrieve: DataRetriever.() -> T): T = Retriever(client).retrieve()


    private inner class Retriever(val client: JsonRpcClient) : DataRetriever {
        override fun getStations(region: Int): List<Station> {
            val stations = ArrayList<Station>()

            try {
                val response = client.call(serviceUrl, "get_stations")
                val stationsArray = response.data.get("stations") as JSONArray

                for (i in 0 until stationsArray.length()) {
                    stations.add(dataBuilder.createStation(stationsArray.getJSONArray(i)))
                }
            } catch (e: Exception) {
                throw RuntimeException(e)
            }

            return stations
        }

        override fun getStrikesGrid(parameters: Parameters, history: History?, flags: Flags): ResultEvent {
            var result = initializeResult(parameters, history, flags)

            nextId = 0

            try {
                val response = JsonRpcData(client, serviceUrl).requestData(parameters)
                result = addGridData(response, result, parameters.gridSize)
                result = addStrikesHistogram(response.data, result)
            } catch (e: Exception) {
                throw RuntimeException(e)
            }

            Log.v(
                Main.LOG_TAG,
                "JsonRpcDataProvider: read %d bytes (%d grid entries, region %d)".format(
                    client.lastNumberOfTransferredBytes,
                    result.strikes?.size,
                    parameters.region
                )
            )
            return result
        }

        override fun getStrikes(parameters: Parameters, history: History?, flags: Flags): ResultEvent {
            var result = initializeResult(parameters, history, flags)

            val intervalDuration = parameters.intervalDuration
            val intervalOffset = parameters.intervalOffset
            if (intervalOffset < 0) {
                nextId = 0
            }

            try {
                val response = client.call(
                    serviceUrl,
                    "get_strikes",
                    intervalDuration,
                    if (intervalOffset < 0) intervalOffset else nextId
                )

                result = addStrikes(response, result)
                result = addStrikesHistogram(response.data, result)
            } catch (e: Exception) {
                throw RuntimeException(e)
            }

            Log.v(
                Main.LOG_TAG,
                "JsonRpcDataProvider: read %d bytes (%d new strikes)".format(
                    client.lastNumberOfTransferredBytes,
                    result.strikes?.size
                )
            )
            return result
        }
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: PreferenceKey) {
        when (key) {
            PreferenceKey.SERVICE_URL -> {
                val serviceUrl: String =
                    sharedPreferences.get(PreferenceKey.SERVICE_URL, DEFAULT_SERVICE_URL.toString())
                this.serviceUrl =
                    toCheckedUrl(if (serviceUrl.isNotBlank()) serviceUrl.trim() else DEFAULT_SERVICE_URL.toString())
            }
            else -> {}
        }
    }

    private fun toCheckedUrl(serviceUrl: String): URL {
        return try {
            URL(serviceUrl)
        } catch (e: Exception) {
            Log.e(Main.LOG_TAG, "JsonRpcDataProvider.tocheckedUrl($serviceUrl) invalid")
            DEFAULT_SERVICE_URL
        }
    }

    companion object {
        private val DATE_TIME_FORMATTER = SimpleDateFormat("yyyyMMdd'T'HH:mm:ss", Locale.US)
        private val DEFAULT_SERVICE_URL = URI("http://bo-service.tryb.de/").toURL()

        init {
            val tz = TimeZone.getTimeZone("UTC")
            DATE_TIME_FORMATTER.timeZone = tz
        }
    }
}
