package org.blitzortung.android.data.provider.standard

import android.content.Context
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import org.assertj.core.api.Assertions.assertThat
import org.blitzortung.android.app.view.PreferenceKey
import org.blitzortung.android.app.view.put
import org.blitzortung.android.data.Flags
import org.blitzortung.android.data.History
import org.blitzortung.android.data.LocalReference
import org.blitzortung.android.data.Parameters
import org.blitzortung.android.data.TimeInterval
import org.blitzortung.android.data.beans.GridElement
import org.blitzortung.android.data.provider.GLOBAL_REGION
import org.blitzortung.android.data.provider.LOCAL_REGION
import org.blitzortung.android.data.provider.result.ResultEvent
import org.blitzortung.android.jsonrpc.JsonRpcClient
import org.blitzortung.android.jsonrpc.JsonRpcResponse
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import java.net.URL

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
        edit.put(PreferenceKey.SERVICE_URL, SERVICE_URL);
        edit.apply()

        uut = JsonRpcDataProvider(preferences, client)
    }


    @Test
    fun getsGlobalData() {
        val parameters = Parameters(
            region = GLOBAL_REGION,
            interval = TimeInterval(
                offset = 30,
                duration = 60
            ),
            countThreshold = 5,
            gridSize = 5000
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
                parameters.countThreshold
            )
        } returns JsonRpcResponse(response)

        val result: ResultEvent = uut.retrieveData { getStrikesGrid(parameters, history, flags) }

        assertThat(result.gridParameters?.latitudeStart).isEqualTo(0.0)
        assertThat(result.gridParameters?.longitudeStart).isEqualTo(0.0)
        assertThat(result.gridParameters?.size).isEqualTo(5000)
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
        val localReference = LocalReference(5,6)
        val parameters = Parameters(
            region = LOCAL_REGION,
            interval = TimeInterval(
                offset = 30,
                duration = 60
            ),
            countThreshold = 5,
            gridSize = 5000,
            localReference = localReference
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
                localReference.x,
                localReference.y,
                parameters.gridSize,
                parameters.intervalDuration,
                parameters.intervalOffset,
                parameters.countThreshold
            )
        } returns JsonRpcResponse(response)

        val result: ResultEvent = uut.retrieveData { getStrikesGrid(parameters, history, flags) }

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
        val localReference = LocalReference(5,6)
        val parameters = Parameters(
            region = 2,
            interval = TimeInterval(
                offset = 30,
                duration = 60
            ),
            countThreshold = 5,
            gridSize = 5000,
            localReference = localReference
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
                parameters.countThreshold
            )
        } returns JsonRpcResponse(response)

        val result: ResultEvent = uut.retrieveData { getStrikesGrid(parameters, history, flags) }

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

    private fun createResponse(): JSONObject {
        val response = JSONObject()
        response.put("t", "20230326T18:49:34")
        response.put("xd", "15")
        response.put("yd", "30")
        response.put("xc", "24")
        response.put("yc", "24")
        val strike1 = JSONArray(listOf(2, 0, 5, 10));
        val strike2 = JSONArray(listOf(1, -1, 9, 20));
        val strikesArray = JSONArray(listOf(strike1, strike2))
        response.put("r", strikesArray)
        return response
    }
}