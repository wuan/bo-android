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

package org.blitzortung.android.data.beans

class Station(
        override val longitude: Double,
        override val latitude: Double,
        val name: String,
        val offlineSince: Long
) : Location {

    val state: State
        get() {
            if (offlineSince == OFFLINE_SINCE_NOT_SET) {
                return State.ON
            } else {
                val now = System.currentTimeMillis()

                val minutesAgo = (now - offlineSince) / 1000 / 60

                if (minutesAgo > 24 * 60) {
                    return State.OFF
                } else if (minutesAgo > 15) {
                    return State.DELAYED
                } else {
                    return State.ON
                }
            }
        }

    enum class State {
        ON, DELAYED, OFF
    }

    companion object {
        val OFFLINE_SINCE_NOT_SET: Long = -1
    }
}
