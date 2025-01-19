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

import java.io.Serializable

class Station(
    override val longitude: Double,
    override val latitude: Double,
    val name: String,
    val offlineSince: Long
) : Location, Serializable {

    val state: State
        get() {
            return if (offlineSince == OFFLINE_SINCE_NOT_SET) {
                State.ON
            } else {
                val now = System.currentTimeMillis()

                val minutesAgo = (now - offlineSince) / 1000 / 60

                when {
                    minutesAgo > 24 * 60 -> {
                        State.OFF
                    }

                    minutesAgo > 15 -> {
                        State.DELAYED
                    }

                    else -> {
                        State.ON
                    }
                }
            }
        }

    enum class State {
        ON, DELAYED, OFF
    }

    companion object {
        const val OFFLINE_SINCE_NOT_SET: Long = -1
    }
}
