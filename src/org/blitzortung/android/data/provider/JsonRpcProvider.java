package org.blitzortung.android.data.provider;

import java.util.ArrayList;
import java.util.List;

import org.alexd.jsonrpc.JSONRPCClient;
import org.blitzortung.android.data.beans.Station;
import org.blitzortung.android.data.beans.Stroke;
import org.json.JSONArray;
import org.json.JSONObject;

public class JsonRpcProvider extends DataProvider {
	
	private JSONRPCClient client;
	
	private Integer nextId = null;
	
	public List<Stroke> getStrokes(int timeInterval) {
		
		List<Stroke> strokes = new ArrayList<Stroke>();
		
		try {
			JSONObject response = client.callJSONObject("get_strokes", timeInterval, nextId);
			JSONArray strokes_array = (JSONArray)response.get("strokes");
			
			for (int i = 0; i < strokes_array.length(); i++) {
				strokes.add(new Stroke(strokes_array.getJSONArray(i)));
			}
			
			if (response.has("next")) {
			  nextId = (Integer)response.get("next");
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		return strokes;
	}

	@Override
	public List<Station> getStations() {
		List<Station> stations = new ArrayList<Station>();
		
		try {
			JSONObject response = client.callJSONObject("get_stations");
			JSONArray stations_array = (JSONArray)response.get("stations");
			
			for (int i = 0; i < stations_array.length(); i++) {
				stations.add(new Station(stations_array.getJSONArray(i)));
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		return stations;
	}

	@Override
	public ProviderType getType() {
		return ProviderType.RPC;
	}

	@Override
	public void setUp() {
		client = JSONRPCClient.create("http://tryb.de:7080/");

		client.setConnectionTimeout(40000);
		client.setSoTimeout(40000);
	}

	@Override
	public void shutDown() {
	}
}
