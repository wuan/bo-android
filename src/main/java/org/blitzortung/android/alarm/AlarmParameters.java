package org.blitzortung.android.alarm;

import org.blitzortung.android.util.MeasurementSystem;

public class AlarmParameters {
    private final static String[] SECTOR_LABELS = {"S", "SW", "W", "NW", "N", "NO", "O", "SO"};

    private static final float[] RANGE_STEPS = {10, 25, 50, 100, 250, 500};

    private static final long alarmInterval = 10 * 60 * 1000;
    
    private MeasurementSystem measurementSystem;
    
    public String[] getSectorLabels() {
        return SECTOR_LABELS;
    }

    public float[] getRangeSteps() {
        return RANGE_STEPS;
    }
    
    public long getAlarmInterval() {
        return alarmInterval;
    }

    public MeasurementSystem getMeasurementSystem() {
        return measurementSystem;
    }

    public void setMeasurementSystem(MeasurementSystem measurementSystem) {
        this.measurementSystem = measurementSystem;
    }
}
