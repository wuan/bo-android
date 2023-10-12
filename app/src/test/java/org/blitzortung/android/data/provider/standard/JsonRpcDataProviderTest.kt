package org.blitzortung.android.data.provider.standard

import android.content.Context
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import org.assertj.core.api.Assertions.assertThat
import org.blitzortung.android.app.view.PreferenceKey
import org.blitzortung.android.app.view.put
import org.blitzortung.android.data.Flags
import org.blitzortung.android.data.LocalReference
import org.blitzortung.android.data.Parameters
import org.blitzortung.android.data.TimeInterval
import org.blitzortung.android.data.beans.RasterElement
import org.blitzortung.android.data.provider.GLOBAL_REGION
import org.blitzortung.android.data.provider.LOCAL_REGION
import org.blitzortung.android.data.provider.result.ResultEvent
import org.blitzortung.android.jsonrpc.JsonRpcClient
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
            rasterBaselength = 5000
        )
        val flags = Flags()

        val response = createResponse()

        every {
            client.call(
                URL(SERVICE_URL),
                "get_global_strikes_grid",
                parameters.intervalDuration,
                parameters.rasterBaselength,
                parameters.intervalOffset,
                parameters.countThreshold
            )
        } returns response

        val result: ResultEvent = uut.retrieveData { getStrikesGrid(parameters, flags) }

        assertThat(result.rasterParameters?.latitudeStart).isEqualTo(0.0)
        assertThat(result.rasterParameters?.longitudeStart).isEqualTo(0.0)
        assertThat(result.rasterParameters?.baselength).isEqualTo(5000)
        assertThat(result.rasterParameters?.latitudeBins).isEqualTo(24)
        assertThat(result.rasterParameters?.longitudeBins).isEqualTo(24)
        assertThat(result.rasterParameters?.latitudeDelta).isEqualTo(30.0)
        assertThat(result.rasterParameters?.longitudeDelta).isEqualTo(15.0)

        assertThat(result.strikes).containsExactly(
            RasterElement(timestamp = 1679856584000L, longitude = 37.5, latitude = -15.0, multiplicity = 5),
            RasterElement(timestamp = 1679856594000L, longitude = 22.5, latitude = 15.0, multiplicity = 9),
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
            rasterBaselength = 5000,
            localReference = localReference
        )
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
                parameters.rasterBaselength,
                parameters.intervalDuration,
                parameters.intervalOffset,
                parameters.countThreshold
            )
        } returns response

        val result: ResultEvent = uut.retrieveData { getStrikesGrid(parameters, flags) }

        assertThat(result.rasterParameters?.latitudeStart).isEqualTo(15.0)
        assertThat(result.rasterParameters?.longitudeStart).isEqualTo(10.0)
        assertThat(result.rasterParameters?.baselength).isEqualTo(5000)
        assertThat(result.rasterParameters?.latitudeBins).isEqualTo(24)
        assertThat(result.rasterParameters?.longitudeBins).isEqualTo(24)
        assertThat(result.rasterParameters?.latitudeDelta).isEqualTo(30.0)
        assertThat(result.rasterParameters?.longitudeDelta).isEqualTo(15.0)

        assertThat(result.strikes).containsExactly(
            RasterElement(timestamp = 1679856584000L, longitude = 47.5, latitude = 0.0, multiplicity = 5),
            RasterElement(timestamp = 1679856594000L, longitude = 32.5, latitude = 30.0, multiplicity = 9),
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
            rasterBaselength = 5000,
            localReference = localReference
        )
        val flags = Flags()

        val response = createResponse()
        response.put("x0", "10")
        response.put("y1", "15")

        every {
            client.call(
                URL(SERVICE_URL),
                "get_strikes_grid",
                parameters.intervalDuration,
                parameters.rasterBaselength,
                parameters.intervalOffset,
                parameters.region,
                parameters.countThreshold
            )
        } returns response

        val result: ResultEvent = uut.retrieveData { getStrikesGrid(parameters, flags) }

        assertThat(result.rasterParameters?.latitudeStart).isEqualTo(15.0)
        assertThat(result.rasterParameters?.longitudeStart).isEqualTo(10.0)
        assertThat(result.rasterParameters?.baselength).isEqualTo(5000)
        assertThat(result.rasterParameters?.latitudeBins).isEqualTo(24)
        assertThat(result.rasterParameters?.longitudeBins).isEqualTo(24)
        assertThat(result.rasterParameters?.latitudeDelta).isEqualTo(30.0)
        assertThat(result.rasterParameters?.longitudeDelta).isEqualTo(15.0)

        assertThat(result.strikes).containsExactly(
            RasterElement(timestamp = 1679856584000L, longitude = 47.5, latitude = 0.0, multiplicity = 5),
            RasterElement(timestamp = 1679856594000L, longitude = 32.5, latitude = 30.0, multiplicity = 9),
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