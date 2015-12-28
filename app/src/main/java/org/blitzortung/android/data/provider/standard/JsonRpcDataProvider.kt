package org.blitzortung.android.data.provider.standard

import android.util.Log
import org.blitzortung.android.app.Main
import org.blitzortung.android.data.Parameters
import org.blitzortung.android.data.beans.Station
import org.blitzortung.android.data.beans.Strike
import org.blitzortung.android.data.provider.DataBuilder
import org.blitzortung.android.data.provider.DataProvider
import org.blitzortung.android.data.provider.DataProviderType
import org.blitzortung.android.data.provider.result.ResultEvent
import org.blitzortung.android.jsonrpc.JsonRpcClient
import org.blitzortung.android.util.TimeFormat
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

class JsonRpcDataProvider(serviceUrl : String? = null) : DataProvider() {

    private val serviceUrl: String
    private val dataBuilder: DataBuilder
    private var client: JsonRpcClient? = null
    private var nextId = 0

    init {
        dataBuilder = DataBuilder()
        this.serviceUrl =
                if (serviceUrl != null && !serviceUrl.isEmpty()) serviceUrl
                else "http://bo-service.tryb.de/"
        Log.v(Main.LOG_TAG, "JsonRpcDataProvider(${this.serviceUrl})")
    }

    override fun getStrikes(parameters: Parameters, result: ResultEvent): ResultEvent {
        var result = result
        val intervalDuration = parameters.intervalDuration
        val intervalOffset = parameters.intervalOffset
        if (intervalOffset < 0) {
            nextId = 0
        }
        result = result.copy(parameters = parameters.copy(region = 0), incrementalData = (nextId != 0))

        try {
            val response = client!!.call("get_strikes", intervalDuration, if (intervalOffset < 0) intervalOffset else nextId)

            result = addStrikes(response, result)
            result = addStrikesHistogram(response, result)
        } catch (e: Exception) {
            throw RuntimeException(e)
        }

        Log.v(Main.LOG_TAG,
                "JsonRpcDataProvider: read %d bytes (%d new strikes)".format(client!!.lastNumberOfTransferredBytes, result.strikes?.size))
        return result
    }

    override fun getStrikesGrid(parameters: Parameters, result: ResultEvent): ResultEvent {
        var result = result

        nextId = 0

        val intervalDuration = parameters.intervalDuration
        val intervalOffset = parameters.intervalOffset
        val rasterBaselength = parameters.rasterBaselength
        val countThreshold = parameters.countThreshold
        val region = parameters.region

        try {
            val response = client!!.call("get_strikes_grid", intervalDuration, rasterBaselength, intervalOffset, region, countThreshold)

            val info = "%.0f km".format(rasterBaselength / 1000f)
            result = addRasterData(response, result, info)
            result = addStrikesHistogram(response, result)
        } catch (e: Exception) {
            throw RuntimeException(e)
        }

        Log.v(Main.LOG_TAG,
                "JsonRpcDataProvider: read %d bytes (%d raster positions, region %d)".format(client!!.lastNumberOfTransferredBytes, result.strikes?.size, region))
        return result
    }

    override fun getStations(region: Int): List<Station> {
        val stations = ArrayList<Station>()

        try {
            val response = client!!.call("get_stations")
            val stations_array = response.get("stations") as JSONArray

            for (i in 0..stations_array.length() - 1) {
                stations.add(dataBuilder.createStation(stations_array.getJSONArray(i)))
            }
        } catch (e: Exception) {
            throw RuntimeException(e)
        }

        return stations
    }

    override val type: DataProviderType
        get() = DataProviderType.RPC

    override fun setUp() {
        val pInfo = pInfo
        val agentSuffix = if (pInfo != null) "-" + Integer.toString(pInfo.versionCode) else ""
        client = JsonRpcClient(serviceUrl, agentSuffix)
        client!!.connectionTimeout = 40000
        client!!.socketTimeout = 40000
    }

    override fun shutDown() {
        client!!.shutdown()
        client = null
    }

    override fun reset() {
        nextId = 0
    }

    override val isCapableOfHistoricalData: Boolean
        get() = true

    @Throws(JSONException::class)
    private fun addStrikes(response: JSONObject, result: ResultEvent): ResultEvent {
        val strikes = ArrayList<Strike>()
        val referenceTimestamp = getReferenceTimestamp(response)
        val strikes_array = response.get("s") as JSONArray
        for (i in 0..strikes_array.length() - 1) {
            strikes.add(dataBuilder.createDefaultStrike(referenceTimestamp, strikes_array.getJSONArray(i)))
        }
        if (response.has("next")) {
            nextId = response.get("next") as Int
        }
        return result.copy(strikes = strikes)
    }

    private fun addRasterData(response: JSONObject, result: ResultEvent, info: String): ResultEvent {
        val rasterParameters = dataBuilder.createRasterParameters(response, info)
        val referenceTimestamp = getReferenceTimestamp(response)

        val strikes_array = response.get("r") as JSONArray
        val strikes = ArrayList<Strike>()
        for (i in 0..strikes_array.length() - 1) {
            strikes.add(dataBuilder.createRasterElement(rasterParameters, referenceTimestamp, strikes_array.getJSONArray(i)))
        }

        return result.copy(strikes = strikes, rasterParameters = rasterParameters, incrementalData = false)
    }

    @Throws(JSONException::class)
    private fun getReferenceTimestamp(response: JSONObject): Long {
        return TimeFormat.parseTime(response.getString("t"))
    }

    @Throws(JSONException::class)
    private fun addStrikesHistogram(response: JSONObject, result: ResultEvent): ResultEvent {
        var result = result

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

    companion object {
        private val DATE_TIME_FORMATTER = SimpleDateFormat("yyyyMMdd'T'HH:mm:ss")

        init {
            val tz = TimeZone.getTimeZone("UTC")
            DATE_TIME_FORMATTER.timeZone = tz
        }
    }
}
