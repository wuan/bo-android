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

package org.blitzortung.android.jsonrpc

import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.Charset
import java.util.zip.GZIPInputStream
import javax.inject.Inject
import javax.inject.Named

class HttpServiceClientDefault @Inject constructor(
        @Named("agentSuffix") agentSuffix: String
) : HttpServiceClient {

    private val userAgentString: String = "bo-android$agentSuffix"
    override var socketTimeout = 0
    override var connectionTimeout = 0

    override fun shutdown() {
    }

    override fun doRequest(baseUrl: URL, data: String): String {
        val connection = baseUrl.openConnection() as HttpURLConnection

        connection.requestMethod = "POST"

        val postDataBytes = data.toByteArray(Charset.forName("UTF-8"))

        connection.setRequestProperty("Content-Type", "text/json")
        connection.setRequestProperty("Content-Length", postDataBytes.size.toString())
        connection.setRequestProperty("User-Agent", userAgentString)
        connection.setRequestProperty("Accept-Encoding", "gzip")

        connection.doOutput = true
        connection.outputStream.write(postDataBytes)
        connection.connectTimeout = connectionTimeout
        connection.readTimeout = socketTimeout

        var inputStream = connection.inputStream

        when (connection.contentEncoding) {
            "gzip" -> {
                inputStream = GZIPInputStream(inputStream)
            }
        }

        return InputStreamReader(inputStream, "UTF-8").use { it.readText() }
    }
}
