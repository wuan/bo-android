package org.blitzortung.android.alarm.handler;

import android.location.Location;
import org.blitzortung.android.alarm.AlarmParameters;
import org.blitzortung.android.alarm.object.AlarmSector;
import org.blitzortung.android.alarm.object.AlarmSectorRange;
import org.blitzortung.android.data.beans.Stroke;

public class AlarmSectorHandler {

    private final AlarmParameters alarmParameters;
    private Location location;
    private final Location strokeLocation;

    public AlarmSectorHandler(AlarmParameters alarmParameters) {
        this.alarmParameters = alarmParameters;
        strokeLocation = new Location("");
    }

    public void setLocation(Location location) {
        this.location = location;
    }

    protected void check(AlarmSector sector, Stroke stroke, long thresholdTime) {
        if (sector != null) {
            float distance = calculateDistanceTo(stroke);

            if (stroke.getTimestamp() > thresholdTime) {
                sector.updateClosestStrokeDistance(distance);
            }

            for (AlarmSectorRange range : sector.getRanges()) {
                if (distance < range.getRangeMaximum()) {
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