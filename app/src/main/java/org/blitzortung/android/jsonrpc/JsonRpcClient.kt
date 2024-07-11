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

import org.json.JSONArray
import org.json.JSONObject
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

data class JsonRpcResponse(
    val data: JSONObject,
)

@Singleton
class JsonRpcClient @Inject constructor(client: HttpServiceClientDefault) : HttpServiceClient by client {

    init {
        connectionTimeout = 40000
        socketTimeout = 40000
    }

    private val id = 0

    var lastNumberOfTransferredBytes: Int = 0
        private set

    private fun buildParameters(parameters: Array<out Any>): JSONArray {
        val parameterArray = JSONArray()
        parameters.forEach { parameterArray.put(it) }
        return parameterArray
    }

    private fun buildRequest(methodName: String, parameters: Array<out Any>): String {
        val requestObject = JSONObject()

        requestObject.put("id", id)
        requestObject.put("method", methodName)
        requestObject.put("params", buildParameters(parameters))

        return requestObject.toString()
    }

    fun call(baseUrl: URL, methodName: String, vararg parameters: Any): JsonRpcResponse {
        val response = doRequest(baseUrl, buildRequest(methodName, parameters))

        lastNumberOfTransferredBytes = response.body.length

        return if (response.body.startsWith("[")) {
            JsonRpcResponse(
                data= JSONArray(response.body).getJSONObject(0),
            )
        } else {
            val responseObject = JSONObject(response.body)

            if (responseObject.has("fault")) {
                throw JsonRpcException(
                    "remote Exception '%s' #%s".format(
                        responseObject.get("faultString"),
                        responseObject.get("faultCode")
                    )
                )
            }
            JsonRpcResponse(
                data= responseObject,
            )
        }
    }
}
