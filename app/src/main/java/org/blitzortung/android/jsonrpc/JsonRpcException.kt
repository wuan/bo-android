package org.blitzortung.android.jsonrpc


class JsonRpcException : RuntimeException {

    constructor(msg: String, e: Exception) : super(msg, e) {
    }

    constructor(msg: String) : super(msg) {
    }

    companion object {
        private val serialVersionUID = -8108432807261988215L
    }
}
