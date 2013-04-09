package org.blitzortung.android.data.beans;

import org.blitzortung.android.util.TimeFormat;
import org.json.JSONArray;
import org.json.JSONException;

import java.io.Serializable;

public class DefaultStroke extends AbstractStroke implements Serializable {
	
	private static final long serialVersionUID = 4201042078597105622L;
	
	private float amplitude;
	
	private short stationCount;
	
	private float lateralError;
	
	private short type;
	
	public DefaultStroke(long referenceTimestamp, JSONArray jsonArray) {
		
		try {
			setTimestamp(referenceTimestamp - 1000 * jsonArray.getInt(0));
			setLongitude((float)jsonArray.getDouble(1));
			setLatitude((float)jsonArray.getDouble(2));
			lateralError = (float)jsonArray.getDouble(3);
			amplitude = (float)jsonArray.getDouble(4);
			stationCount = (short)jsonArray.getInt(5);
			type = (short)jsonArray.getInt(6);
		} catch (JSONException e) {
			throw new IllegalStateException("error with JSON format while parsing stroke data", e);
		}
	}
	
	public DefaultStroke(String line) {
		String[] fields = line.split(" ");
		String timeString = fields[0].replace("-", "") + "T" + fields[1];
		int len = timeString.length();
		setTimestamp(TimeFormat.parseTimeWithMilliseconds(timeString.substring(0, len - 6)));
		setLatitude(Float.valueOf(fields[2]));
		setLongitude(Float.valueOf(fields[3]));
		amplitude = Float.valueOf(fields[4].substring(0, fields[4].length() - 2));
		type = Short.valueOf(fields[5]);
		lateralError = Float.valueOf(fields[6].substring(0, fields[6].length() - 1));
		stationCount = Short.valueOf(fields[7]);
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
