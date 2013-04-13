package org.blitzortung.android.alarm.object;

import org.blitzortung.android.alarm.AlarmParameters;
import org.blitzortung.android.alarm.factory.AlarmObjectFactory;

import java.util.ArrayList;
import java.util.List;

public class AlarmSector {

    private final List<AlarmSectorRange> ranges;

    private final float minimumSectorBearing;

    private final float maximumSectorBearing;

    private String label;

    private float closestStrokeDistance;

    public AlarmSector(AlarmObjectFactory alarmObjectFactory, AlarmParameters alarmParameters, String label, float minimumSectorBearing, float maximumSectorBearing) {
        this.label = label;
        this.minimumSectorBearing = minimumSectorBearing;
        this.maximumSectorBearing = maximumSectorBearing;
        this.closestStrokeDistance = Float.POSITIVE_INFINITY;

        final float[] rangeSteps = alarmParameters.getRangeSteps();
        ranges = new ArrayList<AlarmSectorRange>();
        float rangeMinimum = 0.0f;
        for (float rangeMaximum : rangeSteps) {
            AlarmSectorRange alarmSectorRange = alarmObjectFactory.createAlarmSectorRange(rangeMinimum, rangeMaximum);
            ranges.add(alarmSectorRange);
            rangeMinimum = rangeMaximum;
        }
    }

    public void clearResults() {
        closestStrokeDistance = Float.POSITIVE_INFINITY;
        
        for (AlarmSectorRange range : ranges) {
            range.clearResults();
        }
    }

    public List<AlarmSectorRange> getRanges() {
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

    public void updateClosestStrokeDistance(float distance) {
        closestStrokeDistance = Math.min(distance, closestStrokeDistance);
    }

    public float getClosestStrokeDistance() {
        return closestStrokeDistance;
    }
}
