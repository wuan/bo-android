package org.blitzortung.android.alarm.object;

import org.blitzortung.android.data.beans.Stroke;

public class AlarmSectorRange {

    private final float rangeMinimum;
    
    private final float rangeMaximum;
    
    private int strokeCount;
    
    private long latestStrokeTimestamp;

    public AlarmSectorRange(float rangeMinimum, float rangeMaximum) {
        this.rangeMinimum = rangeMinimum;
        this.rangeMaximum = rangeMaximum;
    }
    
    public void clearResults()
    {
        strokeCount = 0;
        latestStrokeTimestamp = 0;  
    }

    public float getRangeMinimum() {
        return rangeMinimum;
    }

    public float getRangeMaximum() {
        return rangeMaximum;
    }

    public int getStrokeCount() {
        return strokeCount;
    }

    public long getLatestStrokeTimestamp() {
        return latestStrokeTimestamp;
    }

    public void addStroke(Stroke stroke) {
        updateLatestStrokeTimestamp(stroke.getTimestamp());
        incrementStrokeCountBy(stroke.getMultiplicity());
    }

    private void updateLatestStrokeTimestamp(long latestStrokeTimestamp) {
        if (latestStrokeTimestamp > this.getLatestStrokeTimestamp()) {
            this.latestStrokeTimestamp = latestStrokeTimestamp;
        }
    }

    private void incrementStrokeCountBy(int increment) {
        strokeCount += increment;
    }
}
