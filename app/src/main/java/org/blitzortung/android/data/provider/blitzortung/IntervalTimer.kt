package org.blitzortung.android.data.provider.blitzortung

class IntervalTimer(private val intervalLength: Long) {
    private var currentTime: Long = 0
    private var endTime: Long = 0

    fun roundTime(time: Long): Long {
        return time / intervalLength * intervalLength
    }

    fun startInterval(startTime: Long) {
        currentTime = roundTime(startTime)
        endTime = roundTime(System.currentTimeMillis())
    }

    operator fun hasNext(): Boolean {
        return currentTime <= endTime
    }

    operator fun next(): Long {
        val currentTimeCopy = currentTime
        currentTime += intervalLength
        return currentTimeCopy
    }
}
