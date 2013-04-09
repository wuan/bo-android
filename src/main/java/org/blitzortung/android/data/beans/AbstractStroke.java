package org.blitzortung.android.data.beans;

import android.location.Location;

public abstract class AbstractStroke implements Stroke {

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

	public long getTimestamp() {
		return timestamp;
	}
	
	protected void setTimestamp(long timestamp) {
		this.timestamp = timestamp;
	}
	
	public int getMultiplicity() {
		return 1;
	}

    public Location getLocation(Location location) {
        location.setLongitude(getLongitude());
        location.setLatitude(getLatitude());
        return location;
    }
    
	public Location getLocation() {
		return getLocation(new Location(""));
	}
	
}
