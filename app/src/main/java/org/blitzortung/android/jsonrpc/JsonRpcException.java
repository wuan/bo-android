package org.blitzortung.android.jsonrpc;


public class JsonRpcException extends RuntimeException {

    private static final long serialVersionUID = -8108432807261988215L;

    public JsonRpcException(String msg, Exception e) {
        super(msg, e);
    }

    public JsonRpcException(String msg) {
        super(msg);
    }
}
