package org.blitzortung.android.data.beans;

import java.io.Serializable;

import org.blitzortung.android.util.TimeFormat;
import org.json.JSONArray;
import org.json.JSONException;

public class Stroke extends AbstractStroke implements Serializable {
	
	private static final long serialVersionUID = 4201042078597105622L;

	private int nanoseconds;
	
	private float amplitude;
	
	private short stationCount;
	
	private float lateralError;
	
	private short type;
	
	public Stroke(long referenceTimestamp, JSONArray jsonArray) {
		
		try {
			setTimestamp(referenceTimestamp + 1000 * jsonArray.getInt(0) + jsonArray.getInt(1) / 1000000);
			nanoseconds = jsonArray.getInt(1) % 1000000;
			setLongitude((float)jsonArray.getDouble(2));
			setLatitude((float)jsonArray.getDouble(3));
			lateralError = (float)jsonArray.getDouble(4);
			amplitude = (float)jsonArray.getDouble(5);
			stationCount = (short)jsonArray.getInt(6);
			type = (short)jsonArray.getInt(7);
		} catch (JSONException e) {
			throw new RuntimeException("error with json format while parsing stroke data", e);
		}
	}
	
	public Stroke(String line) {
		String[] fields = line.split(" ");
		String timeString = fields[0].replace("-", "") + "T" + fields[1];
		int len = timeString.length();
		setTimestamp(TimeFormat.parseTimeWithMilliseconds(timeString.substring(0, len - 6)));
		nanoseconds = Integer.valueOf(timeString.substring(len - 6));
		setLatitude(Float.valueOf(fields[2]));
		setLongitude(Float.valueOf(fields[3]));
		amplitude = Float.valueOf(fields[4].substring(0, fields[4].length() - 2));
		type = Short.valueOf(fields[5]);
		lateralError = Float.valueOf(fields[6].substring(0, fields[6].length() - 1));
		stationCount = Short.valueOf(fields[7]);
	}
	
	public int getNanoseconds() {
		return nanoseconds;
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
