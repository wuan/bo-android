package org.blitzortung.android.data.provider;

import java.util.ArrayList;
import java.util.List;

import org.alexd.jsonrpc.JSONRPCClient;
import org.alexd.jsonrpc.JSONRPCException;
import org.blitzortung.android.data.Credentials;
import org.blitzortung.android.data.beans.Station;
import org.blitzortung.android.data.beans.Stroke;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class JsonRpcProvider implements DataProvider {

	private final static String TAG = "provider.JsonRpcProvider";
	
	private Credentials creds;
	
	private Integer nextId = null;
	
	public JsonRpcProvider(Credentials creds) {
		this.creds = creds;
	}
	
	public List<Stroke> getStrokes() {
		
		List<Stroke> strokes = new ArrayList<Stroke>();
		
		JSONRPCClient client = JSONRPCClient.create("http://tryb.de:7080/");

		client.setConnectionTimeout(2000);
		client.setSoTimeout(2000);

		Integer start = -60;
		
		if (nextId != null)
			start = nextId;
		
		try {
			JSONObject response = client.callJSONObject("get_strokes", start);
			
			JSONArray strokes_array = (JSONArray)response.get("strokes");
			for (int i = 0; i < strokes_array.length(); i++) {
				strokes.add(new Stroke(strokes_array.getJSONArray(i)));
			}
			if (response.has("next")) {
			  nextId = (Integer)response.get("next");
			}
			
		} catch (JSONRPCException e) {
			e.printStackTrace();
		} catch (JSONException e) {
			e.printStackTrace();
		}
		
		return strokes;
	}

	@Override
	public List<Station> getStations() {
		// TODO Auto-generated method stub
		return null;
	}
}
