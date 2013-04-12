package org.blitzortung.android.alarm.handler;

import android.location.Location;
import org.blitzortung.android.alarm.AlarmParameters;
import org.blitzortung.android.alarm.object.AlarmSector;
import org.blitzortung.android.alarm.object.AlarmSectorRange;
import org.blitzortung.android.data.beans.Stroke;

public class AlarmSectorHandler {

    private final AlarmParameters alarmParameters;
    
    private final Location strokeLocation;
    
    private Location location;
    
    private long thresholdTime;

    public AlarmSectorHandler(AlarmParameters alarmParameters) {
        this.alarmParameters = alarmParameters;
        strokeLocation = new Location("");
    }

    protected void setCheckStrokeParameters(Location location, long thresholdTime) {
        this.location = location;
        this.thresholdTime = thresholdTime;
    }

    protected void checkStroke(AlarmSector sector, Stroke stroke) {
        if (sector != null) {
            float distance = calculateDistanceTo(stroke);

            if (stroke.getTimestamp() >= thresholdTime) {
                sector.updateClosestStrokeDistance(distance);
            }

            for (AlarmSectorRange range : sector.getRanges()) {
                if (distance <= range.getRangeMaximum()) {
                    range.addStroke(stroke);
                    break;
                }
            }
        }
    }

    private float calculateDistanceTo(Stroke stroke) {
        float distanceInMeters = location.distanceTo(stroke.getLocation(strokeLocation));
        return alarmParameters.getMeasurementSystem().calculateDistance(distanceInMeters);
    }
}