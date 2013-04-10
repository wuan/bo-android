package org.blitzortung.android.data.beans;

import android.location.Location;

public interface Stroke {

    long getTimestamp();

    Location getLocation(Location location);

    int getMultiplicity();
}
