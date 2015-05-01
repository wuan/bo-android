package org.blitzortung.android.alert.object;

import org.blitzortung.android.alert.AlertParameters;
import org.blitzortung.android.alert.factory.AlertObjectFactory;

import java.util.ArrayList;
import java.util.List;

public class AlertSector {

    private final List<AlertSectorRange> ranges;

    private final float minimumSectorBearing;

    private final float maximumSectorBearing;

    private String label;

    private float closestStrikeDistance;

    public AlertSector(AlertObjectFactory alertObjectFactory, AlertParameters alertParameters, String label, float minimumSectorBearing, float maximumSectorBearing) {
        this.label = label;
        this.minimumSectorBearing = minimumSectorBearing;
        this.maximumSectorBearing = maximumSectorBearing;
        this.closestStrikeDistance = Float.POSITIVE_INFINITY;

        final float[] rangeSteps = alertParameters.getRangeSteps();
        ranges = new ArrayList<AlertSectorRange>();
        float rangeMinimum = 0.0f;
        for (float rangeMaximum : rangeSteps) {
            AlertSectorRange alertSectorRange = alertObjectFactory.createAlarmSectorRange(rangeMinimum, rangeMaximum);
            ranges.add(alertSectorRange);
            rangeMinimum = rangeMaximum;
        }
    }

    public void clearResults() {
        closestStrikeDistance = Float.POSITIVE_INFINITY;
        
        for (AlertSectorRange range : ranges) {
            range.clearResults();
        }
    }

    public List<AlertSectorRange> getRanges() {
        return ranges;
    }

    public float getMinimumSectorBearing() {
        return minimumSectorBearing;
    }

    public float getMaximumSectorBearing() {
        return maximumSectorBearing;
    }

    public String getLabel() {
        return label;
    }

    public void updateClosestStrikeDistance(float distance) {
        closestStrikeDistance = Math.min(distance, closestStrikeDistance);
    }

    public float getClosestStrikeDistance() {
        return closestStrikeDistance;
    }
}
