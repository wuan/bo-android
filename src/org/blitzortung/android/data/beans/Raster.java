package org.blitzortung.android.data.beans;

import org.json.JSONException;
import org.json.JSONObject;

public class Raster {
	private float lon_start;
	private float lat_start;
	private float lon_delta;
	private float lat_delta;

	public Raster(JSONObject jsonObject) throws JSONException {
		lon_start = (float) jsonObject.getDouble("x0");
		lat_start = (float) jsonObject.getDouble("y1");
		lon_delta = (float) jsonObject.getDouble("xd");
		lat_delta = (float) jsonObject.getDouble("yd");
	}

	public float getCenterLongitude(int offset) {
		return lon_start + lon_delta * (offset + 0.5f);
	}

	public float getCenterLatitude(int offset) {
		return lat_start - lat_delta * (offset + 0.5f);
	}

	public float getLongitudeDelta() {
		return lon_delta;
	}

	public float getLatitudeDelta() {
		return lat_delta;
	}
	
	@Override
	public String toString() {
		return String.format("Raster(%.4f, %.4f; %.4f, %.4f)", lon_start, lon_delta, lat_start, lat_delta);
	}

	public int getLongitudeIndex(double longitude) {
		return (int)((longitude - lon_start) / lon_delta + 0.5);
	}
	
	public int getLatitudeIndex(double latitude) {
		return (int)((lat_start - latitude) / lat_delta + 0.5);
	}

}
