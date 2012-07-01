package org.blitzortung.android.data.beans;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;

import org.blitzortung.android.util.TimeFormat;
import org.json.JSONArray;
import org.json.JSONException;

import android.text.Html;

public class Participant {

	public enum State {
		ON, DELAYED, OFF
	}

	private String name;

	private float longitude;

	private float latitude;

	long offlineSince;

	State state;

	public Participant(JSONArray jsonArray) {
		TimeZone tz = TimeZone.getTimeZone("UTC");
		SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMdd'T'HH:mm:ss");
		formatter.setTimeZone(tz);

		try {
			name = jsonArray.getString(1);
			longitude = (float) jsonArray.getDouble(3);
			latitude = (float) jsonArray.getDouble(4);
			if (jsonArray.length() >= 6) {

				String offlineSinceString = jsonArray.getString(5);
				if (offlineSinceString.length() > 0) {
					offlineSince = TimeFormat.parseTimeWithMilliseconds(offlineSinceString);

					Date now = new GregorianCalendar().getTime();

					int minutesAgo = (int) (now.getTime() - offlineSince) / 1000 / 60;

					updateState(minutesAgo);
				} else {
					state = State.ON;
				}
			} else {
				state = State.ON;
			}
		} catch (JSONException e) {
			throw new RuntimeException("error with JSON format while parsing station data");
		}
	}

	private void updateState(int minutesAgo) {
		if (minutesAgo > 24 * 60)
			state = State.OFF;
		else if (minutesAgo > 15)
			state = State.DELAYED;
		else
			state = State.ON;
	}
	
	public Participant(String line) {
		String[] fields = line.split(" ");
		
		String timeString = fields[7].replace("-", "").replace("&nbsp;", "T");
		int len = timeString.length();
		long now = new GregorianCalendar().getTime().getTime();
		long lastData = TimeFormat.parseTimeWithMilliseconds(timeString.substring(0, len - 6));

		name = Html.fromHtml(fields[3]).toString();
		longitude = Float.valueOf(fields[6]);
		latitude = Float.valueOf(fields[5]);
		updateState((int)(now - lastData)/1000/60);
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

	public long getOfflineSince() {
		return offlineSince;
	}

	public State getState() {
		return state;
	}
}
