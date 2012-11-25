package org.blitzortung.android.data;

import android.util.Log;

public class Parameters {

    private int region;

    private int rasterBaselength;

    private int intervalDuration;

    private int intervalOffset;

    private int offsetIncrement;

    private final int maxRange;

    public Parameters() {
        maxRange = 24 * 60;
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

    public void setIntervalOffset(int intervalOffset) {
        this.intervalOffset = intervalOffset;
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

    public int getRegion() {
        return region;
    }

    public void setRegion(int region) {
        this.region = region;
    }

    public int getRasterBaselength() {
        return rasterBaselength;
    }

    public void setRasterBaselength(int rasterBaselength) {
        this.rasterBaselength = rasterBaselength;
    }

    @Override
    public boolean equals(Object other) {
        if (other != null && other instanceof Parameters) {
            Parameters otherParameters = (Parameters) other;

            return intervalDuration == otherParameters.intervalDuration &&
                    intervalOffset == otherParameters.intervalOffset &&
                    region == otherParameters.region &&
                    rasterBaselength == otherParameters.rasterBaselength;
        }
        return false;
    }

}
