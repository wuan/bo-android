package org.blitzortung.android.data.builder;

import org.blitzortung.android.data.beans.DefaultStrike;
import org.json.JSONArray;
import org.json.JSONException;

public class DefaultStrikeBuilder {

    private long timestamp;

    private float longitude;

    private float latitude;

    private int altitude;

    private float lateralError;

    private float amplitude;

    private short stationCount;

    public DefaultStrikeBuilder() {
        init();
    }

    public void init() {
        longitude = 0.0f;
        latitude = 0.0f;
        timestamp = 0l;
        altitude = 0;
        amplitude = 0.0f;
        lateralError = 0;
        stationCount = 0;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public void setLongitude(float longitude) {
        this.longitude = longitude;
    }

    public void setLatitude(float latitude) {
        this.latitude = latitude;
    }

    public void setAltitude(int altitude) {
        this.altitude = altitude;
    }

    public void setAmplitude(float amplitude) {
        this.amplitude = amplitude;
    }

    public void setLateralError(float lateralError) {
        this.lateralError = lateralError;
    }

    public void setStationCount(short stationCount) {
        this.stationCount = stationCount;
    }

    public DefaultStrike fromJson(long referenceTimestamp, JSONArray jsonArray) {
        try {
            setTimestamp(referenceTimestamp - 1000 * jsonArray.getInt(0));
            setLongitude((float) jsonArray.getDouble(1));
            setLatitude((float) jsonArray.getDouble(2));
            setLateralError((float) jsonArray.getDouble(3));
            setAltitude(0);
            setAmplitude((float) jsonArray.getDouble(4));
            setStationCount((short) jsonArray.getInt(5));
        } catch (JSONException e) {
            throw new IllegalStateException("error with JSON format while parsing strike data", e);
        }

        return build();
    }

    public DefaultStrike build() {
        return new DefaultStrike(timestamp, longitude, latitude, altitude, amplitude, stationCount, lateralError);
    }
}
