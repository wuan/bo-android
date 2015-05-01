package org.blitzortung.android.alert.handler;

import android.location.Location;
import android.util.Log;
import org.blitzortung.android.alert.AlertParameters;
import org.blitzortung.android.alert.AlertResult;
import org.blitzortung.android.alert.object.AlertSector;
import org.blitzortung.android.alert.object.AlertStatus;
import org.blitzortung.android.app.Main;
import org.blitzortung.android.data.beans.Strike;

import java.util.Collection;
import java.util.Locale;
import java.util.SortedMap;
import java.util.TreeMap;

public class AlertStatusHandler {

    private final AlertSectorHandler alertSectorHandler;
    private final AlertParameters alertParameters;

    public AlertStatusHandler(AlertSectorHandler alertSectorHandler, AlertParameters alertParameters) {
        this.alertSectorHandler = alertSectorHandler;
        this.alertParameters = alertParameters;
    }

    public AlertStatus checkStrikes(AlertStatus alertStatus, Collection<? extends Strike> strikes, Location location) {

        alertStatus.clearResults();

        long thresholdTime = System.currentTimeMillis() - alertParameters.getAlarmInterval();

        alertSectorHandler.setCheckStrikeParameters(location, thresholdTime);

        Location strikeLocation = new Location("");

        for (Strike strike : strikes) {
            float bearingToStrike = location.bearingTo(strike.getLocation(strikeLocation));

            AlertSector alertSector = getSectorForBearing(alertStatus, bearingToStrike);
            alertSectorHandler.checkStrike(alertSector, strike);
        }
        return alertStatus;
    }


    public long getLatestTimstampWithin(float distanceLimit, AlertStatus alertStatus) {
        long latestTimestamp = 0;
        
        for (AlertSector sector : alertStatus.getSectors()) {
            latestTimestamp = Math.max(latestTimestamp, alertSectorHandler.getLatestTimestampWithin(distanceLimit, sector));
        }
        
        return latestTimestamp;
    }
    
    public AlertSector getSectorWithClosestStrike(AlertStatus alertStatus) {
        float minDistance = Float.POSITIVE_INFINITY;

        AlertSector sectorWithClosestStrike = null;
        for (AlertSector sector : alertStatus.getSectors()) {
            if (sector.getClosestStrikeDistance() < minDistance) {
                minDistance = sector.getClosestStrikeDistance();
                sectorWithClosestStrike = sector;
            }
        }
        return sectorWithClosestStrike;
    }

    public AlertResult getCurrentActivity(AlertStatus alertStatus) {
        AlertSector sector = getSectorWithClosestStrike(alertStatus);

        return sector != null ? new AlertResult(sector, alertParameters.getMeasurementSystem().getUnitName()) : null;
    }

    public String getTextMessage(AlertStatus alertStatus, float notificationDistanceLimit) {
        SortedMap<Float, AlertSector> distanceSectors = getSectorsSortedByClosestStrikeDistance(alertStatus, notificationDistanceLimit);

        StringBuilder sb = new StringBuilder();

        if (distanceSectors.size() > 0) {
            for (AlertSector sector : distanceSectors.values()) {
                sb.append(sector.getLabel());
                sb.append(" ");
                sb.append(String.format("%.0f%s", sector.getClosestStrikeDistance(), alertParameters.getMeasurementSystem().getUnitName()));
                sb.append(", ");
            }
            sb.setLength(sb.length() - 2);
        }

        return sb.toString();
    }

    private SortedMap<Float, AlertSector> getSectorsSortedByClosestStrikeDistance(AlertStatus alertStatus, float notificationDistanceLimit) {
        SortedMap<Float, AlertSector> distanceSectors = new TreeMap<Float, AlertSector>();

        for (AlertSector sector : alertStatus.getSectors()) {
            if (sector.getClosestStrikeDistance() <= notificationDistanceLimit) {
                distanceSectors.put(sector.getClosestStrikeDistance(), sector);
            }
        }
        return distanceSectors;
    }

    private AlertSector getSectorForBearing(AlertStatus alertStatus, double bearing) {
        for (AlertSector sector : alertStatus.getSectors()) {
            if (sectorContainsBearing(sector, bearing)) {
                return sector;
            }
        }
        Log.w(Main.LOG_TAG, String.format(Locale.ENGLISH, "AlarmStatusHandler.getSectorForBearing(): no sector for bearing %.2f found", bearing));
        return null;
    }

    private boolean sectorContainsBearing(AlertSector sector, double bearing) {
        float minimumSectorBearing = sector.getMinimumSectorBearing();
        float maximumSectorBearing = sector.getMaximumSectorBearing();

        if (maximumSectorBearing > minimumSectorBearing) {
            return bearing < maximumSectorBearing && bearing >= minimumSectorBearing;
        } else {
            return bearing >= minimumSectorBearing || bearing < maximumSectorBearing;
        }
    }
}
