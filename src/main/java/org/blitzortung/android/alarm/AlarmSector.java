package org.blitzortung.android.alarm;

import android.location.Location;
import org.blitzortung.android.data.beans.AbstractStroke;

public class AlarmSector {
	private static final float DISTANCE_STEPS[] = { 10000, 25000, 50000, 100000, 250000, 500000 };

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
		warnMinimumDistance = (float) Double.POSITIVE_INFINITY;
	}

	public void check(Location location, AbstractStroke stroke) {
        check(location.distanceTo(stroke.getLocation()), stroke.getTimestamp(), stroke.getMultiplicity());
    }

    public void check(float distance, long timeStamp, int multiplicity)
    {
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

	public int[] getEventCounts() {
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
		return warnMinimumDistance;
	}

	public static float[] getDistanceSteps() {
		return DISTANCE_STEPS;
	}

	public static int getDistanceStepCount() {
		return DISTANCE_STEPS.length;
	}

	public float getBearing() {
		return sectorCenterBearing;
	}

	public void updateWarnThresholdTime(long warnThresholdTime) {
		this.warnThresholdTime = warnThresholdTime;

		for (int index = 0; index < getDistanceStepCount(); index++) {
			if (latestStrokeTimestamp[index] < warnThresholdTime) {
				latestStrokeTimestamp[index] = 0;
				strokeCount[index] = 0;
			}
		}
	}

    public long getWarnThresholdTime() {
        return warnThresholdTime;
    }
}
