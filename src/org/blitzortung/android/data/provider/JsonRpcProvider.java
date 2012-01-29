package org.blitzortung.android.data.provider;

import java.util.ArrayList;
import java.util.List;

import org.alexd.jsonrpc.JSONRPCClient;
import org.alexd.jsonrpc.JSONRPCException;
import org.blitzortung.android.data.Credentials;
import org.blitzortung.android.data.Stroke;
import org.json.JSONArray;
import org.json.JSONException;

public class JsonRpcProvider implements DataProvider {

	private Credentials creds;
	
	public JsonRpcProvider(Credentials creds) {
		this.creds = creds;
	}
	
	public List<Stroke> getStrokes() {
		
		List<Stroke> strokes = new ArrayList<Stroke>();
		
		JSONRPCClient client = JSONRPCClient.create("http://tryb.de:7080/");

		client.setConnectionTimeout(2000);
		client.setSoTimeout(2000);

		try {
			JSONArray response = client.callJSONArray("get_strokes", -60);
			
			for (int i = 0; i < response.length(); i++) {
				strokes.add(new Stroke(response.getJSONArray(i)));
			}
		} catch (JSONRPCException e) {
			e.printStackTrace();
		} catch (JSONException e) {
			e.printStackTrace();
		}
		
		return strokes;
	}
}
