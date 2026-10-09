package org.blitzortung.android.data.provider.standard

import android.content.Context
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import java.net.URL
import org.assertj.core.api.Assertions.assertThat
import org.blitzortung.android.app.view.PreferenceKey
import org.blitzortung.android.app.view.put
import org.blitzortung.android.data.DataArea
import org.blitzortung.android.data.Flags
import org.blitzortung.android.data.History
import org.blitzortung.android.data.Parameters
import org.blitzortung.android.data.TimeInterval
import org.blitzortung.android.data.beans.GridElement
import org.blitzortung.android.data.provider.GLOBAL_REGION
import org.blitzortung.android.data.provider.LOCAL_REGION
import org.blitzortung.android.data.provider.result.DataReceived
import org.blitzortung.android.jsonrpc.JsonRpcClient
import org.blitzortung.android.jsonrpc.JsonRpcResponse
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

private const val SERVICE_URL = "http://service.url/"

@RunWith(RobolectricTestRunner::class)
class JsonRpcDataProviderTest {
    private lateinit var uut: JsonRpcDataProvider

    @MockK
    private lateinit var client: JsonRpcClient

    @Before
    fun setUp() {
        MockKAnnotations.init(this, relaxed = true)

        val context = RuntimeEnvironment.getApplication()
        val preferences = context.getSharedPreferences(context.packageName, Context.MODE_PRIVATE)

        val edit = preferences.edit()
        edit.put(PreferenceKey.SERVICE_URL, SERVICE_URL)
        edit.apply()

        uut = JsonRpcDataProvider(preferences, client)
    }

    @Test
    fun getsGlobalData() {
        val parameters =
            Parameters(
                region = GLOBAL_REGION,
                interval =
                    TimeInterval(
                        offset = 30,
                        duration = 60,
                    ),
                countThreshold = 5,
                gridSize = 25000,
            )
        val history = History()
        val flags = Flags()

        val response = createResponse()

        every {
            client.call(
                URL(SERVICE_URL),
                "get_global_strikes_grid",
                parameters.intervalDuration,
                parameters.gridSize,
                parameters.intervalOffset,
                parameters.countThreshold,
            )
        } returns JsonRpcResponse(response)

        val result: DataReceived = uut.retrieveData { getStrikesGrid(parameters, history, flags) }

        assertThat(result.gridParameters?.latitudeStart).isEqualTo(0.0)
        assertThat(result.gridParameters?.longitudeStart).isEqualTo(0.0)
        assertThat(result.gridParameters?.size).isEqualTo(25000)
        assertThat(result.gridParameters?.latitudeBins).isEqualTo(24)
        assertThat(result.gridParameters?.longitudeBins).isEqualTo(24)
        assertThat(result.gridParameters?.latitudeDelta).isEqualTo(30.0)
        assertThat(result.gridParameters?.longitudeDelta).isEqualTo(15.0)

        assertThat(result.strikes).containsExactly(
            GridElement(timestamp = 1679856584000L, longitude = 37.5, latitude = -15.0, multiplicity = 5),
            GridElement(timestamp = 1679856594000L, longitude = 22.5, latitude = 15.0, multiplicity = 9),
        )
    }

