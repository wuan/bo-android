package org.blitzortung.android.data.beans;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import org.json.JSONArray;
import org.json.JSONException;

public class Stroke {
	
	private static final SimpleDateFormat DATE_TIME_FORMATTER = new SimpleDateFormat("yyyyMMdd'T'HH:mm:ss.SSS");
	static {
		TimeZone tz = TimeZone.getTimeZone("UTC");
		DATE_TIME_FORMATTER.setTimeZone(tz);
	}
	
	private Date timestamp;
	
	private int nanoseconds;
	
	private float longitude;
	
	private float latitude;
	
	private float amplitude;
	
	private short stationCount;
	
	private float lateralError;
	
	private short type;
	
	public Stroke(JSONArray jsonArray) {
		
		try {
			timestamp = DATE_TIME_FORMATTER.parse(jsonArray.getString(0));
			nanoseconds = jsonArray.getInt(1);
			longitude = (float)jsonArray.getDouble(2);
			latitude = (float)jsonArray.getDouble(3);
			lateralError = (float)jsonArray.getDouble(4);
			amplitude = (float)jsonArray.getDouble(4);
			stationCount = (short)jsonArray.getInt(5);
			type = (short)jsonArray.getInt(6);
		} catch (ParseException e) {
			throw new RuntimeException("error parsing stroke data", e);
		} catch (JSONException e) {
			throw new RuntimeException("error with json format while parsing stroke data", e);
		}
	}
	
	public Stroke(String line) {
		String[] fields = line.split(" ");
		String timeString = fields[0].replace("-", "") + "T" + fields[1];
		int len = timeString.length();
		try {
			timestamp = DATE_TIME_FORMATTER.parse(timeString.substring(0, len-6));
			nanoseconds = Integer.valueOf(timeString.substring(len-6));
			latitude = Float.valueOf(fields[2]);
			longitude = Float.valueOf(fields[3]);
			amplitude = Float.valueOf(fields[4].substring(0, fields[4].length()-2));
			type = Short.valueOf(fields[5]);
			lateralError = Float.valueOf(fields[6].substring(0, fields[6].length()-1));
			stationCount = Short.valueOf(fields[7]);
		} catch (ParseException e) {
			throw new RuntimeException("error parsing stroke data", e);
		}
		
	}
	
	public Date getTime() {
		return timestamp;
	}
	
	public int getNanoseconds() {
		return nanoseconds;
	}
	
	public float getLongitude() {
		return longitude;
	}
	
	public float getLatitude() {
		return latitude;
	}

	public float getAmplitude() {
		return amplitude;
	}

	public short getStationCount() {
		return stationCount;
	}

	public float getLateralError() {
		return lateralError;
	}
	
	public short getType() {
		return type;
	}
	
}
