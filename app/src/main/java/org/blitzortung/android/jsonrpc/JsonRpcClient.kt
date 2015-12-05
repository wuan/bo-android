package org.blitzortung.android.jsonrpc

import org.json.JSONArray
import org.json.JSONObject

class JsonRpcClient(client: HttpServiceClient) : HttpServiceClient by client {

    constructor(uri: String, agentSuffix: String) : this(HttpServiceClientDefault(uri, agentSuffix)) {
    }

    private val id = 0

    var lastNumberOfTransferredBytes: Int = 0
        private set

    // VisibleForTesting
    protected fun buildParameters(parameters: Array<out Any>): JSONArray {
        val parameterArray = JSONArray()
        parameters.forEach { parameterArray.put(it) }
        return parameterArray
    }

    // VisibleForTesting
    protected fun buildRequest(methodName: String, parameters: Array<out Any>): String {
        val requestObject = JSONObject()

        requestObject.put("id", id)
        requestObject.put("method", methodName)
        requestObject.put("params", buildParameters(parameters))

        return requestObject.toString()
    }

    fun call(methodName: String, vararg parameters: Any): JSONObject {
        val response = doRequest(buildRequest(methodName, parameters))

        lastNumberOfTransferredBytes = response.length

        if (response.startsWith("[")) {
            return JSONArray(response).getJSONObject(0)
        } else {
            val responseObject = JSONObject(response)

            if (responseObject.has("fault")) {
                throw JsonRpcException("remote Exception '%s' #%s ".format(responseObject.get("faultString"), responseObject.get("faultCode")))
            }
            return responseObject
        }
    }
}
