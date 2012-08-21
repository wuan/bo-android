package org.blitzortung.android.data.beans;

import android.text.Html;
import org.blitzortung.android.util.TimeFormat;
import org.json.JSONArray;
import org.json.JSONException;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.TimeZone;

public class Participant implements Serializable {

	private static final long serialVersionUID = -6731278932726028829L;

	public enum State {
		ON, DELAYED, OFF
	}

	private String name;

	private float longitude;

	private float latitude;

	long offlineSince;

	State state;

	public Participant(JSONArray jsonArray) {
		try {
			name = jsonArray.getString(1);
			longitude = (float) jsonArray.getDouble(3);
			latitude = (float) jsonArray.getDouble(4);
			if (jsonArray.length() >= 6) {

				String offlineSinceString = jsonArray.getString(5);
				if (offlineSinceString.length() > 0) {
					offlineSince = TimeFormat.parseTimeWithMilliseconds(offlineSinceString);

					long now = System.currentTimeMillis();

					long minutesAgo = (now - offlineSince) / 1000 / 60;

					updateState(minutesAgo);
				} else {
					state = State.ON;
				}
			} else {
				state = State.ON;
			}
		} catch (JSONException e) {
			throw new IllegalStateException("error with JSON format while parsing participants data");
		}
	}

	private void updateState(long minutesAgo) {
		if (minutesAgo > 24 * 60) {
			state = State.OFF;
        } else if (minutesAgo > 15) {
			state = State.DELAYED;
        } else {
			state = State.ON;
        }
	}
	
	public Participant(String line) {
		String[] fields = line.split(" ");
		
		String timeString = fields[7].replace("-", "").replace("&nbsp;", "T");
		int len = timeString.length();
		long now = System.currentTimeMillis();
		long lastData = TimeFormat.parseTimeWithMilliseconds(timeString.substring(0, len - 6));

		name = Html.fromHtml(fields[3]).toString();
		longitude = Float.valueOf(fields[6]);
		latitude = Float.valueOf(fields[5]);

		updateState((now - lastData)/1000/60);

        if (state == State.OFF) {
            offlineSince = lastData;
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

	public long getOfflineSince() {
		return offlineSince;
	}

	public State getState() {
		return state;
	}
}
