package org.blitzortung.android.alarm;

public class AlarmResult {
	
	private final AlarmSector sector;

    private final String bearingName;
	
	public AlarmResult(AlarmSector sector, String bearingName) {
		this.sector = sector;
		this.bearingName = bearingName;
	}

	public float getClosestStrokeDistance() {
		return sector.getClosestStrokeDistance();
	}

    public String getDistanceUnitName() {
        return sector.getDistanceUnitName();
    }

    public String getBearingName() {
        return bearingName;
    }
}
