/*

   Copyright 2015 Andreas Würl

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.

*/

package org.blitzortung.android.data.provider.blitzortung

class TimestampIterator(
    private val intervalLength: Long,
    startTime: Long,
    private val endTime: Long
) : Iterator<Long> {

    private var currentTime: Long = roundTime(startTime)

    private fun roundTime(time: Long): Long {
        return time / intervalLength * intervalLength
    }

    override fun hasNext(): Boolean {
        return currentTime <= endTime
    }

    override fun next(): Long {
        if (!this.hasNext()) {
            throw NoSuchElementException()
        }
        val currentTimeCopy = currentTime
        currentTime += intervalLength
        return currentTimeCopy
    }
}

/**
 * Values of the Timestamp-Sequence are lazy generated, so we provide a Sequence for it
 * @param intervalLength Interval length
 * @param startTime Start time of the sequence
 */
internal fun createTimestampSequence(intervalLength: Long, startTime: Long, endTime: Long): Sequence<Long> {
    return Sequence { TimestampIterator(intervalLength, startTime, endTime) }
}
