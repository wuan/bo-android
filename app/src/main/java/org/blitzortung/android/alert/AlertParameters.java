package org.blitzortung.android.alert;

import android.content.Context;

import org.blitzortung.android.app.R;
import org.blitzortung.android.util.MeasurementSystem;

public class AlertParameters {
    private static final float[] RANGE_STEPS = {10, 25, 50, 100, 250, 500};
    private static final long alarmInterval = 10 * 60 * 1000;
    private static String[] SECTOR_LABELS;
    private MeasurementSystem measurementSystem;

    public void updateSectorLabels(Context context) {
        SECTOR_LABELS = context.getResources().getStringArray(R.array.direction_names);
    }

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
