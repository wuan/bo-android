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

package org.blitzortung.android.alert.handler

import org.blitzortung.android.data.beans.Strike

internal class AggregatingAlertSectorRange(
    val rangeMinimum: Float,
    val rangeMaximum: Float
) {

    var strikeCount: Int = 0
        private set

    var latestStrikeTimestamp: Long = 0
        private set

    fun addStrike(strike: Strike) {
        if (strike.timestamp > this.latestStrikeTimestamp) {
            this.latestStrikeTimestamp = strike.timestamp
        }
        strikeCount += strike.multiplicity
    }


}
