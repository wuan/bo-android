package org.blitzortung.android.util;

public class Period {

    private long lastUpdateTime;
    private int updateCount;

    public Period() {
        restart();
    }

    public static long getCurrentTime() {
        return System.currentTimeMillis() / 1000;
    }

    public boolean shouldUpdate(long currentTime, int currentPeriod) {
        if (lastUpdateTime == 0) {
            lastUpdateTime = currentTime;
            return true;
        }
        return currentTime >= lastUpdateTime + currentPeriod;
    }

    public long getLastUpdateTime() {
        return lastUpdateTime;
    }

    public void setLastUpdateTime(long lastUpdateTime) {
        this.lastUpdateTime = lastUpdateTime;
    }

    public boolean isNthUpdate(int countPeriod) {
        return (updateCount % countPeriod) == 0;
    }

    public long getCurrentUpdatePeriod(long currentTime, int currentPeriod) {
        return currentPeriod - (currentTime - lastUpdateTime);
    }

    public void restart() {
        lastUpdateTime = 0;
        updateCount = 0;
    }

}
