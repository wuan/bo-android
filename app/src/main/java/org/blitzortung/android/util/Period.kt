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

package org.blitzortung.android.util

class Period {
    internal var lastUpdateTime: Long = 0L

    internal var updateCount: Int = 0
        private set

    init {
        restart()
    }

    fun shouldUpdate(
        currentTime: Long,
        currentPeriod: Int,
    ): Boolean =
        if (currentTime >= lastUpdateTime + currentPeriod) {
            updateCount++
            lastUpdateTime = currentTime
            true
        } else {
            false
        }

    fun isNthUpdate(countPeriod: Int): Boolean = (updateCount % countPeriod) == 0

    fun getCurrentUpdatePeriod(
        currentTime: Long,
        currentPeriod: Int,
    ): Long = currentPeriod - (currentTime - lastUpdateTime)

    fun restart() {
        lastUpdateTime = 0
        updateCount = 0
    }

    companion object {
        val currentTime: Long
            get() = System.currentTimeMillis() / 1000
    }
}
