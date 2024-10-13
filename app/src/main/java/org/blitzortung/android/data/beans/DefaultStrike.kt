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

data class DefaultStrike(
    override val timestamp: Long = 0,
    override val longitude: Double = 0.0,
    override val latitude: Double = 0.0,
    val altitude: Int = 0,
    val amplitude: Float = 0f,
    val stationCount: Short = 0,
    val lateralError: Double = 0.0
) : Strike, Serializable {

    override val multiplicity = 1

    companion object {
        private const val serialVersionUID = 4201042078597105622L
    }
}
