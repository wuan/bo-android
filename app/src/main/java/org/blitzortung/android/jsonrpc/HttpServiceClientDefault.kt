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
