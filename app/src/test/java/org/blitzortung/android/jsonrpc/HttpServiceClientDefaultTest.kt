package org.blitzortung.android.jsonrpc

import io.mockk.MockKAnnotations
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.net.*
import java.util.zip.GZIPOutputStream


class HttpServiceClientDefaultTest {
    private lateinit var uut: HttpServiceClientDefault

    companion object {
        val handler = MockURLStreamHandler()
        @BeforeClass
        @JvmStatic
        fun setUpClass() {
            URL.setURLStreamHandlerFactory(handler)
        }
    }

    @Before
    fun setUp() {
        MockKAnnotations.init(this, relaxed = true)

        uut = HttpServiceClientDefault("-foo")

        handler.responseHeaders.clear()
    }

    @Test
    fun getsPlainData() {
        val data = "output"
        val responseValue = "input"
        handler.response = responseValue.toByteArray()
        val response = uut.doRequest(URL("http://base.url"), data)

        assertThat(handler.connection.headers).containsExactlyInAnyOrderEntriesOf(
            mutableMapOf(
                Pair("Content-Type", "text/json"),
                Pair("Content-Length", "${data.length}"),
                Pair("User-Agent", "bo-android-foo"),
                Pair("Accept-Encoding", "gzip")
            )
        )

        assertThat(handler.connection.outputStream.toString("UTF-8")).isEqualTo(data)
        assertThat(response).isEqualTo(responseValue)
    }

    @Test
    fun getsGzippedData() {
        val data = "output"
        val responseValue = "input"

        val obj = ByteArrayOutputStream()
        val gzip = GZIPOutputStream(obj)
        gzip.write(responseValue.toByteArray());
        gzip.close()

        handler.response = obj.toByteArray()
        handler.responseHeaders["content-encoding"] = "gzip"
        val response = uut.doRequest(URL("http://base.url"), data)

        assertThat(handler.connection.outputStream.toString("UTF-8")).isEqualTo(data)
        assertThat(response).isEqualTo(responseValue)
    }
}

class MockURLStreamHandler : URLStreamHandler(), URLStreamHandlerFactory {
    var response: ByteArray = ByteArray(0)
    var responseHeaders = mutableMapOf<String, String>()

    lateinit var connection: MockHttpURLConnection
        private set

    // *** URLStreamHandler
    @Throws(IOException::class)
    protected override fun openConnection(u: URL?): URLConnection {
        connection = MockHttpURLConnection(u, responseHeaders) { this.response }
        return connection
    }

    // *** URLStreamHandlerFactory
    override fun createURLStreamHandler(protocol: String?): URLStreamHandler {
        return this
    }
}

class MockHttpURLConnection(
    val url: URL?,
    private val responseHeaders: MutableMap<String, String>,
    val responseSupplier: () -> ByteArray,

    ) : HttpURLConnection(url) {

    val outputStream = ByteArrayOutputStream()

    val headers = mutableMapOf<String?, String?>()

    @Throws(IOException::class)
    override fun getInputStream(): InputStream {
        return responseSupplier().inputStream()
    }

    @Throws(IOException::class)
    override fun connect() {
    }

    override fun disconnect() {}
    override fun usingProxy(): Boolean {
        return false
    }

    override fun getOutputStream(): OutputStream {
        return outputStream
    }

    override fun setRequestProperty(key: String?, value: String?) {
        headers.put(key, value);
    }

    override fun getHeaderField(name: String?): String? {
        return responseHeaders.get(name)
    }
}