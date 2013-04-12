package org.blitzortung.android.alarm.object;

import org.blitzortung.android.alarm.AlarmParameters;
import org.blitzortung.android.alarm.factory.AlarmObjectFactory;

import java.util.ArrayList;
import java.util.Collection;

public class AlarmStatus {

    private final Collection<AlarmSector> sectors;

    public AlarmStatus(AlarmObjectFactory alarmObjectFactory, AlarmParameters alarmParameters) {
        final String[] sectorLabels = alarmParameters.getSectorLabels();
        float sectorWidth = 360f / sectorLabels.length;
        
        sectors = new ArrayList<AlarmSector>();

        float bearing = -180;
        for (String sectorLabel : sectorLabels) {
            float minimumSectorBearing = bearing - sectorWidth / 2.0f;
            minimumSectorBearing += (minimumSectorBearing < -180f ? 360f : 0f);
            AlarmSector alarmSector = alarmObjectFactory.createAlarmSector(alarmParameters, sectorLabel, minimumSectorBearing, bearing + sectorWidth / 2.0f);
            sectors.add(alarmSector);
            bearing += sectorWidth;
        }
    }

    public void clearResults() {
        for (AlarmSector sector : sectors) {
            sector.clearResults();
        }
    }

    public Collection<AlarmSector> getSectors() {
        return sectors;
    }
}
