package org.blitzortung.android.jsonrpc

import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.net.URL

@RunWith(RobolectricTestRunner::class)
class JsonRpcClientTest {
    @MockK
    private lateinit var httpClient: HttpServiceClientDefault

    private lateinit var uut: JsonRpcClient

    @Before
    fun setUp() {
        MockKAnnotations.init(this, relaxed = true)

        uut = JsonRpcClient(httpClient)
    }

    @Test
    fun callReturnsArray() {
        val response = "[{\"foo\": \"bar\"}]"
        every { httpClient.doRequest(any(), any()) } answers { HttpServiceClientResult(response) }

        val baseUrl = URL("http://base.url/")
        val methodName = "<methodName>"
        val result: JsonRpcResponse = uut.call(baseUrl, methodName)

        assertThat(result.data.length()).isEqualTo(1)
        assertThat(result.data.get("foo")).isEqualTo("bar")

        val params = JSONObject(mapOf(Pair("id", 0), Pair("method", methodName), Pair("params", JSONArray())))
        verify { httpClient.doRequest(baseUrl, params.toString()) }

        assertThat(uut.lastNumberOfTransferredBytes).isEqualTo(16)
    }

    @Test
    fun callReturnsObject() {
        val response = "{\"foo\": \"bar\"}"
        every { httpClient.doRequest(any(), any()) } answers { HttpServiceClientResult(response) }

        val baseUrl = URL("http://base.url/")
        val methodName = "<methodName>"
        val result: JsonRpcResponse = uut.call(baseUrl, methodName)

        assertThat(result.data.length()).isEqualTo(1)
        assertThat(result.data.get("foo")).isEqualTo("bar")

        val params = JSONObject(mapOf(Pair("id", 0), Pair("method", methodName), Pair("params", JSONArray())))
        verify { httpClient.doRequest(baseUrl, params.toString()) }

        assertThat(uut.lastNumberOfTransferredBytes).isEqualTo(response.length)
    }

    @Test
    fun handlesRemoteException() {
        every {
            httpClient.doRequest(
                any(),
                any()
            )
        } answers { HttpServiceClientResult("{\"fault\":true,\"faultString\": \"foo\",\"faultCode\":\"1234\"}") }

        val baseUrl = URL("http://base.url/")
        val methodName = "<methodName>"

        assertThatThrownBy {
            uut.call(baseUrl, methodName)
        }.isInstanceOf(JsonRpcException::class.java).hasMessage("remote Exception 'foo' #1234")
    }
}
