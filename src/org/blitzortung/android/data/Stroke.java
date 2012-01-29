package org.blitzortung.android.data;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import org.json.JSONArray;
import org.json.JSONException;

public class Stroke {
	
	private Date timestamp;
	
	private float longitude;
	
	private float latitude;
	
	public Stroke(JSONArray jsonArray) {
		TimeZone tz = TimeZone.getTimeZone("UTC");
		SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMdd'T'HH:mm:ss");
		formatter.setTimeZone(tz);
		
		try {
			timestamp = formatter.parse(jsonArray.getString(0));
			longitude = (float)jsonArray.getDouble(1);
			latitude = (float)jsonArray.getDouble(2);
		} catch (ParseException e) {
			throw new RuntimeException("error parsing stroke data");
		} catch (JSONException e) {
			throw new RuntimeException("error with json format while parsing stroke data");
		}
	}
	
	public Date getTime() {
		return timestamp;
	}
	
	public float getLongitude() {
		return longitude;
	}
	
	public float getLatitude() {
		return latitude;
	}
}
