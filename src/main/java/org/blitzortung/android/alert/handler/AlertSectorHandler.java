package org.blitzortung.android.alert.handler;

import android.location.Location;
import org.blitzortung.android.alert.AlertParameters;
import org.blitzortung.android.alert.object.AlertSector;
import org.blitzortung.android.alert.object.AlertSectorRange;
import org.blitzortung.android.data.beans.Stroke;

public class AlertSectorHandler {

    private final AlertParameters alertParameters;
    
    private final Location strokeLocation;
    
    private Location location;
    
    private long thresholdTime;

    public AlertSectorHandler(AlertParameters alertParameters) {
        this.alertParameters = alertParameters;
        strokeLocation = new Location("");
    }

    protected void setCheckStrokeParameters(Location location, long thresholdTime) {
        this.location = location;
        this.thresholdTime = thresholdTime;
    }

    protected void checkStroke(AlertSector sector, Stroke stroke) {
        if (sector != null) {
            float distance = calculateDistanceTo(stroke);

            for (AlertSectorRange range : sector.getRanges()) {
                if (distance <= range.getRangeMaximum()) {
                    range.addStroke(stroke);

                    if (stroke.getTimestamp() >= thresholdTime) {
                        sector.updateClosestStrokeDistance(distance);
                    }
                    
                    break;
                }
            }
        }
    }

    private float calculateDistanceTo(Stroke stroke) {
        float distanceInMeters = location.distanceTo(stroke.getLocation(strokeLocation));
        return alertParameters.getMeasurementSystem().calculateDistance(distanceInMeters);
    }

    public long getLatestTimestampWithin(float distanceLimit, AlertSector sector) {
        long latestTimestamp = 0;
        
        for (AlertSectorRange range : sector.getRanges()) {
            if (distanceLimit <= range.getRangeMaximum()) {
                latestTimestamp = Math.max(latestTimestamp, range.getLatestStrokeTimestamp());
            }
        }
        
        return latestTimestamp;
    }
}