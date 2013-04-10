package org.blitzortung.android.alarm;

import android.location.Location;
import org.blitzortung.android.data.beans.Stroke;
import org.blitzortung.android.util.MeasurementSystem;

public class AlarmSector {
    private static final float DISTANCE_STEPS[] = {10, 25, 50, 100, 250, 500};

    private final int strokeCount[];
    private long latestStrokeTimestamp[];
    private float minimumAlarmRelevantStrokeDistance;

    private final float sectorBearing;
    private final Location tmpLocation;
    private long thresholdTime;
    private MeasurementSystem measurementSystem;

    public AlarmSector(float sectorBearing, long thresholdTime, MeasurementSystem measurementSystem) {
        this.thresholdTime = thresholdTime;
        this.sectorBearing = sectorBearing;
        this.measurementSystem = measurementSystem;

        int size = getDistanceStepCount();
        strokeCount = new int[size];
        latestStrokeTimestamp = new long[size];
        
        tmpLocation = new Location("");
        
        reset();
    }

    private void reset() {
        for (int i=0; i < getDistanceStepCount(); i++) {
            strokeCount[i] = 0;
            latestStrokeTimestamp[i] = 0;
        }

        minimumAlarmRelevantStrokeDistance = Float.POSITIVE_INFINITY;
    }

    public void check(Stroke stroke, Location location) {
        float distanceInMeters = location.distanceTo(stroke.getLocation(tmpLocation));
        float distance = measurementSystem.calculateDistance(distanceInMeters);
        check(distance, stroke.getTimestamp(), stroke.getMultiplicity());
    }

    // VisibleForTesting
    protected void check(float distance, long timeStamp, int multiplicity) {
        int stepIndex = 0;
        for (double stepDistance : DISTANCE_STEPS) {
            if (distance <= stepDistance) {
                strokeCount[stepIndex] += multiplicity;

                if (timeStamp > latestStrokeTimestamp[stepIndex]) {
                    latestStrokeTimestamp[stepIndex] = timeStamp;
                }
                
                if (timeStamp > thresholdTime) {
                    minimumAlarmRelevantStrokeDistance = Math.min(distance, minimumAlarmRelevantStrokeDistance);
                }
                break;
            }
            stepIndex++;
        }
    }

    // VisibleForTesting
    protected int[] getStrokeCounts() {
        return strokeCount;
    }

    // VisibleForTesting
    protected long[] getTimesOfLatestStroke() {
        return latestStrokeTimestamp;
    }

    public int getStrokeCount(int index) {
        return strokeCount[index];
    }

    public long getLatestTime(int index) {
        return latestStrokeTimestamp[index];
    }

    public float getMinimumAlarmRelevantStrokeDistance() {
        return minimumAlarmRelevantStrokeDistance;
    }

    public static float[] getDistanceSteps() {
        return DISTANCE_STEPS;
    }

    public static int getDistanceStepCount() {
        return DISTANCE_STEPS.length;
    }

    public float getBearing() {
        return sectorBearing;
    }

    public void update(long thresholdTime, MeasurementSystem measurementSystem) {
        reset();

        this.thresholdTime = thresholdTime;
        this.measurementSystem = measurementSystem;
    }

    public long getThresholdTime() {
        return thresholdTime;
    }

    public String getDistanceUnitName() {
        return measurementSystem.getUnitName();
    }
}