    @Test
    fun getsLocalData() {
        val dataArea = DataArea(5, 6, 5)
        val parameters =
            Parameters(
                region = LOCAL_REGION,
                interval =
                    TimeInterval(
                        offset = 30,
                        duration = 60,
                    ),
                countThreshold = 5,
                gridSize = 5000,
                dataArea = dataArea,
            )
        val history = History()
        val flags = Flags()

        val response = createResponse()
        response.put("x0", "10")
        response.put("y1", "15")

        every {
            client.call(
                URL(SERVICE_URL),
                "get_local_strikes_grid",
                dataArea.x,
                dataArea.y,
                parameters.gridSize,
                parameters.intervalDuration,
                parameters.intervalOffset,
                parameters.countThreshold,
                dataArea.scale,
            )
        } returns JsonRpcResponse(response)

        val result: DataReceived = uut.retrieveData { getStrikesGrid(parameters, history, flags) }

        assertThat(result.gridParameters?.latitudeStart).isEqualTo(15.0)
        assertThat(result.gridParameters?.longitudeStart).isEqualTo(10.0)
        assertThat(result.gridParameters?.size).isEqualTo(5000)
        assertThat(result.gridParameters?.latitudeBins).isEqualTo(24)
        assertThat(result.gridParameters?.longitudeBins).isEqualTo(24)
        assertThat(result.gridParameters?.latitudeDelta).isEqualTo(30.0)
        assertThat(result.gridParameters?.longitudeDelta).isEqualTo(15.0)

        assertThat(result.strikes).containsExactly(
            GridElement(timestamp = 1679856584000L, longitude = 47.5, latitude = 0.0, multiplicity = 5),
            GridElement(timestamp = 1679856594000L, longitude = 32.5, latitude = 30.0, multiplicity = 9),
        )
    }

    @Test
    fun getsRegionData() {
        val dataArea = DataArea(5, 6, 5)
        val parameters =
            Parameters(
                region = 2,
                interval =
                    TimeInterval(
                        offset = 30,
                        duration = 60,
                    ),
                countThreshold = 5,
                gridSize = 5000,
                dataArea = dataArea,
            )
        val history = History()
        val flags = Flags()

        val response = createResponse()
        response.put("x0", "10")
        response.put("y1", "15")

        every {
            client.call(
                URL(SERVICE_URL),
                "get_strikes_grid",
                parameters.intervalDuration,
                parameters.gridSize,
                parameters.intervalOffset,
                parameters.region,
                parameters.countThreshold,
            )
        } returns JsonRpcResponse(response)

        val result: DataReceived = uut.retrieveData { getStrikesGrid(parameters, history, flags) }

        assertThat(result.gridParameters?.latitudeStart).isEqualTo(15.0)
        assertThat(result.gridParameters?.longitudeStart).isEqualTo(10.0)
        assertThat(result.gridParameters?.size).isEqualTo(5000)
        assertThat(result.gridParameters?.latitudeBins).isEqualTo(24)
        assertThat(result.gridParameters?.longitudeBins).isEqualTo(24)
        assertThat(result.gridParameters?.latitudeDelta).isEqualTo(30.0)
        assertThat(result.gridParameters?.longitudeDelta).isEqualTo(15.0)

        assertThat(result.strikes).containsExactly(
            GridElement(timestamp = 1679856584000L, longitude = 47.5, latitude = 0.0, multiplicity = 5),
            GridElement(timestamp = 1679856594000L, longitude = 32.5, latitude = 30.0, multiplicity = 9),
        )
    }

    @Test
    fun getsStrikes() {
        val parameters =
            Parameters(
                region = LOCAL_REGION,
                interval =
                    TimeInterval(
                        offset = -1,
                        duration = 60,
                    ),
                countThreshold = 5,
                gridSize = 5000,
                dataArea = DataArea(5, 6, 5),
            )
        val history = History()
        val flags = Flags()

        val response = JSONObject()
        response.put("t", "20230326T18:49:34")
        response.put("s", JSONArray())
        response.put("h", JSONArray(listOf(1, 2, 3)))

        every {
            client.call(
                URL(SERVICE_URL),
                "get_strikes",
                parameters.intervalDuration,
                parameters.intervalOffset,
            )
        } returns JsonRpcResponse(response)

        val result: DataReceived = uut.retrieveData { getStrikes(parameters, history, flags) }

        assertThat(result.strikes).isEmpty()
    }

    @Test
    fun getStrikesGridWrapsIOException() {
        val parameters = Parameters(region = GLOBAL_REGION, gridSize = 25000)

        every {
            client.call(
                URL(SERVICE_URL),
                "get_global_strikes_grid",
                parameters.intervalDuration,
                parameters.gridSize,
                parameters.intervalOffset,
                parameters.countThreshold,
            )
        } throws java.io.IOException("boom")

        val failure =
            runCatching {
                uut.retrieveData { getStrikesGrid(parameters, History(), Flags()) }
            }

        assertThat(failure.exceptionOrNull())
            .isInstanceOf(org.blitzortung.android.data.provider.DataProviderException::class.java)
    }

