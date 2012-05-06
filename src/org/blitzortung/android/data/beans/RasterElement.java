package org.blitzortung.android.data.beans;

import java.text.ParseException;
import java.util.Date;

import org.json.JSONArray;
import org.json.JSONException;

import android.util.Log;

public class RasterElement extends AbstractStroke {

	private Date timestamp;
	
	private float longitude;
	
	private float latitude;
	
	private int count;
	
	public  RasterElement(Raster raster, Date referenceTimestamp, JSONArray jsonArray) {
		try {
			longitude = raster.getCenterLongitude(jsonArray.getInt(0));
			latitude = raster.getCenterLatitude(jsonArray.getInt(1));
			count = jsonArray.getInt(2);
			
			timestamp = new Date();
			timestamp.setTime(referenceTimestamp.getTime() + 1000 * jsonArray.getInt(3));
		} catch (JSONException e) {
			throw new RuntimeException("error with json format while parsing stroke data", e);
		}
	}

	@Override
	public float getLongitude() {
		return longitude;
	}

	@Override
	public float getLatitude() {
		return latitude;
	}

	@Override
	public Date getTime() {
		return timestamp;
	}
	
	@Override
	public int getCount() {
		return count;
	}
}
