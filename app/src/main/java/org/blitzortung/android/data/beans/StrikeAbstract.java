package org.blitzortung.android.data.beans;

import android.location.Location;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@EqualsAndHashCode
@RequiredArgsConstructor
public abstract class StrikeAbstract implements Strike {

    protected final long timestamp;

    protected final float longitude;

    protected final float latitude;

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
