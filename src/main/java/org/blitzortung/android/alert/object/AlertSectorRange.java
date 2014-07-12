package org.blitzortung.android.alert.object;

import org.blitzortung.android.data.beans.Strike;

public class AlertSectorRange {

    private final float rangeMinimum;
    
    private final float rangeMaximum;
    
    private int strikeCount;
    
    private long latestStrikeTimestamp;

    public AlertSectorRange(float rangeMinimum, float rangeMaximum) {
        this.rangeMinimum = rangeMinimum;
        this.rangeMaximum = rangeMaximum;
    }
    
    public void clearResults()
    {
        strikeCount = 0;
        latestStrikeTimestamp = 0;
    }

    public float getRangeMinimum() {
        return rangeMinimum;
    }

    public float getRangeMaximum() {
        return rangeMaximum;
    }

    public int getStrikeCount() {
        return strikeCount;
    }

    public long getLatestStrikeTimestamp() {
        return latestStrikeTimestamp;
    }

    public void addStrike(Strike strike) {
        updateLatestStrikeTimestamp(strike.getTimestamp());
        incrementStrikeCountBy(strike.getMultiplicity());
    }

    private void updateLatestStrikeTimestamp(long latestStrikeTimestamp) {
        if (latestStrikeTimestamp > this.getLatestStrikeTimestamp()) {
            this.latestStrikeTimestamp = latestStrikeTimestamp;
        }
    }

    private void incrementStrikeCountBy(int increment) {
        strikeCount += increment;
    }
}
