package org.blitzortung.android.data.beans;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import org.json.JSONArray;
import org.json.JSONException;

public class Station {
	
	private String name;
	
	private float longitude;
	
	private float latitude;
	
	Date lastDataTime;
	
	public Station(JSONArray jsonArray) {
		TimeZone tz = TimeZone.getTimeZone("UTC");
		SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMdd'T'HH:mm:ss");
		formatter.setTimeZone(tz);
		
		try {
			name = jsonArray.getString(0);
			longitude = (float)jsonArray.getDouble(1);
			latitude = (float)jsonArray.getDouble(2);
			lastDataTime = formatter.parse(jsonArray.getString(3));
		} catch (ParseException e) {
			throw new RuntimeException("error parsing station data");
		} catch (JSONException e) {
			throw new RuntimeException("error with JSON format while parsing station data");
		}
	}
	
	public String getName() {
		return name;
	}
	
	public float getLongitude() {
		return longitude;
	}
	
	public float getLatitude() {
		return latitude;
	}
	
	public Date getLastDataTime() {
		return lastDataTime;
	}
}
