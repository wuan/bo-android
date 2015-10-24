package org.blitzortung.android.alert;

import org.blitzortung.android.alert.data.AlertSector;

public class AlertResult {
	
	private final AlertSector sector;
    
    private final String distanceUnitName;

    public AlertResult(AlertSector sector, String distanceUnitName) {
		this.sector = sector;
        this.distanceUnitName = distanceUnitName;
    }

	public float getClosestStrikeDistance() {
		return sector.getClosestStrikeDistance();
	}

    public String getDistanceUnitName() {
        return distanceUnitName;
    }

    public String getBearingName() {
        return sector.getLabel();
    }
    
    @Override
    public String toString() {
        return String.format("%s %.1f %s", getBearingName(), getClosestStrikeDistance(), getDistanceUnitName());
    }
}
