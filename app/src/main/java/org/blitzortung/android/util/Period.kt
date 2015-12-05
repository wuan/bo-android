package org.blitzortung.android.util

class Period {

    var lastUpdateTime: Long = 0L
    private var updateCount: Int = 0

    init {
        restart()
    }

    fun shouldUpdate(currentTime: Long, currentPeriod: Int): Boolean {
        if (lastUpdateTime == 0L) {
            lastUpdateTime = currentTime
            return true
        }
        return currentTime >= lastUpdateTime + currentPeriod
    }

    fun isNthUpdate(countPeriod: Int): Boolean {
        return (updateCount % countPeriod) == 0
    }

    fun getCurrentUpdatePeriod(currentTime: Long, currentPeriod: Int): Long {
        return currentPeriod - (currentTime - lastUpdateTime)
    }

    fun restart() {
        lastUpdateTime = 0
        updateCount = 0
    }

    companion object {
        val currentTime: Long
            get() = System.currentTimeMillis() / 1000
    }

}
