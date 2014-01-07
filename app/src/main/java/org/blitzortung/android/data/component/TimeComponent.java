package org.blitzortung.android.data.component;

public class TimeComponent {
    private final int intervalDuration;
    private final int intervalOffset;
    private final long referenceTime;
    private final boolean incremental;

    public TimeComponent(int intervalDuration, int intervalOffset, long referenceTime, boolean incremental) {
        this.intervalDuration = intervalDuration;
        this.intervalOffset = intervalOffset;
        this.referenceTime = referenceTime;
        this.incremental = incremental;
    }

    public boolean isRealtime() {
        return intervalOffset == 0;
    }

    public long getReferenceTime() {
        return referenceTime;
    }

    public int getIntervalDuration() {
        return intervalDuration;
    }

    public int getIntervalOffset() {
        return intervalOffset;
    }

    public boolean isIncremental() {
        return incremental;
    }
}
