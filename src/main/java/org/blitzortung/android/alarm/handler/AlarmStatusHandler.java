package org.blitzortung.android.alarm.handler;

import android.location.Location;
import android.util.Log;
import org.blitzortung.android.alarm.AlarmParameters;
import org.blitzortung.android.alarm.AlarmResult;
import org.blitzortung.android.alarm.object.AlarmSector;
import org.blitzortung.android.alarm.object.AlarmStatus;
import org.blitzortung.android.app.Main;
import org.blitzortung.android.data.beans.Stroke;

import java.util.Collection;
import java.util.Locale;
import java.util.SortedMap;
import java.util.TreeMap;

public class AlarmStatusHandler {

    private final AlarmSectorHandler alarmSectorHandler;
    private final AlarmParameters alarmParameters;

    public AlarmStatusHandler(AlarmSectorHandler alarmSectorHandler, AlarmParameters alarmParameters) {
        this.alarmSectorHandler = alarmSectorHandler;
        this.alarmParameters = alarmParameters;
    }

    public AlarmStatus checkStrokes(AlarmStatus alarmStatus, Collection<? extends Stroke> strokes, Location location) {

        alarmStatus.clearResults();

        long thresholdTime = System.currentTimeMillis() - alarmParameters.getAlarmInterval();

        alarmSectorHandler.setCheckStrokeParameters(location, thresholdTime);

        Location strokeLocation = new Location("");

        for (Stroke stroke : strokes) {
            float bearingToStroke = location.bearingTo(stroke.getLocation(strokeLocation));

            AlarmSector alarmSector = getSectorForBearing(alarmStatus, bearingToStroke);
            alarmSectorHandler.checkStroke(alarmSector, stroke);
        }
        return alarmStatus;
    }

    public AlarmSector getSectorWithClosestStroke(AlarmStatus alarmStatus) {
        float minDistance = Float.POSITIVE_INFINITY;

        AlarmSector sectorWithClosestStroke = null;
        for (AlarmSector sector : alarmStatus.getSectors()) {
            if (sector.getClosestStrokeDistance() < minDistance) {
                minDistance = sector.getClosestStrokeDistance();
                sectorWithClosestStroke = sector;
            }
        }
        return sectorWithClosestStroke;
    }

    public AlarmResult getCurrentActivity(AlarmStatus alarmStatus) {
        AlarmSector sector = getSectorWithClosestStroke(alarmStatus);

        return sector != null ? new AlarmResult(sector, alarmParameters.getMeasurementSystem().getUnitName()) : null;
    }

    public String getTextMessage(AlarmStatus alarmStatus, float notificationDistanceLimit) {
        SortedMap<Float, AlarmSector> distanceSectors = getSectorsSortedByClosestStrokeDistance(alarmStatus, notificationDistanceLimit);

        StringBuilder sb = new StringBuilder();

        if (distanceSectors.size() > 0) {
            for (AlarmSector sector : distanceSectors.values()) {
                sb.append(sector.getLabel());
                sb.append(" ");
                sb.append(String.format("%.0f%s", sector.getClosestStrokeDistance(), alarmParameters.getMeasurementSystem().getUnitName()));
                sb.append(", ");
            }
            sb.setLength(sb.length() - 2);
        }

        return sb.toString();
    }

    private SortedMap<Float, AlarmSector> getSectorsSortedByClosestStrokeDistance(AlarmStatus alarmStatus, float notificationDistanceLimit) {
        SortedMap<Float, AlarmSector> distanceSectors = new TreeMap<Float, AlarmSector>();

        for (AlarmSector sector : alarmStatus.getSectors()) {
            if (sector.getClosestStrokeDistance() <= notificationDistanceLimit) {
                distanceSectors.put(sector.getClosestStrokeDistance(), sector);
            }
        }
        return distanceSectors;
    }

    private AlarmSector getSectorForBearing(AlarmStatus alarmStatus, double bearing) {
        for (AlarmSector sector : alarmStatus.getSectors()) {
            if (sectorContainsBearing(sector, bearing)) {
                return sector;
            }
        }
        Log.w(Main.LOG_TAG, String.format(Locale.ENGLISH, "AlarmStatusHandler.getSectorForBearing(): no sector for bearing %.2f found", bearing));
        return null;
    }

    private boolean sectorContainsBearing(AlarmSector sector, double bearing) {
        float minimumSectorBearing = sector.getMinimumSectorBearing();
        float maximumSectorBearing = sector.getMaximumSectorBearing();

        if (maximumSectorBearing > minimumSectorBearing) {
            return bearing < maximumSectorBearing && bearing >= minimumSectorBearing;
        } else {
            return bearing >= minimumSectorBearing || bearing < maximumSectorBearing;
        }
    }
}
