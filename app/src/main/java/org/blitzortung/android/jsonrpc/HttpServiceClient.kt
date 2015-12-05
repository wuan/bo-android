package org.blitzortung.android.jsonrpc

interface HttpServiceClient {
    open fun shutdown()
    open fun doRequest(data: String): String
    var socketTimeout: Int
    var connectionTimeout: Int
}

