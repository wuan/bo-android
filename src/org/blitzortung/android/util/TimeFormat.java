package org.blitzortung.android.util;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

public class TimeFormat {
	private static final SimpleDateFormat DATE_TIME_MILLISECONDS_FORMATTER = new SimpleDateFormat("yyyyMMdd'T'HH:mm:ss.SSS");
	static {
		TimeZone tz = TimeZone.getTimeZone("UTC");
		DATE_TIME_MILLISECONDS_FORMATTER.setTimeZone(tz);
	}
	
	private static final SimpleDateFormat DATE_TIME_FORMATTER = new SimpleDateFormat("yyyyMMdd'T'HH:mm:ss");
	static {
		TimeZone tz = TimeZone.getTimeZone("UTC");
		DATE_TIME_FORMATTER.setTimeZone(tz);
	}
	
	public static long parseTimeWithMilliseconds(String timestampString) {
		try {
			Date timestamp  = DATE_TIME_MILLISECONDS_FORMATTER.parse(timestampString);
			return timestamp.getTime();
		} catch (ParseException e) {
			e.printStackTrace();
		}
		return 0;
	}
	
	public static long parseTime(String timestampString) {
		try {
			Date timestamp  = DATE_TIME_FORMATTER.parse(timestampString);
			return timestamp.getTime();
		} catch (ParseException e) {
			e.printStackTrace();
		}
		return 0;
	}
}