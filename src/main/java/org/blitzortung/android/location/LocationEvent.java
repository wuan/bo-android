package org.blitzortung.android.location;

import android.location.Location;
import org.blitzortung.android.protocol.Event;

public class LocationEvent implements Event {

    private final Location location;

    public LocationEvent(final Location location) {
        this.location = location;
    }

    public Location getLocation() {
        return location;
    }
}
