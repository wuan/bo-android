package org.blitzortung.android.alarm.factory;

import org.blitzortung.android.alarm.AlarmParameters;
import org.blitzortung.android.alarm.handler.AlarmSectorHandler;
import org.blitzortung.android.alarm.handler.AlarmStatusHandler;
import org.blitzortung.android.alarm.object.AlarmSector;
import org.blitzortung.android.alarm.object.AlarmSectorRange;
import org.blitzortung.android.alarm.object.AlarmStatus;

public class AlarmObjectFactory {
    
    public AlarmObjectFactory() {
    }
    
    public AlarmStatus createAlarmStatus(AlarmParameters alarmParameters) {
        return new AlarmStatus(this, alarmParameters);
    }

    public AlarmSector createAlarmSector(AlarmParameters alarmParameters, String sectorLabel, float minimumSectorBearing, float maximumSectorBearing) {
        return new AlarmSector(this, alarmParameters, sectorLabel, minimumSectorBearing, maximumSectorBearing);
    }

    public AlarmSectorRange createAlarmSectorRange(float rangeMinimum, float rangeMaximum) {
        return new AlarmSectorRange(rangeMinimum, rangeMaximum);
    }

    public AlarmStatusHandler createAlarmStatusHandler(AlarmParameters alarmParameters) {
        return new AlarmStatusHandler(createAlarmSectorHandler(alarmParameters), alarmParameters);
    }

    public AlarmSectorHandler createAlarmSectorHandler(AlarmParameters alarmParameters) {
        return new AlarmSectorHandler(alarmParameters);
    }
}
