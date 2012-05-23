package org.blitzortung.android.alarm;


public class AlarmSector {
	private static final float DISTANCE_LIMITS[] = {10000, 25000, 50000, 100000, 250000, 500000};
	
	int count[];
	long latestTime[];
	float distances[];
	
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
		distances = new float[size];
		
		for (int i=0; i<size; i++) {
			distances[i] = (float)Double.POSITIVE_INFINITY;
		}
		
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
				distances[index] = Math.min(distance,  distances[index]);
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
	
	public float getDistance(int index) {
		return distances[index];
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
