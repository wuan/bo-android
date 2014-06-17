package org.blitzortung.android.alert.factory;

import org.blitzortung.android.alert.AlertParameters;
import org.blitzortung.android.alert.handler.AlertSectorHandler;
import org.blitzortung.android.alert.handler.AlertStatusHandler;
import org.blitzortung.android.alert.object.AlertSector;
import org.blitzortung.android.alert.object.AlertSectorRange;
import org.blitzortung.android.alert.object.AlertStatus;

public class AlertObjectFactory {
    
    public AlertObjectFactory() {
    }
    
    public AlertStatus createAlarmStatus(AlertParameters alertParameters) {
        return new AlertStatus(this, alertParameters);
    }

    public AlertSector createAlarmSector(AlertParameters alertParameters, String sectorLabel, float minimumSectorBearing, float maximumSectorBearing) {
        return new AlertSector(this, alertParameters, sectorLabel, minimumSectorBearing, maximumSectorBearing);
    }

    public AlertSectorRange createAlarmSectorRange(float rangeMinimum, float rangeMaximum) {
        return new AlertSectorRange(rangeMinimum, rangeMaximum);
    }

    public AlertStatusHandler createAlarmStatusHandler(AlertParameters alertParameters) {
        return new AlertStatusHandler(createAlarmSectorHandler(alertParameters), alertParameters);
    }

    public AlertSectorHandler createAlarmSectorHandler(AlertParameters alertParameters) {
        return new AlertSectorHandler(alertParameters);
    }
}
