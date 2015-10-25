package org.blitzortung.android.data.beans;

public class Station {

    public enum State {
        ON, DELAYED, OFF
    }

    public static final long OFFLINE_SINCE_NOT_SET = -1;

    private String name;

    private float longitude;

    private float latitude;

    private long offlineSince;

    public Station(String name, float longitude, float latitude, long offlineSince) {
        this.name = name;
        this.longitude = longitude;
        this.latitude = latitude;
        this.offlineSince = offlineSince;
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
        if (offlineSince == OFFLINE_SINCE_NOT_SET) {
            return State.ON;
        } else {

            long now = System.currentTimeMillis();

            long minutesAgo = (now - offlineSince) / 1000 / 60;

            if (minutesAgo > 24 * 60) {
                return State.OFF;
            } else if (minutesAgo > 15) {
                return State.DELAYED;
            } else {
                return State.ON;
            }
        }
    }
}
