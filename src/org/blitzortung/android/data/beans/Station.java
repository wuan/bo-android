package org.blitzortung.android.data.beans;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import org.json.JSONArray;
import org.json.JSONException;

public class Station {
	
	enum Status {
		ON,
		DELAYED,
		OFF
	}
	
	private String name;
	
	private float longitude;
	
	private float latitude;
	
	Date lastDataTime;
	
	Status status;
	
	public Station(JSONArray jsonArray) {
		TimeZone tz = TimeZone.getTimeZone("UTC");
		SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMdd'T'HH:mm:ss");
		formatter.setTimeZone(tz);
		
		try {
			name = jsonArray.getString(1);
			longitude = (float)jsonArray.getDouble(3);
			latitude = (float)jsonArray.getDouble(4);
			lastDataTime = formatter.parse(jsonArray.getString(5));
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
