package org.blitzortung.android.alarm;

import org.blitzortung.android.alarm.object.AlarmSector;
import org.blitzortung.android.util.MeasurementSystem;

public class AlarmResult {
	
	private final AlarmSector sector;
    private final MeasurementSystem measurementSystem;

    public AlarmResult(AlarmSector sector, MeasurementSystem measurementSystem) {
		this.sector = sector;
        this.measurementSystem = measurementSystem;
    }

	public float getClosestStrokeDistance() {
		return sector.getClosestStrokeDistance();
	}

    public String getDistanceUnitName() {
        return measurementSystem.getUnitName();
    }

    public String getBearingName() {
        return sector.getLabel();
    }
}
