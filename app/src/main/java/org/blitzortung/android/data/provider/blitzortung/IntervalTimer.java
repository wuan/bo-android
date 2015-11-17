package org.blitzortung.android.data.provider.blitzortung;

public class IntervalTimer {

    private final long intervalLength;
    private long currentTime;
    private long endTime;

    public IntervalTimer(long intervalLength) {
        this.intervalLength = intervalLength;
    }

    public long roundTime(long time) {
        return time / intervalLength * intervalLength;
    }

    public void startInterval(long startTime) {
        currentTime = roundTime(startTime);
        endTime = roundTime(System.currentTimeMillis());
    }

    public boolean hasNext() {
        return currentTime <= endTime;
    }

    public long next() {
        long currentTimeCopy = currentTime;
        currentTime += intervalLength;
        return currentTimeCopy;
    }
}
