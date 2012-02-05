package org.blitzortung.android.data.provider;

import java.util.ArrayList;
import java.util.List;

import org.alexd.jsonrpc.JSONRPCClient;
import org.blitzortung.android.data.beans.Station;
import org.blitzortung.android.data.beans.Stroke;
import org.json.JSONArray;
import org.json.JSONObject;

public class JsonRpcProvider extends DataProvider {
	
	private Integer nextId = null;
	
	public DataResult<Stroke> getStrokes(int timeInterval) {
		
		DataResult<Stroke> strokesResult = new DataResult<Stroke>();
		
		JSONRPCClient client = JSONRPCClient.create("http://tryb.de:7080/");

		client.setConnectionTimeout(10000);
		client.setSoTimeout(10000);
		
		try {
			JSONObject response = client.callJSONObject("get_strokes", timeInterval, nextId);
			JSONArray strokes_array = (JSONArray)response.get("strokes");
			
			List<Stroke> strokes = new ArrayList<Stroke>();
			
			for (int i = 0; i < strokes_array.length(); i++) {
				strokes.add(new Stroke(strokes_array.getJSONArray(i)));
			}
			strokesResult.setData(strokes);
			
			if (response.has("next")) {
			  nextId = (Integer)response.get("next");
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		
		return strokesResult;
	}

	@Override
	public DataResult<Station> getStations() {
		return new DataResult<Station>();
	}

	@Override
	public ProviderType getType() {
		return ProviderType.RPC;
	}
}
