package org.blitzortung.android.data.provider;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import org.blitzortung.android.data.beans.AbstractStroke;
import org.blitzortung.android.data.beans.Raster;
import org.blitzortung.android.data.beans.RasterElement;
import org.blitzortung.android.data.beans.Station;
import org.blitzortung.android.data.beans.Stroke;
import org.blitzortung.android.jsonrpc.JsonRpcClient;
import org.json.JSONArray;
import org.json.JSONObject;

public class JsonRpcProvider extends DataProvider {
	
	public static final SimpleDateFormat DATE_TIME_FORMATTER = new SimpleDateFormat("yyyyMMdd'T'HH:mm:ss");
	static {
		TimeZone tz = TimeZone.getTimeZone("UTC");
		DATE_TIME_FORMATTER.setTimeZone(tz);
	}

	private JsonRpcClient client;
	
	private Integer nextId = null;
	
	public List<AbstractStroke> getStrokes(int timeInterval) {
		//return getIndividualStrokes(timeInterval);
		return getStrokesRaster(timeInterval);
	}
	
	public List<AbstractStroke> getIndividualStrokes(int timeInterval) {
		
		List<AbstractStroke> strokes = new ArrayList<AbstractStroke>();
		
		try {
			JSONObject response = client.call("get_strokes", timeInterval, nextId);

			Date timestamp = new Date();
			timestamp = DATE_TIME_FORMATTER.parse(response.getString("reference_time"));
			
			JSONArray strokes_array = (JSONArray)response.get("strokes");
			
			for (int i = 0; i < strokes_array.length(); i++) {
				strokes.add(new Stroke(timestamp, strokes_array.getJSONArray(i)));
			}
			
			if (response.has("next")) {
			  nextId = (Integer)response.get("next");
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		return strokes;
	}
	
	Raster raster = null;
	
	public List<AbstractStroke> getStrokesRaster(int timeInterval) {
		
		List<AbstractStroke> strokes = new ArrayList<AbstractStroke>();
		
		nextId = null;
		
		try {
			JSONObject response = client.call("get_strokes_raster", timeInterval);

			raster = new Raster(response);
			
			Date timestamp = new Date();
			timestamp = DATE_TIME_FORMATTER.parse(response.getString("reference_time"));

			JSONArray strokes_array = (JSONArray)response.get("strokes_raster");
			
			for (int i = 0; i < strokes_array.length(); i++) {
				strokes.add(new RasterElement(raster, timestamp, strokes_array.getJSONArray(i)));
			}
			
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		
		return strokes;
	}
	
	public Raster getRaster() {
		return raster;
	}

	@Override
	public List<Station> getStations() {
		List<Station> stations = new ArrayList<Station>();
		
		try {
			JSONObject response = client.call("get_stations");
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
		client = new JsonRpcClient("http://tryb.de:7080/");

		client.setConnectionTimeout(40000);
		client.setSocketTimeout(40000);
	}

	@Override
	public void shutDown() {
	}
}
