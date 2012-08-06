package org.blitzortung.android.alarm;

public class AlarmResult {

	private final int range;
	
	private final int sector;
	
	private final float distance;
	
	public AlarmResult(int range, int sector, float distance) {
		this.range = range;
		this.sector = sector;
		this.distance = distance;
	}

	public int getRange() {
		return range;
	}

	public int getSector() {
		return sector;
	}

	public float getDistance() {
		return distance;
	}
}
