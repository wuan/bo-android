package org.blitzortung.android.alarm;

import android.location.Location;
import org.blitzortung.android.data.beans.AbstractStroke;

public class AlarmSector {
    private static final float DISTANCE_STEPS[] = {10000, 25000, 50000, 100000, 250000, 500000};

    private int strokeCount[];
    private long latestStrokeTimestamp[];
    private float warnMinimumDistance;

    private final float sectorCenterBearing;
    private long warnThresholdTime;

    public AlarmSector(float sectorCenterBearing, long warnThresholdTime) {
        this.warnThresholdTime = warnThresholdTime;
        this.sectorCenterBearing = sectorCenterBearing;

        reset();
    }

    public void reset() {
        int size = getDistanceStepCount();

        strokeCount = new int[size];
        latestStrokeTimestamp = new long[size];
        warnMinimumDistance = Float.POSITIVE_INFINITY;
    }

    public void check(Location location, AbstractStroke stroke) {
        check(location.distanceTo(stroke.getLocation()), stroke.getTimestamp(), stroke.getMultiplicity());
    }

    public void check(float distance, long timeStamp, int multiplicity) {
        int stepIndex = 0;
        for (double stepDistance : DISTANCE_STEPS) {
            if (distance <= stepDistance) {
                strokeCount[stepIndex] += multiplicity;

                if (timeStamp > latestStrokeTimestamp[stepIndex]) {
                    latestStrokeTimestamp[stepIndex] = timeStamp;
                }
                if (timeStamp > warnThresholdTime) {
                    warnMinimumDistance = Math.min(distance, warnMinimumDistance);
                }
                break;
            }
            stepIndex++;
        }
    }

    public int[] getEventCount() {
        return strokeCount;
    }

    public long[] getLatestTimes() {
        return latestStrokeTimestamp;
    }

    public int getMinimumIndex() {
        for (int index = 0; index < getDistanceStepCount(); index++) {
            if (strokeCount[index] > 0) {
                return index;
            }
        }
        return -1;
    }

    public int getCount(int index) {
        return strokeCount[index];
    }

    public long getLatestTime(int index) {
        return latestStrokeTimestamp[index];
    }

    public float getWarnMinimumDistance() {
        if (warnMinimumDistance < Float.POSITIVE_INFINITY) {
            return warnMinimumDistance;
        }

        for (int index = 0; index < getDistanceStepCount(); index++) {
            if (latestStrokeTimestamp[index] >= warnThresholdTime) {
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
        return sectorCenterBearing;
    }

    public void updateWarnThresholdTime(long warnThresholdTime, long oldestStrokeTime) {
        this.warnThresholdTime = warnThresholdTime;

        for (int index = 0; index < getDistanceStepCount(); index++) {
            if (latestStrokeTimestamp[index] < warnThresholdTime) {
                if (latestStrokeTimestamp[index] < oldestStrokeTime) {
                    latestStrokeTimestamp[index] = 0;
                    strokeCount[index] = 0;
                }

                if (warnMinimumDistance >= getDistanceStep(index - 1) &&
                        warnMinimumDistance < getDistanceStep(index)) {
                    warnMinimumDistance = Float.POSITIVE_INFINITY;
                }
            }
        }
    }

    public long getWarnThresholdTime() {
        return warnThresholdTime;
    }
}
