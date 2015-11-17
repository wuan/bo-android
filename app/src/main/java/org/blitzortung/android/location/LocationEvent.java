package org.blitzortung.android.location;

import android.location.Location;

import org.blitzortung.android.protocol.Event;

import lombok.Value;

@Value
public class LocationEvent implements Event {
    private final Location location;
}
