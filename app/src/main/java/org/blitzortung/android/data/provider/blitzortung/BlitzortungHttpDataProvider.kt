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

        val startTime = System.currentTimeMillis() - intervalDuration * 60 * 1000
        val intervalSequence = createTimestampSequence(10 * 6 * 1000L, Math.max(latestTime, startTime))

        val strikes = retrieveData("BlitzortungHttpDataProvider: read %d bytes (%d new strikes) from region $region",
            intervalSequence.map {
                intervalTime.timeInMillis = it

                return@map readFromUrl(Type.STRIKES, region, intervalTime)
            }, { strikeMapBuilder.buildFromLine(it) })

        if (latestTime > 0L) {
            result = result.copy(incrementalData = true)
        }

        if (strikes.count() > 0) {
            //TODO Maybe we should get the maximum timestamp from strikes instead of the last?
            latestTime = strikes[strikes.lastIndex].timestamp
        }

        result = result.copy(strikes = strikes)

        return result
    }

    override fun getStrikesGrid(parameters: Parameters, result: ResultEvent): ResultEvent {
        return result
    }

    private fun readFromUrl(type: Type, region: Int, intervalTime: Calendar? = null): BufferedReader? {

        val useGzipCompression = type == Type.STATIONS

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

            reader = inputStream.bufferedReader()
        } catch (e: FileNotFoundException) {
            Log.w(Main.LOG_TAG, "URL '%s' not found".format(urlString))
            return null
        }

        return reader
    }

    /**
     * Used to retrieve Data from Blitzortung
     * @param readerSeq A sequence of nullable BufferedReader, which the data is read from
     * @param parse A Lambda which receives a sequence of lines from a buffered reader and transforms them into a sequence of T
     * @return Returns a list of parsed T's
     */
    private fun <T: Any> retrieveData(logMessage: String, readerSeq: Sequence<BufferedReader?>, parse: (String) -> T?): List<T> {
        var size = 0

        val username = username
        val password = password
        if (username == null || password == null)
            throw RuntimeException("no credentials provided")

        return readerSeq.filterNotNull().flatMap {
            val tmpList = it.lineSequence().mapNotNull {
                size += it.length

                return@mapNotNull parse(it)
            }

            Log.v(Main.LOG_TAG,
                    logMessage.format(size, tmpList.count()))

            return@flatMap tmpList
        }.toList()
    }

    override fun getStations(region: Int): List<Station> {
        return retrieveData("BlitzortungHttpProvider: read %d bytes (%d stations) from region $region",
                sequenceOf(readFromUrl(Type.STATIONS, region))) {
                    stationMapBuilder.buildFromLine(it)
                }
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
        override fun getPasswordAuthentication(): PasswordAuthentication {
            return PasswordAuthentication(username, password?.toCharArray())
        }
    }

}
