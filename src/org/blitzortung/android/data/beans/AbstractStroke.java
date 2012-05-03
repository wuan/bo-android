package org.blitzortung.android.data.beans;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

public abstract class AbstractStroke {

	protected static final SimpleDateFormat DATE_TIME_FORMATTER = new SimpleDateFormat("yyyyMMdd'T'HH:mm:ss.SSS");
	static {
		TimeZone tz = TimeZone.getTimeZone("UTC");
		DATE_TIME_FORMATTER.setTimeZone(tz);
	}

	public abstract float getLongitude();

	public abstract float getLatitude();

	public abstract Date getTime();
}
