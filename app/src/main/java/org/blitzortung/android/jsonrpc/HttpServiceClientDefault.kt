package org.blitzortung.android.jsonrpc

import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL

class HttpServiceClientDefault internal constructor(uriString: String, agentSuffix: String) : HttpServiceClient {

    private val url: URL
    private val userAgentString: String
    override var socketTimeout = 0
    override var connectionTimeout = 0

    init {
        url = URI(uriString).toURL()
        userAgentString = "bo-android" + agentSuffix
    }

    override fun shutdown() {
    }

    override fun doRequest(data: String): String {
        val connection = url.openConnection() as HttpURLConnection

        connection.requestMethod = "POST"

        val postDataBytes = data.toByteArray("UTF-8")

        connection.setRequestProperty("Content-Type", "text/json")
        connection.setRequestProperty("Content-Length", postDataBytes.size.toString())
        connection.setRequestProperty("User-Agent", userAgentString)

        connection.doOutput = true
        connection.outputStream.write(postDataBytes)
        connection.connectTimeout = connectionTimeout
        connection.readTimeout = socketTimeout

        return InputStreamReader(connection.inputStream, "UTF-8").use { it.readText() }
    }
}
