package org.blitzortung.android.data.beans;

import java.io.Serializable;

import org.blitzortung.android.data.Coordsys;
import org.json.JSONException;
import org.json.JSONObject;

import android.graphics.Point;
import android.graphics.RectF;

import com.google.android.maps.Projection;

public class Raster implements Serializable {

	private static final long serialVersionUID = -2751803268193381643L;
	
	private float lon_start;
	private float lat_start;
	private float lon_delta;
	private float lat_delta;
	private int lon_count;
	private int lat_count;

	public Raster(JSONObject jsonObject) throws JSONException {
		lon_start = (float) jsonObject.getDouble("x0");
		lat_start = (float) jsonObject.getDouble("y1");
		lon_delta = (float) jsonObject.getDouble("xd");
		lat_delta = (float) jsonObject.getDouble("yd");
		lon_count = jsonObject.getInt("xc");
		lat_count = jsonObject.getInt("yc");
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
	
	public RectF getRect(Projection projection) {
		Point leftTop = new Point();
		leftTop = projection.toPixels(Coordsys.toMapCoords(lon_start, lat_start), leftTop);
		Point bottomRight = new Point();
		bottomRight = projection.toPixels(Coordsys.toMapCoords(lon_start + lon_count * lon_delta, lat_start - lat_count * lat_delta), bottomRight);
		return new RectF(leftTop.x, leftTop.y, bottomRight.x, bottomRight.y);
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
