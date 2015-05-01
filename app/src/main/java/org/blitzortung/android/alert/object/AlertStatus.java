package org.blitzortung.android.alert.object;

import org.blitzortung.android.alert.AlertParameters;
import org.blitzortung.android.alert.factory.AlertObjectFactory;

import java.util.ArrayList;
import java.util.Collection;

public class AlertStatus {

    private final Collection<AlertSector> sectors;
    private final AlertParameters alertParameters;

    public AlertStatus(AlertObjectFactory alertObjectFactory, AlertParameters alertParameters) {
        this.alertParameters = alertParameters;
        final String[] sectorLabels = alertParameters.getSectorLabels();
        float sectorWidth = 360f / sectorLabels.length;
        
        sectors = new ArrayList<AlertSector>();

        float bearing = -180;
        for (String sectorLabel : sectorLabels) {
            float minimumSectorBearing = bearing - sectorWidth / 2.0f;
            minimumSectorBearing += (minimumSectorBearing < -180f ? 360f : 0f);
            final float maximumSectorBearing = bearing + sectorWidth / 2.0f;
            AlertSector alertSector = alertObjectFactory.createAlarmSector(alertParameters, sectorLabel, minimumSectorBearing, maximumSectorBearing);
            sectors.add(alertSector);
            bearing += sectorWidth;
        }
    }

    public void clearResults() {
        for (AlertSector sector : sectors) {
            sector.clearResults();
        }
    }

    public Collection<AlertSector> getSectors() {
        return sectors;
    }

    public AlertParameters getAlertParameters() {
        return alertParameters;
    }
}
