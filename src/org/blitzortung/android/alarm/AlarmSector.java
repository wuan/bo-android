package org.blitzortung.android.alarm;


public class AlarmSector {
	private final static float DISTANCE_LIMITS[] = {10000, 25000, 50000, 100000, 250000, 500000};
	
	int count[];
	long latestTime[];
	
	float minDistance;
	float bearing;
	
	public AlarmSector(float bearing) {
		this.bearing = bearing;
		
		reset();
	}
	
	public void reset() {
		int size = DISTANCE_LIMITS.length;
		
		count = new int[size];
		latestTime = new long[size];
		
		minDistance = (float)Double.POSITIVE_INFINITY;
	}
	
	public void check(int multiplicity, float distance, long time) {
		
		minDistance = Math.min(distance,  minDistance);
		
		int index = 0;
		for (double distanceLimit : DISTANCE_LIMITS) {
			if (distance <= distanceLimit) {
				count[index] += multiplicity;
				if (time > latestTime[index]) {
					latestTime[index] = time;
				}
				//Log.v("AlarmSector", String.format("distance %.1f, limit %.0f, count %d", distance, distanceLimit, count[index]));
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
	
	public static float[] getDistanceLimits() {
		return DISTANCE_LIMITS;
	}
	
	public static int getDistanceLimitCount() {
		return DISTANCE_LIMITS.length;
	}

	public float getMinDistance() {
		return minDistance;
	}

	public float getBearing() {
		return bearing;
	}
}
