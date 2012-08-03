package org.blitzortung.android.alarm;

public class AlarmSector {
	private static final float DISTANCE_LIMITS[] = { 10000, 25000, 50000, 100000, 250000, 500000 };

	int count[];
	long latestTime[];
	float minimumDistance;

	final float sectorCenterBearing;
	long warnThresholdTime;

	public AlarmSector(float sectorCenterBearing, long warnThresholdTime) {
		this.warnThresholdTime = warnThresholdTime;
		this.sectorCenterBearing = sectorCenterBearing;

		reset();
	}

	public void reset() {
		int size = getSize();

		count = new int[size];
		latestTime = new long[size];
		minimumDistance = (float) Double.POSITIVE_INFINITY;
	}

	public void check(int multiplicity, float distance, long time) {

		int index = 0;
		for (double distanceLimit : DISTANCE_LIMITS) {
			if (distance <= distanceLimit) {
				count[index] += multiplicity;
				if (time > latestTime[index]) {
					latestTime[index] = time;
				}
				if (time > warnThresholdTime) {
					minimumDistance = Math.min(distance, minimumDistance);
				}
				break;
			}
			index++;
		}
	}

	public int[] getEventCounts() {
		return count;
	}

	public long[] getLatestTimes() {
		return latestTime;
	}

	public int getMinimumIndex() {
		for (int index = 0; index < getSize(); index++) {
			if (count[index] > 0) {
				return index;
			}
		}
		return -1;
	}

	public int getCount(int index) {
		return count[index];
	}

	public long getLatestTime(int index) {
		return latestTime[index];
	}

	public float getMinimumDistance() {
		return minimumDistance;
	}

	private int getSize() {
		return DISTANCE_LIMITS.length;
	}

	public static float[] getDistanceLimits() {
		return DISTANCE_LIMITS;
	}

	public static int getDistanceLimitCount() {
		return DISTANCE_LIMITS.length;
	}

	public float getBearing() {
		return sectorCenterBearing;
	}

	public void updateThreshold(long warnThresholdTime) {
		this.warnThresholdTime = warnThresholdTime;
		for (int index = 0; index < getSize(); index++) {
			if (latestTime[index] < warnThresholdTime) {
				latestTime[index] = 0;
				count[index] = 0;
			}
		}
	}
}
