package org.blitzortung.android.data.beans;

import java.util.Date;

import org.json.JSONArray;
import org.json.JSONException;

public class RasterElement extends AbstractStroke {

	private Date timestamp;
	
	private float longitude;
	
	private float latitude;
	
	private int count;
	
	public  RasterElement(JSONArray jsonArray) {
		
		try {
			longitude = (float)jsonArray.getDouble(0);
			latitude = (float)jsonArray.getDouble(1);
			count = (int)jsonArray.getInt(2);
			timestamp = new Date();
			//timestamp = DATE_TIME_FORMATTER.parse(jsonArray.getString(3));
		//} catch (ParseException e) {
		//	throw new RuntimeException("error parsing stroke data", e);
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
	
	public int getCount() {
		return count;
	}
}
