package org.blitzortung.android.time;

public class RangeHandler {

    private int intervalDuration;

    private int intervalOffset;

    private int offsetIncrement;

    private final int maxRange;

    public RangeHandler() {
        maxRange = 24 * 60;
        intervalDuration = 60;
        intervalOffset = 0;
        offsetIncrement = 15;
    }

    public int getIntervalDuration() {
        return intervalDuration;
    }

    public boolean setIntervalDuration(int intervalDuration) {
        boolean valueChanged = this.intervalDuration != intervalDuration;
        this.intervalDuration = intervalDuration;
        return valueChanged;
    }

    public void setOffsetIncrement(int offsetIncrement) {
        this.offsetIncrement = offsetIncrement;
    }

    public int getIntervalOffset() {
        return intervalOffset;
    }

    public boolean revInterval() {
        alignIntervalOffsetWithIncrement();

        intervalOffset -= offsetIncrement;

        if (intervalOffset < -maxRange + intervalDuration) {
            intervalOffset = -maxRange + intervalDuration;
            alignIntervalOffsetWithIncrement();
            return false;
        } else {
            return true;
        }
    }

    public boolean ffwdInterval() {
        alignIntervalOffsetWithIncrement();

        intervalOffset += offsetIncrement;

        if (intervalOffset >0) {
            intervalOffset = 0;
            return false;
        } else {
            return true;
        }
    }

    public boolean isRealtime() {
        return intervalOffset == 0;
    }

    public boolean goRealtime() {
        boolean wasRealtime = isRealtime();

        intervalOffset = 0;

        return !wasRealtime;
    }

    private void alignIntervalOffsetWithIncrement() {
        intervalOffset = (intervalOffset / offsetIncrement ) * offsetIncrement;
    }
}
