package org.blitzortung.android.data.beans;

import java.io.Serializable;

public class DefaultStrike extends StrikeAbstract implements Serializable {
	
	private static final long serialVersionUID = 4201042078597105622L;

    private int altitude;

	private float amplitude;
	
	private short stationCount;
	
	private float lateralError;

    public DefaultStrike(long timestamp, float xCoord, float yCoord, int altitude, float amplitude, short stationCount, float lateralError)
    {
        setTimestamp(timestamp);
        setLongitude(xCoord);
        setLatitude(yCoord);
        this.lateralError = lateralError;
        this.altitude = altitude;
        this.amplitude = amplitude;
        this.stationCount = stationCount;
    }

    public int getAltitude() {
        return altitude;
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
	
}
