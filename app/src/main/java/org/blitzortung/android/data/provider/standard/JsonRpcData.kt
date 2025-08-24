package org.blitzortung.android.data.provider.standard

import org.blitzortung.android.data.Parameters
import org.blitzortung.android.data.provider.GLOBAL_REGION
import org.blitzortung.android.data.provider.LOCAL_REGION
import org.blitzortung.android.data.provider.LOCAL_REGION_THRESHOLD
import org.blitzortung.android.jsonrpc.JsonRpcClient
import org.blitzortung.android.jsonrpc.JsonRpcResponse
import java.net.URL
import kotlin.math.max

class JsonRpcData(
    private val client: JsonRpcClient,
    private val serviceUrl: URL,
) {

    fun requestData(parameters: Parameters): JsonRpcResponse {
        val intervalDuration = parameters.intervalDuration
        val intervalOffset = parameters.intervalOffset
        val gridSize = parameters.gridSize
        val countThreshold = parameters.countThreshold
        val region = parameters.region
        val localReference = parameters.dataArea

        return when (region) {
            GLOBAL_REGION -> {
                val jsonRpcResponse: JsonRpcResponse = client.call(
                    serviceUrl,
                    "get_global_strikes_grid",
                    intervalDuration,
                    max(gridSize, LOCAL_REGION_THRESHOLD),
                    intervalOffset,
                    countThreshold
                )
                with(
                    jsonRpcResponse
                ) {
                    data.put("y1", 0.0)
                    data.put("x0", 0.0)
                }
                jsonRpcResponse
            }

            LOCAL_REGION -> {
                client.call(
                    serviceUrl,
                    "get_local_strikes_grid",
                    localReference!!.x,
                    localReference.y,
                    gridSize,
                    intervalDuration,
                    intervalOffset,
                    countThreshold,
                    localReference.scale,
                )
            }

            else -> {
                client.call(
                    serviceUrl,
                    "get_strikes_grid",
                    intervalDuration,
                    gridSize,
                    intervalOffset,
                    region,
                    countThreshold
                )
            }
        }

    }
}
