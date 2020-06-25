/*

   Copyright 2015-2016 Andreas Würl

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

import android.content.SharedPreferences
import android.util.Log
import org.blitzortung.android.app.Main
import org.blitzortung.android.app.view.OnSharedPreferenceChangeListener
import org.blitzortung.android.app.view.PreferenceKey
import org.blitzortung.android.app.view.get
import org.blitzortung.android.data.Parameters
import org.blitzortung.android.data.beans.Station
import org.blitzortung.android.data.beans.Strike
import org.blitzortung.android.data.provider.data.DataProvider
import org.blitzortung.android.data.provider.data.DataProvider.DataRetriever
import org.blitzortung.android.data.provider.DataProviderType
import org.blitzortung.android.data.provider.result.ResultEvent
import java.io.BufferedReader
import java.io.ByteArrayInputStream
import java.io.FileNotFoundException
import java.io.InputStreamReader
import java.net.*
import java.util.*
import java.util.zip.GZIPInputStream
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.max

@Singleton
class BlitzortungHttpDataProvider @Inject constructor(
        preferences: SharedPreferences,
        private val urlFormatter: UrlFormatter,
        mapBuilderFactory: MapBuilderFactory
) : OnSharedPreferenceChangeListener, DataProvider {

    private val strikeMapBuilder: MapBuilder<Strike> = mapBuilderFactory.createAbstractStrikeMapBuilder()
    private val stationMapBuilder: MapBuilder<Station> = mapBuilderFactory.createStationMapBuilder()
    private var latestTime: Long = 0
    private var strikes: List<Strike> = emptyList()
    private var parameters: Parameters? = null

    private lateinit var username: String
    private lateinit var password: String

    init {
        preferences.registerOnSharedPreferenceChangeListener(this)
        onSharedPreferenceChanged(preferences, PreferenceKey.USERNAME, PreferenceKey.PASSWORD )
    }

    private fun readFromUrl(type: Type, region: Int, intervalTime: Calendar? = null): BufferedReader? {

        val useGzipCompression = type == Type.STATIONS

        val reader: BufferedReader

        val urlString = urlFormatter.getUrlFor(type, region, intervalTime, useGzipCompression)

        try {
            val url = URL(urlString)
            val connection = url.openConnection()
            connection.connectTimeout = 40000
            connection.readTimeout = 40000
            connection.allowUserInteraction = false
            var inputStream = connection.inputStream
            if (useGzipCompression) {
                inputStream = GZIPInputStream(inputStream)
            }

            reader = inputStream.bufferedReader()
        } catch (e: Exception) {
            when (e) {
                is FileNotFoundException -> {
                    Log.w(Main.LOG_TAG, "BlitzortungHttpDataProvider.readFromUrl() $urlString not found")
                    return BufferedReader(InputStreamReader(ByteArrayInputStream("".toByteArray())))
                }
                else -> {
                    Log.w(Main.LOG_TAG, "BlitzortungHttpDataProvider.readFromUrl() $urlString failed")
                    throw RuntimeException(e)
                }
            }
        }

        Log.v(Main.LOG_TAG, "BlitzortungHttpDataProvider.readFromUrl() $urlString")

        return reader
    }

    /**
     * Used to retrieve Data from Blitzortung
     * @param readerSeq A sequence of nullable BufferedReader, which the data is read from
     * @param parse A Lambda which receives a sequence of lines from a buffered reader and transforms them into a sequence of T
     * @return Returns a list of parsed T's
     */
    private fun <T : Any> retrieveData(logMessage: String, readerSeq: Sequence<BufferedReader?>, parse: (String) -> T?): List<T> {
        var size = 0

        val strikeSequence: Sequence<T> = readerSeq.filterNotNull().flatMap { reader ->
            reader.lineSequence().mapNotNull { line ->
                size += line.length

                parse(line)
            }
        }

        val strikeList = try {
            strikeSequence.toList()
        } catch (e: SocketException) {
            Log.w(Main.LOG_TAG, e)
            emptyList<T>()
        } catch (e: SocketTimeoutException) {
            Log.w(Main.LOG_TAG, e)
            emptyList<T>()
        }

        Log.v(Main.LOG_TAG, logMessage.format(size, strikeList.count()))

        return strikeList
    }

    override val type: DataProviderType = DataProviderType.HTTP

    override fun reset() {
        latestTime = 0L
        strikes = emptyList()
    }

    enum class Type {
        STRIKES, STATIONS
    }

    override fun <T> retrieveData(retrieve: DataRetriever.() -> T): T {
        return Retriever().retrieve()
    }

    private inner class Retriever : DataRetriever {
        override fun getStrikes(parameters: Parameters, result: ResultEvent): ResultEvent {
            var resultVar = result
            if (parameters != this@BlitzortungHttpDataProvider.parameters) {
                this@BlitzortungHttpDataProvider.parameters = parameters
                reset()
            }

            val intervalDuration = parameters.intervalDuration
            val intervalOffset = parameters.intervalOffset
            val region = parameters.region

            val tz = TimeZone.getTimeZone("UTC")
            val intervalTime = GregorianCalendar(tz)

            val millisecondsPerMinute = 60 * 1000L
            val endTime = System.currentTimeMillis() + intervalOffset * millisecondsPerMinute
            val startTime = endTime - intervalDuration * millisecondsPerMinute
            val intervalSequence = createTimestampSequence(10 * millisecondsPerMinute, max(startTime, latestTime), endTime)

            Authenticator.setDefault(MyAuthenticator())

            val strikes = retrieveData("BlitzortungHttpDataProvider.getStrikes() read %d bytes (%d new strikes) from region $region",
                    intervalSequence.map {
                        intervalTime.timeInMillis = it

                        return@map readFromUrl(Type.STRIKES, region, intervalTime)
                    }) { strikeMapBuilder.buildFromLine(it) }

            if (latestTime > 0L) {
                resultVar = resultVar.copy(updated = strikes.size)
                val expireTime = resultVar.referenceTime - (intervalDuration - intervalOffset) * millisecondsPerMinute
                this@BlitzortungHttpDataProvider.strikes = this@BlitzortungHttpDataProvider.strikes.filter { it.timestamp > expireTime }
                this@BlitzortungHttpDataProvider.strikes += strikes
            } else {
                this@BlitzortungHttpDataProvider.strikes = strikes
            }

            if (strikes.count() > 0) {
                latestTime = endTime - millisecondsPerMinute
                Log.v(Main.LOG_TAG, "BlitzortungHttpDataProvider.getStrikes() set latest time to $latestTime")
            }

            resultVar = resultVar.copy(strikes = this@BlitzortungHttpDataProvider.strikes, referenceTime = endTime)

            return resultVar
        }

        override fun getStrikesGrid(parameters: Parameters, result: ResultEvent): ResultEvent {
            return result
        }

        override fun getStations(region: Int): List<Station> {

            Authenticator.setDefault(MyAuthenticator())

            return retrieveData("BlitzortungHttpDataProvider.getStations() read %d bytes (%d stations) from region $region",
                    sequenceOf(readFromUrl(Type.STATIONS, region))) {
                stationMapBuilder.buildFromLine(it)
            }
        }
    }

    private inner class MyAuthenticator : Authenticator() {
        override fun getPasswordAuthentication(): PasswordAuthentication {
            return PasswordAuthentication(username, password.toCharArray())
        }
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: PreferenceKey) {
        when (key) {
            PreferenceKey.USERNAME -> username = sharedPreferences.get(key, "")

            PreferenceKey.PASSWORD -> password = sharedPreferences.get(key, "")

            else -> {
            }
        }
    }
}
