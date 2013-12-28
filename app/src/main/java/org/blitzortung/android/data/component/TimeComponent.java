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
}
