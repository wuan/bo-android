package org.blitzortung.android.data;

public class Parameters {

    private int region = -1;

    private int rasterBaselength;

    private int intervalDuration;

    private int intervalOffset;

    private int offsetIncrement;

    private final int maxRange;
    private int countThreshold;

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

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Parameter(");
        sb.append("region ").append(region).append(", ");
        sb.append("duration ").append(intervalDuration).append(", ");
        sb.append("offset ").append(intervalOffset).append(")");
        
        return sb.toString();
    }

    public void setCountThreshold(int countThreshold) {
        this.countThreshold = countThreshold;
    }

    public int getCountThreshold() {
        return countThreshold;
    }
}
