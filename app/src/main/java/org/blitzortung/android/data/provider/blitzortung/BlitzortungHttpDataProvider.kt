package org.blitzortung.android.data.provider.blitzortung

import android.util.Log
import org.blitzortung.android.app.Main
import org.blitzortung.android.data.Parameters
import org.blitzortung.android.data.beans.Station
import org.blitzortung.android.data.beans.Strike
import org.blitzortung.android.data.provider.DataProvider
import org.blitzortung.android.data.provider.DataProviderType
import org.blitzortung.android.data.provider.result.ResultEvent
import java.io.BufferedReader
import java.io.FileNotFoundException
import java.io.InputStreamReader
import java.net.Authenticator
import java.net.PasswordAuthentication
import java.net.URL
import java.util.*
import java.util.zip.GZIPInputStream

class BlitzortungHttpDataProvider @JvmOverloads constructor(private val urlFormatter: UrlFormatter = UrlFormatter(), mapBuilderFactory: MapBuilderFactory = MapBuilderFactory()) : DataProvider() {

    private val strikeMapBuilder: MapBuilder<Strike>
    private val stationMapBuilder: MapBuilder<Station>
    private var latestTime: Long = 0

    init {
        strikeMapBuilder = mapBuilderFactory.createAbstractStrikeMapBuilder()
        stationMapBuilder = mapBuilderFactory.createStationMapBuilder()
    }

    override fun getStrikes(parameters: Parameters, result: ResultEvent): ResultEvent {
        var result = result
        val intervalDuration = parameters.intervalDuration
        val region = parameters.region

        val tz = TimeZone.getTimeZone("UTC")
        val intervalTime = GregorianCalendar(tz)

        val username = username
        val password = password
        if (username != null && password != null) {

            val strikes = ArrayList<Strike>()
            val intervalTimer = IntervalTimer(10 * 60 * 1000L)
            val startTime = System.currentTimeMillis() - intervalDuration * 60 * 1000

            intervalTimer.startInterval(Math.max(latestTime, startTime))

            while (intervalTimer.hasNext()) {
                intervalTime.timeInMillis = intervalTimer.next()

                val reader = readFromUrl(Type.STRIKES, region, intervalTime) ?: continue

                var size = 0
                reader.use { reader ->
                    reader.forEachLine { line ->
                        size += line.length

                        val strike = strikeMapBuilder.buildFromLine(line)
                        val timestamp = strike.timestamp

                        if (timestamp > latestTime && timestamp >= startTime) {
                            strikes.add(strike)
                        }
                    }
                }
                Log.v(Main.LOG_TAG,
                        "BlitzortungHttpDataProvider: read %d bytes (%d new strikes) from region %d".format(size, strikes.size, region))

                reader.close()
            }

            if (latestTime > 0L) {
                result = result.copy(incrementalData = true)
            }

            if (strikes.size > 0) {
                latestTime = strikes[strikes.size - 1].timestamp
            }
            result = result.copy(strikes = strikes)
        } else {
            throw RuntimeException("no credentials provided")
        }
        return result
    }

    override fun getStrikesGrid(parameters: Parameters, result: ResultEvent): ResultEvent {
        return result
    }

    private fun readFromUrl(type: Type, region: Int, intervalTime: Calendar? = null): BufferedReader? {

        val useGzipCompression = if (type == Type.STATIONS) true else false

        Authenticator.setDefault(MyAuthenticator())

        val reader: BufferedReader

        val urlString = urlFormatter.getUrlFor(type, region, intervalTime, useGzipCompression)
        try {
            val url: URL
            url = URL(urlString)
            val connection = url.openConnection()
            connection.connectTimeout = 60000
            connection.readTimeout = 60000
            connection.allowUserInteraction = false
            var inputStream = connection.inputStream
            if (useGzipCompression) {
                inputStream = GZIPInputStream(inputStream)
            }
            reader = BufferedReader(InputStreamReader(inputStream))
        } catch (e: FileNotFoundException) {
            Log.w(Main.LOG_TAG, "URL '%s' not found".format(urlString))
            return null
        }

        return reader
    }

    override fun getStations(region: Int): List<Station> {
        val stations = ArrayList<Station>()

        val username = username
        val password = password
        if (username != null && password != null) {

            val reader = readFromUrl(Type.STATIONS, region)

            var size = 0
            reader?.use { reader ->
                reader.forEachLine { line ->
                    size += line.length
                    try {
                        val station = stationMapBuilder.buildFromLine(line)
                        stations.add(station)
                    } catch (e: NumberFormatException) {
                        Log.w(Main.LOG_TAG, "BlitzortungHttpProvider: error parsing '%s'".format(line))
                    }
                }
            }
            Log.v(Main.LOG_TAG,
                    "BlitzortungHttpProvider: read %d bytes (%d stations) from region %d".format(size, stations.size, region))

        } else {
            throw RuntimeException("no credentials provided")
        }

        return stations
    }

    override val type: DataProviderType
        get() = DataProviderType.HTTP

    override fun setUp() {
    }

    override fun shutDown() {
    }

    override fun reset() {
        latestTime = 0
    }

    override val isCapableOfHistoricalData: Boolean
        get() = false

    enum class Type {
        STRIKES, STATIONS
    }

    private inner class MyAuthenticator : Authenticator() {

        public override fun getPasswordAuthentication(): PasswordAuthentication {
            return PasswordAuthentication(username, password?.toCharArray())
        }
    }

}
