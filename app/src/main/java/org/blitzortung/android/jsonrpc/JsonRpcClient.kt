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

import java.net.URL
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import javax.inject.Singleton
import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONTokener

data class JsonRpcResponse(
    val data: JSONObject,
    val id: Int? = null,
)

@Singleton
class JsonRpcClient
    @Inject
    constructor(client: HttpServiceClientDefault) : HttpServiceClient by client {
        init {
            connectionTimeout = 40000
            socketTimeout = 40000
        }

        private val nextRequestId = AtomicInteger(1)

        var lastNumberOfTransferredBytes: Int = 0
            private set

        private fun buildParameters(parameters: Array<out Any>): JSONArray {
            val parameterArray = JSONArray()
            parameters.forEach { parameterArray.put(it) }
            return parameterArray
        }

        private fun buildRequest(
            id: Int,
            methodName: String,
            parameters: Array<out Any>,
        ): String {
            val requestObject = JSONObject()

            requestObject.put("jsonrpc", JSON_RPC_VERSION)
            requestObject.put("id", id)
            requestObject.put("method", methodName)
            requestObject.put("params", buildParameters(parameters))

            return requestObject.toString()
        }

        fun call(
            baseUrl: URL,
            methodName: String,
            vararg parameters: Any,
        ): JsonRpcResponse {
            val requestId = nextRequestId.getAndIncrement()
            val response = doRequest(baseUrl, buildRequest(requestId, methodName, parameters))

            lastNumberOfTransferredBytes = response.body.length

            return parseResponse(response.body, requestId)
        }

        private fun parseResponse(
            body: String,
            requestId: Int,
        ): JsonRpcResponse {
            val parsed = JSONTokener(body).nextValue()

            val responseObject =
                when (parsed) {
                    is JSONObject -> parsed
                    is JSONArray -> selectBatchResponse(parsed, requestId)
                    else -> throw JsonRpcException("invalid JSON-RPC response")
                }

            if (responseObject.optString("jsonrpc") != JSON_RPC_VERSION) {
                throw JsonRpcException("unsupported JSON-RPC response version")
            }

            val error = responseObject.opt("error")
            if (error != null && error != JSONObject.NULL) {
                throw createException(error)
            }

            if (!responseObject.has("result")) {
                throw JsonRpcException("invalid JSON-RPC response: missing result")
            }

            val result = responseObject.get("result")
            val data =
                result as? JSONObject
                    ?: throw JsonRpcException("invalid JSON-RPC response result")

            return JsonRpcResponse(data = data, id = responseObject.optInt("id", requestId))
        }

        private fun selectBatchResponse(
            array: JSONArray,
            requestId: Int,
        ): JSONObject {
            for (i in 0 until array.length()) {
                val candidate = array.optJSONObject(i) ?: continue
                if (candidate.optInt("id", Int.MIN_VALUE) == requestId) {
                    return candidate
                }
            }
            return array.optJSONObject(0)
                ?: throw JsonRpcException("invalid JSON-RPC batch response")
        }

        private fun createException(error: Any): JsonRpcException {
            val errorObject = error as? JSONObject
                ?: return JsonRpcException("remote Exception: $error")

            return JsonRpcException(
                "remote Exception '%s' #%s".format(
                    errorObject.optString("message"),
                    errorObject.optInt("code"),
                ),
            )
        }

        companion object {
            const val JSON_RPC_VERSION = "2.0"
        }
    }
