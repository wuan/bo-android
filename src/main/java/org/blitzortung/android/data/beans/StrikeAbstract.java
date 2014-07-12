package org.blitzortung.android.data.beans;

import android.location.Location;

public abstract class StrikeAbstract implements Strike {

	private long timestamp;

	private float longitude;
	
	private float latitude;
	
	public float getLongitude() {
		return longitude;
	}
	
	protected void setLongitude(float longitude) {
		this.longitude = longitude;
	}

	public float getLatitude() {
		return latitude;
	}
	
	protected void setLatitude(float latitude) {
		this.latitude = latitude;
	}

    @Override
	public long getTimestamp() {
		return timestamp;
	}
	
	protected void setTimestamp(long timestamp) {
		this.timestamp = timestamp;
	}
	
    @Override
	public int getMultiplicity() {
		return 1;
	}

    @Override
    public Location getLocation(Location location) {
        location.setLongitude(getLongitude());
        location.setLatitude(getLatitude());
        return location;
    }
}
