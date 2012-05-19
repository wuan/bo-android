package org.blitzortung.android.data.beans;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import android.location.Location;

public abstract class AbstractStroke {

	public static final SimpleDateFormat DATE_TIME_FORMATTER = new SimpleDateFormat("yyyyMMdd'T'HH:mm:ss.SSS");
	static {
		TimeZone tz = TimeZone.getTimeZone("UTC");
		DATE_TIME_FORMATTER.setTimeZone(tz);
	}

	public abstract float getLongitude();

	public abstract float getLatitude();

	public abstract Date getTime();
	
	public int getCount() {
		return 1;
	}

	public Location getLocation() {
		Location location =  new Location("");
		location.setLongitude(getLongitude());
		location.setLatitude(getLatitude());
		return location;
	}
	
}
