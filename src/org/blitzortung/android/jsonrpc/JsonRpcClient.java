package org.blitzortung.android.jsonrpc;

import java.io.UnsupportedEncodingException;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class JsonRpcClient extends HttpServiceClient {

	private int id;

	private JSONArray buildParameters(Object[] parameters) {
		JSONArray parameterArray = new JSONArray();
		for (Object parameter : parameters) {
			parameterArray.put(parameter);
		}
		return parameterArray;
	}

	private JsonRequestEntity buildRequest(String methodName, Object[] parameters) {
		JSONObject requestObject = new JSONObject();
		try {
			requestObject.put("id", id);
			requestObject.put("method", methodName);
			requestObject.put("params", buildParameters(parameters));
		} catch (JSONException e) {
			throw new JsonRpcException("invalid JSON request", e);
		}

		JsonRequestEntity jsonRequestEntity;
		try {
			jsonRequestEntity = new JsonRequestEntity(requestObject);
		} catch (UnsupportedEncodingException e) {
			throw new JsonRpcException("unable to create entity", e);
		}
		return jsonRequestEntity;
	}

	public JsonRpcClient(String uri) {
		super(uri);
	}

	public JSONObject call(String methodName, Object... parameters) {
		String response = doRequest(buildRequest(methodName, parameters));

		try {
			if (response.startsWith("[")) {

				JSONArray responseArray = new JSONArray(response);
				return responseArray.getJSONObject(0);
			} else {

				JSONObject responseObject = new JSONObject(response);

				if (responseObject.has("fault")) {
					throw new JsonRpcException(String.format("remote Exception '%s' #%d ", responseObject.get("faultString"),
							responseObject.get("faultCode")));
				}
				return responseObject;
			}
		} catch (JSONException e) {
			throw new JsonRpcException("response not in JSON format", e);
		}
	}

}
