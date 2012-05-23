package org.blitzortung.android.alarm;

import android.util.Log;

public class AlarmStatus {

	private final static String[] DIRECTION_LABELS = { "S", "SW", "W", "NW", "N", "NO", "O", "SO" };

	private AlarmSector sectors[];

	public AlarmStatus() {
		int size = DIRECTION_LABELS.length;

		sectors = new AlarmSector[size];

		float bearing = -180;
		for (int i = 0; i < size; i++) {
			sectors[i] = new AlarmSector(bearing);
			bearing += getSectorSize();
		}
	}

	public void check(int multiplicity, float distance, float bearing, long time) {
		// Log.v("AlarmStatus", String.format("check() #%d, %.1f %.1f¡",
		// multiplicity, distance, bearing));
		int sector = getSectorNumber(bearing);

		sectors[sector].check(multiplicity, distance, time);
	}

	private int getSectorNumber(double bearing) {
		return ((int) (Math.round(bearing / getSectorSize())) + getSectorCount() / 2) % getSectorCount();
	}

	public float getSectorSize() {
		return 360.0f / getSectorCount();
	}

	public int getSectorCount() {
		return DIRECTION_LABELS.length;
	}

	public String getSectorLabel(int sectorNumber) {
		return DIRECTION_LABELS[sectorNumber];
	}

	public void reset() {
		for (AlarmSector sector : sectors) {
			sector.reset();
		}
	}

	public int getSectorWithClosestStroke() {
		double minDistance = Double.POSITIVE_INFINITY;
		int sectorIndex = -1;

		int index = 0;
		for (AlarmSector sector : sectors) {
			if (sector.getMinDistance() < minDistance) {
				minDistance = sector.getMinDistance();
				sectorIndex = index;
			}
			index++;
		}
		return sectorIndex;
	}

	public AlarmSector getSector(int index) {
		return sectors[index];
	}

	public double getClosestStrokeDistance(int alarmSector) {
		return sectors[alarmSector].getMinDistance();
	}

	public float getSectorBearing(int alarmSector) {
		return sectors[alarmSector].getBearing();
	}

	public AlarmResult currentActivity(long time) {
		for (int range = 0; range < AlarmSector.getDistanceLimitCount(); range++) {
			for (int index=0; index < sectors.length; index++) {
				//Log.v("AlarmStatus", "currentActivity " + range + " " + sectors[index].getEventCounts()[range] + " " + sectors[index].getLatestTimes()[range]);
				if (sectors[index].getEventCounts()[range] > 0) {
					if (sectors[index].getLatestTimes()[range] > time) {
						return new AlarmResult(range, index, sectors[index].getDistance(range));
					}
				}
			}
		}
		return null;
	}
}
