package org.blitzortung.android.alarm;

import android.location.Location;
import org.blitzortung.android.data.beans.AbstractStroke;
import org.blitzortung.android.util.MeasurementSystem;

public class AlarmSector {
    private static final float DISTANCE_STEPS[] = {10, 25, 50, 100, 250, 500};

    private int strokeCount[];
    private long latestStrokeTimestamp[];
    private float closestStrokeDistance;

    private final float sectorBearing;
    private long thresholdTime;
    private MeasurementSystem measurementSystem;

    public AlarmSector(float sectorBearing, long thresholdTime, MeasurementSystem measurementSystem) {
        this.thresholdTime = thresholdTime;
        this.sectorBearing = sectorBearing;
        this.measurementSystem = measurementSystem;

        reset();
    }

    public void reset() {
        int size = getDistanceStepCount();

        strokeCount = new int[size];
        latestStrokeTimestamp = new long[size];
        closestStrokeDistance = Float.POSITIVE_INFINITY;
    }

    public void check(AbstractStroke stroke, Location location) {
        float distanceInMeters = location.distanceTo(stroke.getLocation());
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
                    closestStrokeDistance = Math.min(distance, closestStrokeDistance);
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

    public float getClosestStrokeDistance() {
        if (closestStrokeDistance < Float.POSITIVE_INFINITY) {
            return closestStrokeDistance;
        }

        for (int index = 0; index < getDistanceStepCount(); index++) {
            if (latestStrokeTimestamp[index] >= thresholdTime) {
                return getDistanceStep(index - 1);
            }
        }

        return Float.POSITIVE_INFINITY;
    }

    public static float[] getDistanceSteps() {
        return DISTANCE_STEPS;
    }

    private static float getDistanceStep(int index) {
        if (index == -1) {
            return 0.0f;
        }
        return DISTANCE_STEPS[index];
    }

    public static int getDistanceStepCount() {
        return DISTANCE_STEPS.length;
    }

    public float getBearing() {
        return sectorBearing;
    }

    public void update(long thresholdTime, long oldestStrokeTime, MeasurementSystem measurementSystem) {
        this.thresholdTime = thresholdTime;

        for (int index = 0; index < getDistanceStepCount(); index++) {
            if (latestStrokeTimestamp[index] < thresholdTime) {
                if (latestStrokeTimestamp[index] < oldestStrokeTime) {
                    latestStrokeTimestamp[index] = 0;
                    strokeCount[index] = 0;
                }

                if (closestStrokeDistance >= getDistanceStep(index - 1) &&
                        closestStrokeDistance < getDistanceStep(index)) {
                    closestStrokeDistance = Float.POSITIVE_INFINITY;
                }
            }
        }
    }

    public long getThresholdTime() {
        return thresholdTime;
    }

    public String getDistanceUnitName() {
        return measurementSystem.getUnitName();
    }
}
