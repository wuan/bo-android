package org.blitzortung.android.jsonrpc;

import org.apache.http.Header;
import org.apache.http.entity.StringEntity;
import org.apache.http.message.BasicHeader;
import org.apache.http.protocol.HTTP;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;

public class JsonRequestEntity extends StringEntity {

	public JsonRequestEntity(JSONObject jsonObject) throws UnsupportedEncodingException {
		super(jsonObject.toString());
	}

	@Override
	public Header getContentType() {
		return new BasicHeader(HTTP.CONTENT_TYPE, "text/json");
	}

}
