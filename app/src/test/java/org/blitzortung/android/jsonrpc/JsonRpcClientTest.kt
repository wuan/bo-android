package org.blitzortung.android.jsonrpc

import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.verify
import java.net.URL
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

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
    fun callReturnsResultFromBatch() {
        val response = "[{\"jsonrpc\": \"2.0\", \"id\": 1, \"result\": {\"foo\": \"bar\"}}]"
        every { httpClient.doRequest(any(), any()) } answers { HttpServiceClientResult(response) }

        val baseUrl = URL("http://base.url/")
        val methodName = "<methodName>"
        val result: JsonRpcResponse = uut.call(baseUrl, methodName)

        assertThat(result.id).isEqualTo(1)
        assertThat(result.data.length()).isEqualTo(1)
        assertThat(result.data.get("foo")).isEqualTo("bar")

        val params =
            JSONObject(
                mapOf(
                    Pair("jsonrpc", "2.0"),
                    Pair("id", 1),
                    Pair("method", methodName),
                    Pair("params", JSONArray()),
                ),
            )
        verify { httpClient.doRequest(baseUrl, params.toString()) }

        assertThat(uut.lastNumberOfTransferredBytes).isEqualTo(response.length)
    }

    @Test
    fun callReturnsResult() {
        val response = "{\"jsonrpc\": \"2.0\", \"id\": 1, \"result\": {\"foo\": \"bar\"}}"
        every { httpClient.doRequest(any(), any()) } answers { HttpServiceClientResult(response) }

        val baseUrl = URL("http://base.url/")
        val methodName = "<methodName>"
        val result: JsonRpcResponse = uut.call(baseUrl, methodName)

        assertThat(result.id).isEqualTo(1)
        assertThat(result.data.length()).isEqualTo(1)
        assertThat(result.data.get("foo")).isEqualTo("bar")

        val params =
            JSONObject(
                mapOf(
                    Pair("jsonrpc", "2.0"),
                    Pair("id", 1),
                    Pair("method", methodName),
                    Pair("params", JSONArray()),
                ),
            )
        verify { httpClient.doRequest(baseUrl, params.toString()) }

        assertThat(uut.lastNumberOfTransferredBytes).isEqualTo(response.length)
    }

    @Test
    fun usesIncrementingRequestIds() {
        val response = "{\"jsonrpc\": \"2.0\", \"id\": 1, \"result\": {\"foo\": \"bar\"}}"
        every { httpClient.doRequest(any(), any()) } answers { HttpServiceClientResult(response) }

        val baseUrl = URL("http://base.url/")

        val firstRequest =
            JSONObject(
                mapOf(
                    Pair("jsonrpc", "2.0"),
                    Pair("id", 1),
                    Pair("method", "first"),
                    Pair("params", JSONArray()),
                ),
            )
        val secondRequest =
            JSONObject(
                mapOf(
                    Pair("jsonrpc", "2.0"),
                    Pair("id", 2),
                    Pair("method", "second"),
                    Pair("params", JSONArray()),
                ),
            )

        uut.call(baseUrl, "first")
        uut.call(baseUrl, "second")

        verify { httpClient.doRequest(baseUrl, firstRequest.toString()) }
        verify { httpClient.doRequest(baseUrl, secondRequest.toString()) }
    }

    @Test
    fun handlesRemoteException() {
        every {
            httpClient.doRequest(
                any(),
                any(),
            )
        } answers {
            HttpServiceClientResult(
                "{\"jsonrpc\": \"2.0\", \"id\": 1, \"error\": " +
                    "{\"code\": -32601, \"message\": \"foo\", \"data\": \"\"}}",
            )
        }

        val baseUrl = URL("http://base.url/")
        val methodName = "<methodName>"

        assertThatThrownBy {
            uut.call(baseUrl, methodName)
        }.isInstanceOf(JsonRpcException::class.java).hasMessage("remote Exception 'foo' #-32601")
    }

    @Test
    fun rejectsNonV2Response() {
        every { httpClient.doRequest(any(), any()) } answers { HttpServiceClientResult("[{\"foo\": \"bar\"}]") }

        val baseUrl = URL("http://base.url/")

        assertThatThrownBy {
            uut.call(baseUrl, "<methodName>")
        }.isInstanceOf(JsonRpcException::class.java).hasMessage("unsupported JSON-RPC response version")
    }
}
