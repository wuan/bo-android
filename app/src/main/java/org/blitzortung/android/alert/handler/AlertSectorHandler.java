package org.blitzortung.android.alert.handler;

import android.location.Location;

import com.annimon.stream.Stream;

import org.blitzortung.android.alert.AlertParameters;
import org.blitzortung.android.alert.data.AlertSector;
import org.blitzortung.android.alert.data.AlertSectorRange;
import org.blitzortung.android.data.beans.Strike;

public class AlertSectorHandler {

    private final AlertParameters alertParameters;

    private final Location strikeLocation;

    private Location location;

    private long thresholdTime;

    public AlertSectorHandler(AlertParameters alertParameters) {
        this.alertParameters = alertParameters;
        strikeLocation = new Location("");
    }

    protected void setCheckStrikeParameters(Location location, long thresholdTime) {
        this.location = location;
        this.thresholdTime = thresholdTime;
    }

    protected void checkStrike(AlertSector sector, Strike strike) {
        if (sector != null) {
            float distance = calculateDistanceTo(strike);

            Stream.of(sector.getRanges())
                    .filter(r -> distance <= r.getRangeMaximum())
                    .findFirst()
                    .ifPresent(range -> {
                        range.addStrike(strike);

                        if (strike.getTimestamp() >= thresholdTime) {
                            sector.updateClosestStrikeDistance(distance);
                        }
                    });
        }
    }

    private float calculateDistanceTo(Strike strike) {
        float distanceInMeters = location.distanceTo(strike.getLocation(strikeLocation));
        return alertParameters.getMeasurementSystem().calculateDistance(distanceInMeters);
    }

    public long getLatestTimestampWithin(float distanceLimit, AlertSector sector) {
        long latestTimestamp = 0;

        for (AlertSectorRange range : sector.getRanges()) {
            if (distanceLimit <= range.getRangeMaximum()) {
                latestTimestamp = Math.max(latestTimestamp, range.getLatestStrikeTimestamp());
            }
        }

        return latestTimestamp;
    }
}