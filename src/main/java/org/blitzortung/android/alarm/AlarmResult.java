package org.blitzortung.android.alarm;

import org.blitzortung.android.alarm.object.AlarmSector;

public class AlarmResult {
	
	private final AlarmSector sector;
    private final String distanceUnitName;

    public AlarmResult(AlarmSector sector, String distanceUnitName) {
		this.sector = sector;
        this.distanceUnitName = distanceUnitName;
    }

	public float getClosestStrokeDistance() {
		return sector.getClosestStrokeDistance();
	}

    public String getDistanceUnitName() {
        return distanceUnitName;
    }

    public String getBearingName() {
        return sector.getLabel();
    }
}
