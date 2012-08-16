package org.blitzortung.android.alarm;

import android.location.Location;
import org.blitzortung.android.data.beans.AbstractStroke;

import java.util.SortedMap;
import java.util.TreeMap;

public class AlarmStatus {

    private final static String[] DIRECTION_LABELS = {"S", "SW", "W", "NW", "N", "NO", "O", "SO"};

    private final static int SECTOR_COUNT = DIRECTION_LABELS.length;

    // VisibleForTesting
    protected final AlarmSector[] sectors;

    public AlarmStatus(long warnThresholdTime) {
        sectors = new AlarmSector[SECTOR_COUNT];

        float bearing = -180;
        for (int i = 0; i < SECTOR_COUNT; i++) {
            sectors[i] = new AlarmSector(bearing, warnThresholdTime);
            bearing += getSectorSize();
        }
    }

    public void updateWarnThresholdTime(long warnThresholdTime) {
        for (int i = 0; i < SECTOR_COUNT; i++) {
            sectors[i].updateWarnThresholdTime(warnThresholdTime);
        }
    }

    public void check(Location location, AbstractStroke stroke) {
        int sector = getSectorNumber(location.bearingTo(stroke.getLocation()));

        sectors[sector].check(location, stroke);
    }

    private int getSectorNumber(double bearing) {
        return ((int) (Math.round(bearing / getSectorSize())) + getSectorCount() / 2) % getSectorCount();
    }

    public float getSectorSize() {
        return 360.0f / getSectorCount();
    }

    public int getSectorCount() {
        return DIRECTION_LABELS.length;
    }

    public String getSectorLabel(int sectorNumber) {
        return DIRECTION_LABELS[sectorNumber];
    }

    public void reset() {
        for (AlarmSector sector : sectors) {
            sector.reset();
        }
    }

    public int getSectorWithClosestStroke() {
        double minDistance = Double.POSITIVE_INFINITY;
        int sectorIndex = -1;

        int index = 0;
        for (AlarmSector sector : sectors) {
            if (sector.getWarnMinimumDistance() < minDistance) {
                minDistance = sector.getWarnMinimumDistance();
                sectorIndex = index;
            }
            index++;
        }
        return sectorIndex;
    }

    public AlarmSector getSector(int index) {
        return sectors[index];
    }

    public float getClosestStrokeDistance() {
        int alarmSector = getSectorWithClosestStroke();
        if (alarmSector >= 0) {
            return sectors[alarmSector].getWarnMinimumDistance();
        } else {
            return Float.POSITIVE_INFINITY;
        }
    }

    public float getSectorBearing(int alarmSector) {
        return sectors[alarmSector].getBearing();
    }

    public AlarmResult currentActivity() {
        int closestStrokeSectorIndex = getSectorWithClosestStroke();

        if (closestStrokeSectorIndex >= 0) {
            AlarmSector sector = sectors[closestStrokeSectorIndex];
            return new AlarmResult(sector.getMinimumIndex(), closestStrokeSectorIndex, sector.getWarnMinimumDistance());
        }

        return null;
    }

    public String getTextMessage(float notificationDistanceLimit) {
        SortedMap<Float, Integer> distanceSectors = new TreeMap<Float, Integer>();

        for (int sectorIndex = 0; sectorIndex < getSectorCount(); sectorIndex++) {
            AlarmSector sector = getSector(sectorIndex);
            if (sector.getWarnMinimumDistance() <= notificationDistanceLimit) {
                distanceSectors.put(sector.getWarnMinimumDistance(), sectorIndex);
            }
        }
        StringBuilder sb = new StringBuilder();

        if (distanceSectors.size() > 0) {
            for (int sectorIndex : distanceSectors.values()) {
                AlarmSector sector = getSector(sectorIndex);
                sb.append(getSectorLabel(sectorIndex));
                sb.append(" ");
                sb.append(String.format("%.0fkm", sector.getWarnMinimumDistance() / 1000.0));
                sb.append(", ");
            }
            sb.setLength(sb.length() - 2);
        }

        return sb.toString();
    }
}