    @Test
    fun getStrikesGridWrapsJSONException() {
        val parameters = Parameters(region = GLOBAL_REGION, gridSize = 25000)

        val response = JSONObject()
        response.put("t", "20230326T18:49:34")
        // "r" missing -> JSONException while parsing the grid data

        every {
            client.call(
                URL(SERVICE_URL),
                "get_global_strikes_grid",
                parameters.intervalDuration,
                parameters.gridSize,
                parameters.intervalOffset,
                parameters.countThreshold,
            )
        } returns JsonRpcResponse(response)

        val failure =
            runCatching {
                uut.retrieveData { getStrikesGrid(parameters, History(), Flags()) }
            }

        assertThat(failure.exceptionOrNull())
            .isInstanceOf(org.blitzortung.android.data.provider.DataProviderException::class.java)
    }

    @Test
    fun getStrikesWrapsIOException() {
        val parameters = Parameters(region = LOCAL_REGION, dataArea = DataArea(5, 6, 5), gridSize = 5000)

        every {
            client.call(
                URL(SERVICE_URL),
                "get_strikes",
                parameters.intervalDuration,
                parameters.intervalOffset,
            )
        } throws java.io.IOException("boom")

        val failure =
            runCatching {
                uut.retrieveData { getStrikes(parameters, History(), Flags()) }
            }

        assertThat(failure.exceptionOrNull())
            .isInstanceOf(org.blitzortung.android.data.provider.DataProviderException::class.java)
    }

    @Test
    fun getStrikesWrapsJSONException() {
        val parameters = Parameters(region = LOCAL_REGION, dataArea = DataArea(5, 6, 5), gridSize = 5000)

        val response = JSONObject()
        response.put("t", "20230326T18:49:34")
        // "s" missing -> JSONException while parsing the strikes

        every {
            client.call(
                URL(SERVICE_URL),
                "get_strikes",
                parameters.intervalDuration,
                parameters.intervalOffset,
            )
        } returns JsonRpcResponse(response)

        val failure =
            runCatching {
                uut.retrieveData { getStrikes(parameters, History(), Flags()) }
            }

        assertThat(failure.exceptionOrNull())
            .isInstanceOf(org.blitzortung.android.data.provider.DataProviderException::class.java)
    }

    @Test
    fun invalidServiceUrlFallsBackToDefault() {
        val context = RuntimeEnvironment.getApplication()
        val preferences = context.getSharedPreferences("invalid-url-prefs", Context.MODE_PRIVATE)
        val edit = preferences.edit()
        edit.put(PreferenceKey.SERVICE_URL, "not a url")
        edit.apply()

        val provider = JsonRpcDataProvider(preferences, client)

        val parameters = Parameters(region = GLOBAL_REGION, gridSize = 25000)
        every {
            client.call(
                any<URL>(),
                "get_global_strikes_grid",
                parameters.intervalDuration,
                parameters.gridSize,
                parameters.intervalOffset,
                parameters.countThreshold,
            )
        } returns JsonRpcResponse(createResponse())

        val result: DataReceived = provider.retrieveData { getStrikesGrid(parameters, History(), Flags()) }

        assertThat(result).isNotNull
    }

    private fun createResponse(): JSONObject {
        val response = JSONObject()
        response.put("t", "20230326T18:49:34")
        response.put("xd", "15")
        response.put("yd", "30")
        response.put("xc", "24")
        response.put("yc", "24")
        val strike1 = JSONArray(listOf(2, 0, 5, 10))
        val strike2 = JSONArray(listOf(1, -1, 9, 20))
        val strikesArray = JSONArray(listOf(strike1, strike2))
        response.put("r", strikesArray)
        return response
    }
}
