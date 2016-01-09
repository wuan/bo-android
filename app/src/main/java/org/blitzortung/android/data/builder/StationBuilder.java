package org.blitzortung.android.data.builder;

import org.blitzortung.android.data.beans.Station;
import org.blitzortung.android.util.TimeFormat;
import org.json.JSONArray;
import org.json.JSONException;

public class StationBuilder {

	private String name;

	private float longitude;

	private float latitude;

	private long offlineSince;

    public void init() {
        name = "";
        longitude = 0.0f;
        latitude = 0.0f;
        setOfflineSince(0);
    }

	public void setName(String name) {
		this.name = name;
	}

	public void setLongitude(float longitude) {
        this.longitude = longitude;
	}

	public void setLatitude(float latitude) {
        this.latitude = latitude;
	}

	public void setOfflineSince(long offlineSince) {
        this.offlineSince = offlineSince;
	}

    public Station fromJson(JSONArray jsonArray) {
        try {
            setName(jsonArray.getString(1));
            setLongitude((float) jsonArray.getDouble(3));
            setLatitude((float) jsonArray.getDouble(4));
            if (jsonArray.length() >= 6) {

                String offlineSinceString = jsonArray.getString(5);
                setOfflineSince( offlineSinceString.length() > 0
                        ? TimeFormat.parseTimeWithMilliseconds(offlineSinceString)
                        : Station.OFFLINE_SINCE_NOT_SET);
            } else {
                setOfflineSince(Station.OFFLINE_SINCE_NOT_SET);
            }
        } catch (JSONException e) {
            throw new IllegalStateException("error with JSON format while parsing participants data");
        }
        return build();
    }

    public Station build() {
        return new Station(name, longitude, latitude, offlineSince);
    }

}
